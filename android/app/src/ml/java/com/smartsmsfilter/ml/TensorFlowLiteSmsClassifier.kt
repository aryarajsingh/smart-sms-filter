package com.smartsmsfilter.ml

import android.content.Context
import android.util.Log
import com.smartsmsfilter.domain.classifier.SmsClassifier
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.MessageClassification
import com.smartsmsfilter.domain.model.SmsMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay

/**
 * TensorFlow Lite implementation of SMS classifier
 */
@Singleton
class TensorFlowLiteSmsClassifier @Inject constructor(
    private val context: Context
) : SmsClassifier {
    
    init {
        Log.d(TAG, "TensorFlowLiteSmsClassifier instance created!")
    }
    
    companion object {
        private const val TAG = "TFLiteSmsClassifier"
        private const val MODEL_FILE = "mobile_sms_classifier.tflite"
        private const val VOCAB_FILE = "vocab.txt"
        private const val MAX_SEQUENCE_LENGTH = 60
        private const val CONFIDENCE_THRESHOLD = 0.4f // Lowered to accept more classifications
        
        // ML model categories -> App categories mapping
        private val ML_TO_APP_CATEGORY = mapOf(
            0 to MessageCategory.INBOX,      // INBOX
            1 to MessageCategory.SPAM,       // SPAM  
            2 to MessageCategory.INBOX,      // OTP -> INBOX (important)
            3 to MessageCategory.INBOX,      // BANKING -> INBOX (important)
            4 to MessageCategory.INBOX,      // ECOMMERCE -> INBOX (could be important)
            5 to MessageCategory.NEEDS_REVIEW // NEEDS_REVIEW
        )
    }
    
    private var interpreter: Interpreter? = null
    private var vocabulary: Map<String, Int> = emptyMap()
    private var isInitialized = false
    
    /**
     * Initialize the TensorFlow Lite model and tokenizer
     */
    private suspend fun initialize() = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext
        
        try {
            Log.d(TAG, "Initializing TensorFlow Lite SMS classifier...")
            
            // Check if model file exists
            val modelExists = try {
                context.assets.list("")?.contains(MODEL_FILE) ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Cannot check model file existence", e)
                false
            }
            
            if (!modelExists) {
                Log.w(TAG, "Model file not found: $MODEL_FILE")
                throw IllegalStateException("Model file not found in assets")
            }
            
            // Load model
            val modelBuffer = loadModelFromAssets()
            val options = Interpreter.Options().apply {
                setNumThreads(2) // Conservative threading for mobile
                setUseXNNPACK(false) // Disable XNNPACK as it can cause issues
            }
            interpreter = Interpreter(modelBuffer, options)
            
            // Load vocabulary
            vocabulary = loadVocabulary()
            
            isInitialized = true
            Log.d(TAG, "TensorFlow Lite classifier initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TensorFlow Lite classifier: ${e.message}", e)
            isInitialized = false
            interpreter = null
            vocabulary = emptyMap()
            throw e
        }
    }
    
    override suspend fun classifyMessage(message: SmsMessage): MessageClassification {
        return try {
            // Ensure model is initialized with retry logic
            if (!isInitialized) {
                try {
                    initialize()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize model, using rule-based fallback", e)
                    // Use rule-based classification as fallback
                    return classifyWithRules(message)
                }
            }
            
            withContext(Dispatchers.Default) {
                val startTime = System.currentTimeMillis()
                
                // Tokenize input text
                val inputSequence = tokenizeText(message.content)
                
                // Prepare input array for TensorFlow Lite
                val input = Array(1) { inputSequence }
                val output = Array(1) { FloatArray(6) } // 6 ML model categories
                
                // Run inference with null check
                val currentInterpreter = interpreter
                if (currentInterpreter == null) {
                    Log.e(TAG, "Interpreter is null, using rule-based fallback")
                    return@withContext classifyWithRules(message)
                }
                
                currentInterpreter.run(input, output)
                
                val probabilities = output[0]
                val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
                val confidence = probabilities[maxIndex]
                
                // If confidence is too low, use rule-based fallback instead of NEEDS_REVIEW
                if (confidence < CONFIDENCE_THRESHOLD) {
                    Log.d(TAG, "Low confidence ($confidence), using rule-based fallback")
                    return@withContext classifyWithRules(message)
                }
                
                // Map ML category to app category
                val appCategory = ML_TO_APP_CATEGORY[maxIndex] ?: MessageCategory.INBOX
                
                val processingTime = System.currentTimeMillis() - startTime
                
                // Generate reasoning
                val reasons = generateReasoning(maxIndex, confidence, message.content, processingTime)
                
                Log.d(TAG, "Classified message in ${processingTime}ms: $appCategory (confidence: $confidence)")
                
                MessageClassification(
                    category = appCategory,
                    confidence = confidence,
                    reasons = reasons
                )
            }
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory during classification, using rule-based fallback", e)
            // Try to recover by clearing memory and use fallback
            System.gc()
            classifyWithRules(message)
        } catch (e: Exception) {
            Log.e(TAG, "Error classifying message: ${e.message}, using rule-based fallback", e)
            // Use rule-based fallback for any errors
            classifyWithRules(message)
        }
    }
    
    override suspend fun classifyBatch(messages: List<SmsMessage>): Map<Long, MessageClassification> {
        return messages.associate { message ->
            message.id to classifyMessage(message)
        }
    }
    
    override suspend fun learnFromCorrection(
        message: SmsMessage, 
        userCorrection: MessageClassification
    ) {
        try {
            // TensorFlow Lite models are static, cannot learn online
            // Store corrections for future model retraining
            Log.d(TAG, "Learning from correction: ${message.sender} -> ${userCorrection.category}")
            
            // Store feedback in SharedPreferences for analytics
            context.getSharedPreferences("ml_feedback", Context.MODE_PRIVATE)
                .edit()
                .putString(
                    "feedback_${System.currentTimeMillis()}",
                    "${message.sender}|${userCorrection.category}"
                )
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error storing user feedback", e)
        }
    }
    
    override fun getConfidenceThreshold(): Float = CONFIDENCE_THRESHOLD
    
    /**
     * Load TensorFlow Lite model from assets
     */
    private fun loadModelFromAssets(): MappedByteBuffer {
        return context.assets.openFd(MODEL_FILE).use { fileDescriptor ->
            FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                val fileChannel = inputStream.channel
                val startOffset = fileDescriptor.startOffset
                val declaredLength = fileDescriptor.declaredLength
                fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
            }
        }
    }
    
    /**
     * Load vocabulary from assets
     */
    private fun loadVocabulary(): Map<String, Int> {
        val vocab = mutableMapOf<String, Int>()
        
        context.assets.open(VOCAB_FILE).bufferedReader().use { reader ->
            reader.readLines().forEachIndexed { index, word ->
                vocab[word.trim()] = index
            }
        }
        
        Log.d(TAG, "Loaded vocabulary with ${vocab.size} tokens")
        return vocab
    }
    
    /**
     * Tokenize text using the loaded vocabulary
     */
    private fun tokenizeText(text: String): IntArray {
        val tokens = IntArray(MAX_SEQUENCE_LENGTH)
        
        // Simple preprocessing
        val processedText = text.lowercase()
            .replace(Regex("[₹Rs.?]"), "rs") // Normalize currency
            .replace(Regex("\\b\\d{10,12}\\b"), "phone") // Normalize phone numbers
            .replace(Regex("\\d{4}\\d*"), "number") // Normalize account numbers
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()
        
        // Tokenize by whitespace
        val words = processedText.split(Regex("\\s+")).filter { it.isNotBlank() }
        
        // Convert to indices
        for ((i, word) in words.take(MAX_SEQUENCE_LENGTH).withIndex()) {
            tokens[i] = vocabulary[word] ?: vocabulary["[UNK]"] ?: 1 // UNK token
        }
        
        return tokens
    }
    
    /**
     * Generate human-readable reasoning for the classification
     */
    private fun generateReasoning(
        predictedIndex: Int, 
        confidence: Float, 
        content: String,
        processingTime: Long
    ): List<String> {
        val reasons = mutableListOf<String>()
        
        val categoryName = when (predictedIndex) {
            0 -> "Important message"
            1 -> "Spam/promotional content"
            2 -> "OTP/verification code"
            3 -> "Banking/financial transaction"
            4 -> "E-commerce/shopping update"
            5 -> "Uncertain classification"
            else -> "Unknown category"
        }
        
        reasons.add("ML model classified as: $categoryName")
        reasons.add("Confidence score: ${String.format("%.2f", confidence)}")
        
        if (confidence < CONFIDENCE_THRESHOLD) {
            reasons.add("Low confidence classification")
        }
        
        // Add content-based hints
        val lowerContent = content.lowercase()
        when {
            lowerContent.contains("otp") || lowerContent.contains("verification") -> 
                reasons.add("Contains OTP/verification keywords")
            lowerContent.contains("bank") || lowerContent.contains("account") -> 
                reasons.add("Contains banking keywords")
            lowerContent.contains("order") || lowerContent.contains("delivery") -> 
                reasons.add("Contains e-commerce keywords")
            lowerContent.contains("congratulations") || lowerContent.contains("winner") -> 
                reasons.add("Contains promotional keywords")
        }
        
        reasons.add("Processed in ${processingTime}ms using ML model")
        
        return reasons
    }
    
    /**
     * Rule-based classification fallback when ML model is not available
     * This ensures the app still works even without the ML model
     */
    private fun classifyWithRules(message: SmsMessage): MessageClassification {
        val content = message.content.lowercase()
        val sender = message.sender.uppercase()
        val reasons = mutableListOf<String>()
        
        // Priority 1: OTP Detection (Always INBOX)
        if (isOtpMessage(content)) {
            reasons.add("OTP/Verification code detected")
            reasons.add("Security messages always go to inbox")
            return MessageClassification(
                category = MessageCategory.INBOX,
                confidence = 0.95f,
                reasons = reasons
            )
        }
        
        // Priority 2: Banking/Financial (Always INBOX)
        if (isBankingMessage(content, sender)) {
            reasons.add("Banking/Financial transaction detected")
            reasons.add("Financial messages are important")
            return MessageClassification(
                category = MessageCategory.INBOX,
                confidence = 0.9f,
                reasons = reasons
            )
        }
        
        // Priority 3: Known Spam Patterns
        if (isSpamMessage(content, sender)) {
            reasons.add("Promotional/Spam pattern detected")
            reasons.add("Contains marketing keywords")
            return MessageClassification(
                category = MessageCategory.SPAM,
                confidence = 0.85f,
                reasons = reasons
            )
        }
        
        // Priority 4: E-commerce/Delivery (INBOX)
        if (isEcommerceMessage(content)) {
            reasons.add("E-commerce/Delivery update detected")
            reasons.add("Order updates are important")
            return MessageClassification(
                category = MessageCategory.INBOX,
                confidence = 0.8f,
                reasons = reasons
            )
        }
        
        // Priority 5: Government/Official (INBOX)
        if (isOfficialMessage(sender)) {
            reasons.add("Official/Government sender detected")
            return MessageClassification(
                category = MessageCategory.INBOX,
                confidence = 0.85f,
                reasons = reasons
            )
        }
        
        // Default: If no clear pattern, mark as INBOX (safer than NEEDS_REVIEW)
        // This prevents the "everything is review" problem
        reasons.add("No spam indicators found")
        reasons.add("Defaulting to inbox for safety")
        return MessageClassification(
            category = MessageCategory.INBOX,
            confidence = 0.6f,
            reasons = reasons
        )
    }
    
    private fun isOtpMessage(content: String): Boolean {
        val otpPatterns = listOf(
            "otp", "one time password", "verification code", "verify",
            "authentication", "2fa", "two factor", "passcode",
            "security code", "confirmation code", "validate"
        )
        
        // Check for OTP keywords
        if (otpPatterns.any { content.contains(it) }) return true
        
        // Check for 4-8 digit codes
        val codePattern = Regex("\\b\\d{4,8}\\b")
        if (codePattern.containsMatchIn(content) && content.length < 200) return true
        
        return false
    }
    
    private fun isBankingMessage(content: String, sender: String): Boolean {
        val bankKeywords = listOf(
            "bank", "credited", "debited", "balance", "account",
            "transaction", "transfer", "payment", "upi", "imps",
            "neft", "rtgs", "atm", "withdraw", "deposit",
            "a/c", "ac no", "inr", "rs.", "₹"
        )
        
        val bankSenders = listOf(
            "BANK", "SBI", "HDFC", "ICICI", "AXIS", "PNB",
            "BOB", "CANARA", "KOTAK", "IDBI", "UNION",
            "PAYTM", "PHONEPE", "GPAY", "BHIM"
        )
        
        // Check sender
        if (bankSenders.any { sender.contains(it) }) return true
        
        // Check content
        val matchCount = bankKeywords.count { content.contains(it) }
        return matchCount >= 2 // At least 2 banking keywords
    }
    
    private fun isSpamMessage(content: String, sender: String): Boolean {
        val spamKeywords = listOf(
            "congratulations", "winner", "won", "prize", "lucky",
            "claim", "free", "offer", "discount", "sale", "limited time",
            "hurry", "exclusive", "click here", "download now",
            "earn money", "cash back", "reward", "bonus",
            "loan", "credit", "emi", "insurance", "policy"
        )
        
        val spamSenders = listOf(
            "PROMO", "OFFER", "SALE", "DEAL", "AD-", "AX-",
            "BZ-", "CP-", "DM-", "HP-", "IM-", "JM-",
            "LM-", "QP-", "TD-", "TM-", "VM-", "VK-"
        )
        
        // Check for spam sender patterns
        if (spamSenders.any { sender.startsWith(it) }) return true
        
        // Count spam indicators
        val spamCount = spamKeywords.count { content.contains(it) }
        
        // Multiple spam keywords = likely spam
        if (spamCount >= 3) return true
        
        // Check for excessive capitalization or special characters
        val capsRatio = content.count { it.isUpperCase() }.toFloat() / content.length
        if (capsRatio > 0.3 && content.length > 50) return true
        
        return false
    }
    
    private fun isEcommerceMessage(content: String): Boolean {
        val ecomKeywords = listOf(
            "order", "delivery", "shipped", "tracking", "package",
            "courier", "dispatch", "arrived", "out for delivery",
            "delivered", "return", "refund", "exchange",
            "amazon", "flipkart", "myntra", "snapdeal"
        )
        
        val matchCount = ecomKeywords.count { content.contains(it) }
        return matchCount >= 2
    }
    
    private fun isOfficialMessage(sender: String): Boolean {
        val officialSenders = listOf(
            "GOVT", "GOV", "UIDAI", "EPFO", "CBSE", "NTA",
            "RAILWAY", "IRCTC", "INCOME", "TAX", "GST",
            "ELECTION", "POLICE", "COVID", "HEALTH"
        )
        
        return officialSenders.any { sender.contains(it) }
    }
}
