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
        private const val CONFIDENCE_THRESHOLD = 0.6f
        
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
            
            // Load model
            val modelBuffer = loadModelFromAssets()
            val options = Interpreter.Options().apply {
                setNumThreads(2) // Conservative threading for mobile
                setUseXNNPACK(true) // Enable optimizations
            }
            interpreter = Interpreter(modelBuffer, options)
            
            // Load vocabulary
            vocabulary = loadVocabulary()
            
            isInitialized = true
            Log.d(TAG, "TensorFlow Lite classifier initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TensorFlow Lite classifier", e)
            throw e
        }
    }
    
    override suspend fun classifyMessage(message: SmsMessage): MessageClassification {
        try {
            // Ensure model is initialized
            if (!isInitialized) {
                initialize()
            }
            
            return withContext(Dispatchers.Default) {
                val startTime = System.currentTimeMillis()
                
                // Tokenize input text
                val inputSequence = tokenizeText(message.content)
                
                // Prepare input array for TensorFlow Lite
                val input = Array(1) { inputSequence }
                val output = Array(1) { FloatArray(6) } // 6 ML model categories
                
                // Run inference
                interpreter?.run(input, output)
                
                val probabilities = output[0]
                val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
                val confidence = probabilities[maxIndex]
                
                // Map ML category to app category
                val appCategory = ML_TO_APP_CATEGORY[maxIndex] ?: MessageCategory.NEEDS_REVIEW
                
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
        } catch (e: Exception) {
            Log.e(TAG, "Error classifying message with TensorFlow Lite", e)
            // Fallback to needs review with low confidence
            return MessageClassification(
                category = MessageCategory.NEEDS_REVIEW,
                confidence = 0.1f,
                reasons = listOf("ML classification failed, needs manual review")
            )
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
        // TensorFlow Lite models are static, cannot learn online
        // This would require retraining the model offline
        Log.d(TAG, "Learning from correction: ${message.sender} -> ${userCorrection.category}")
        // TODO: Could store corrections for future model retraining
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
}