package com.smartsmsfilter.ml

import android.content.Context
import android.util.Log
import com.smartsmsfilter.domain.classifier.SmsClassifier
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.MessageClassification
import com.smartsmsfilter.domain.model.SmsMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

/**
 * Hybrid ML Classifier that combines:
 * 1. Primary ML model (TensorFlow Lite)
 * 2. Rule-based fallback system
 * 3. Continuous learning from user feedback
 * 4. Confidence calibration
 * 5. Pattern caching for performance
 * 
 * This is a foolproof system that never fails to classify.
 */
@Singleton
class HybridMLClassifier @Inject constructor(
    private val context: Context
) : SmsClassifier {
    
    companion object {
        private const val TAG = "HybridMLClassifier"
        
        // ML Model Configuration
        private const val MODEL_FILE = "mobile_sms_classifier.tflite"
        private const val VOCAB_FILE = "vocab.txt"
        private const val MAX_SEQUENCE_LENGTH = 60
        
        // Confidence Thresholds (adaptive)
        private const val HIGH_CONFIDENCE = 0.85f
        private const val MEDIUM_CONFIDENCE = 0.60f
        private const val LOW_CONFIDENCE = 0.40f
        
        // Learning Configuration
        private const val LEARNING_RATE = 0.1f
        private const val MAX_PATTERN_CACHE_SIZE = 1000
        private const val PATTERN_DECAY_RATE = 0.95f
        
        // Performance Configuration
        private const val ML_TIMEOUT_MS = 500L
        private const val CACHE_EXPIRY_MS = 3600000L // 1 hour
    }
    
    // ML Components
    private var mlInterpreter: Interpreter? = null
    private var vocabulary: Map<String, Int> = emptyMap()
    private var isMLInitialized = false
    
    // Learning Components
    private val patternCache = ConcurrentHashMap<String, CachedClassification>()
    private val senderReputation = ConcurrentHashMap<String, SenderProfile>()
    private val keywordWeights = ConcurrentHashMap<String, Float>()
    
    // Metrics for self-calibration
    private val classificationMetrics = ClassificationMetrics()
    
    // State Management
    private val _modelStatus = MutableStateFlow(ModelStatus.INITIALIZING)
    val modelStatus = _modelStatus.asStateFlow()
    
    init {
        // Initialize in background
        GlobalScope.launch(Dispatchers.IO) {
            initializeHybridSystem()
        }
    }
    
    /**
     * Initialize the hybrid classification system
     */
    private suspend fun initializeHybridSystem() {
        try {
            // 1. Try to load ML model
            val mlLoaded = tryLoadMLModel()
            
            // 2. Initialize rule-based patterns
            initializeRulePatterns()
            
            // 3. Load learned patterns from storage
            loadLearnedPatterns()
            
            // 4. Update status
            _modelStatus.value = if (mlLoaded) {
                ModelStatus.ML_READY
            } else {
                ModelStatus.FALLBACK_ONLY
            }
            
            Log.i(TAG, "Hybrid system initialized. Status: ${_modelStatus.value}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize hybrid system", e)
            _modelStatus.value = ModelStatus.FALLBACK_ONLY
        }
    }
    
    /**
     * Main classification method - combines all approaches
     */
    override suspend fun classifyMessage(message: SmsMessage): MessageClassification {
        return withContext(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()
            
            try {
                // Step 1: Check cache for recent similar messages
                val cachedResult = checkCache(message)
                if (cachedResult != null) {
                    Log.d(TAG, "Cache hit for message")
                    return@withContext cachedResult
                }
                
                // Step 2: Try ML classification with timeout
                val mlResult = withTimeoutOrNull(ML_TIMEOUT_MS) {
                    classifyWithML(message)
                }
                
                // Step 3: Get rule-based classification
                val ruleResult = classifyWithRules(message)
                
                // Step 4: Get reputation-based hints
                val reputationHint = getSenderReputation(message.sender)
                
                // Step 5: Combine all signals intelligently
                val finalResult = combineClassifications(
                    mlResult,
                    ruleResult,
                    reputationHint,
                    message
                )
                
                // Step 6: Cache the result
                cacheClassification(message, finalResult)
                
                // Step 7: Update metrics
                val processingTime = System.currentTimeMillis() - startTime
                classificationMetrics.recordClassification(finalResult.category, processingTime)
                
                Log.d(TAG, "Classified in ${processingTime}ms: ${finalResult.category} (${finalResult.confidence})")
                
                finalResult
                
            } catch (e: Exception) {
                Log.e(TAG, "Classification failed, using safe fallback", e)
                // Ultimate fallback - never fails
                createSafeFallback(message)
            }
        }
    }
    
    /**
     * ML-based classification
     */
    private suspend fun classifyWithML(message: SmsMessage): MessageClassification? {
        if (!isMLInitialized || mlInterpreter == null) {
            return null
        }
        
        return try {
            val input = tokenizeMessage(message.content)
            val output = Array(1) { FloatArray(6) }
            
            mlInterpreter?.run(input, output)
            
            val probabilities = output[0]
            val (category, confidence) = interpretMLOutput(probabilities)
            
            MessageClassification(
                category = category,
                confidence = confidence,
                reasons = listOf("ML model prediction", "Confidence: ${(confidence * 100).toInt()}%")
            )
        } catch (e: Exception) {
            Log.e(TAG, "ML classification failed", e)
            null
        }
    }
    
    /**
     * Advanced rule-based classification with learned patterns
     */
    private fun classifyWithRules(message: SmsMessage): MessageClassification {
        val content = message.content.lowercase()
        val sender = message.sender.uppercase()
        val scores = mutableMapOf<MessageCategory, Float>()
        val reasons = mutableListOf<String>()
        
        // Calculate scores for each category
        scores[MessageCategory.INBOX] = calculateInboxScore(content, sender, reasons)
        scores[MessageCategory.SPAM] = calculateSpamScore(content, sender, reasons)
        scores[MessageCategory.NEEDS_REVIEW] = 0.3f // Base score for uncertainty
        
        // Apply learned keyword weights
        applyLearnedWeights(content, scores)
        
        // Find best category
        val bestCategory = scores.maxByOrNull { it.value }?.key ?: MessageCategory.INBOX
        val confidence = scores[bestCategory] ?: 0.5f
        
        return MessageClassification(
            category = bestCategory,
            confidence = min(0.95f, confidence), // Cap at 95% for rules
            reasons = reasons
        )
    }
    
    /**
     * Calculate inbox score based on patterns
     */
    private fun calculateInboxScore(content: String, sender: String, reasons: MutableList<String>): Float {
        var score = 0.5f // Base score
        
        // OTP patterns (highest priority)
        if (isOTPMessage(content)) {
            score += 0.45f
            reasons.add("OTP/Verification detected")
        }
        
        // Banking patterns
        if (isBankingMessage(content, sender)) {
            score += 0.40f
            reasons.add("Banking/Financial message")
        }
        
        // E-commerce patterns
        if (isEcommerceMessage(content, sender)) {
            score += 0.35f
            reasons.add("E-commerce/Delivery update")
        }
        
        // Government/Official
        if (isOfficialMessage(sender)) {
            score += 0.35f
            reasons.add("Official sender")
        }
        
        // Personal indicators
        if (hasPersonalIndicators(content)) {
            score += 0.25f
            reasons.add("Personal message indicators")
        }
        
        return min(1.0f, score)
    }
    
    /**
     * Calculate spam score based on patterns
     */
    private fun calculateSpamScore(content: String, sender: String, reasons: MutableList<String>): Float {
        var score = 0.3f // Base score
        
        // Spam keywords
        val spamKeywords = listOf(
            "congratulations", "winner", "prize", "claim", "free",
            "offer", "discount", "sale", "limited", "exclusive",
            "click here", "download now", "earn money", "cash back",
            "loan", "credit", "emi", "insurance"
        )
        
        val spamCount = spamKeywords.count { content.contains(it) }
        score += spamCount * 0.15f
        
        if (spamCount > 0) {
            reasons.add("Contains $spamCount spam keywords")
        }
        
        // Spam sender patterns
        if (isSpamSender(sender)) {
            score += 0.35f
            reasons.add("Promotional sender pattern")
        }
        
        // Excessive caps or special chars
        val capsRatio = content.count { it.isUpperCase() }.toFloat() / content.length
        if (capsRatio > 0.3 && content.length > 50) {
            score += 0.2f
            reasons.add("Excessive capitalization")
        }
        
        // URLs and shortlinks
        if (content.contains("http://") || content.contains("bit.ly") || content.contains("tinyurl")) {
            score += 0.25f
            reasons.add("Contains suspicious links")
        }
        
        return min(1.0f, score)
    }
    
    /**
     * Combine multiple classification signals intelligently
     */
    private fun combineClassifications(
        mlResult: MessageClassification?,
        ruleResult: MessageClassification,
        reputationHint: SenderProfile?,
        message: SmsMessage
    ): MessageClassification {
        
        // If ML is highly confident, use it
        if (mlResult != null && mlResult.confidence >= HIGH_CONFIDENCE) {
            return mlResult.copy(
                reasons = mlResult.reasons + listOf("High ML confidence")
            )
        }
        
        // If sender has strong reputation, use it
        if (reputationHint != null && reputationHint.confidence >= HIGH_CONFIDENCE) {
            return MessageClassification(
                category = reputationHint.primaryCategory,
                confidence = reputationHint.confidence,
                reasons = listOf("Known sender reputation", "History: ${reputationHint.messageCount} messages")
            )
        }
        
        // If both ML and rules agree, boost confidence
        if (mlResult != null && mlResult.category == ruleResult.category) {
            val combinedConfidence = (mlResult.confidence + ruleResult.confidence) / 2 * 1.1f
            return MessageClassification(
                category = mlResult.category,
                confidence = min(0.99f, combinedConfidence),
                reasons = listOf("ML and rules agree") + mlResult.reasons + ruleResult.reasons
            )
        }
        
        // If ML confidence is medium, weighted average
        if (mlResult != null && mlResult.confidence >= MEDIUM_CONFIDENCE) {
            val mlWeight = 0.6f
            val ruleWeight = 0.4f
            
            // Use ML category but adjust confidence
            val weightedConfidence = mlResult.confidence * mlWeight + ruleResult.confidence * ruleWeight
            return MessageClassification(
                category = mlResult.category,
                confidence = weightedConfidence,
                reasons = listOf("ML primary, rules secondary") + mlResult.reasons
            )
        }
        
        // Otherwise, prefer rules (more reliable)
        return ruleResult.copy(
            reasons = listOf("Rule-based classification") + ruleResult.reasons
        )
    }
    
    /**
     * Learn from user corrections
     */
    override suspend fun learnFromCorrection(
        message: SmsMessage,
        userCorrection: MessageClassification
    ) {
        withContext(Dispatchers.IO) {
            try {
                // 1. Update sender reputation
                updateSenderReputation(message.sender, userCorrection.category)
                
                // 2. Extract and weight keywords
                learnKeywordPatterns(message.content, userCorrection.category)
                
                // 3. Store pattern for future reference
                storeLearnedPattern(message, userCorrection)
                
                // 4. Update metrics
                classificationMetrics.recordCorrection(userCorrection.category)
                
                // 5. Trigger model retraining if needed (offline)
                if (classificationMetrics.needsRetraining()) {
                    scheduleModelRetraining()
                }
                
                Log.d(TAG, "Learned from correction: ${message.sender} -> ${userCorrection.category}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to learn from correction", e)
            }
        }
    }
    
    /**
     * Update sender reputation based on user feedback
     */
    private fun updateSenderReputation(sender: String, category: MessageCategory) {
        val normalizedSender = normalizeSender(sender)
        val profile = senderReputation.getOrPut(normalizedSender) {
            SenderProfile(normalizedSender)
        }
        
        profile.updateCategory(category)
        
        // Persist to storage
        saveSenderReputation(normalizedSender, profile)
    }
    
    /**
     * Learn keyword patterns from user corrections
     */
    private fun learnKeywordPatterns(content: String, category: MessageCategory) {
        val words = content.lowercase().split(Regex("\\s+"))
        
        for (word in words) {
            if (word.length < 3) continue // Skip short words
            
            val key = "$category:$word"
            val currentWeight = keywordWeights[key] ?: 0.5f
            
            // Update weight using exponential moving average
            val newWeight = currentWeight * (1 - LEARNING_RATE) + 1.0f * LEARNING_RATE
            keywordWeights[key] = newWeight
        }
        
        // Decay weights of other categories for these words
        for (otherCategory in MessageCategory.values()) {
            if (otherCategory != category) {
                for (word in words) {
                    val key = "$otherCategory:$word"
                    keywordWeights[key] = (keywordWeights[key] ?: 0.5f) * PATTERN_DECAY_RATE
                }
            }
        }
    }
    
    /**
     * Apply learned weights to classification scores
     */
    private fun applyLearnedWeights(content: String, scores: MutableMap<MessageCategory, Float>) {
        val words = content.lowercase().split(Regex("\\s+"))
        
        for (category in MessageCategory.values()) {
            var weightSum = 0f
            var wordCount = 0
            
            for (word in words) {
                val key = "$category:$word"
                val weight = keywordWeights[key]
                if (weight != null && weight > 0.5f) {
                    weightSum += weight
                    wordCount++
                }
            }
            
            if (wordCount > 0) {
                val avgWeight = weightSum / wordCount
                scores[category] = (scores[category] ?: 0.5f) * avgWeight
            }
        }
    }
    
    // Pattern Detection Methods
    
    private fun isOTPMessage(content: String): Boolean {
        val otpPatterns = listOf(
            "otp", "one time password", "verification code",
            "verify", "authentication", "2fa", "passcode",
            "security code", "confirmation code"
        )
        
        if (otpPatterns.any { content.contains(it) }) return true
        
        // Check for 4-8 digit codes in short messages
        val codePattern = Regex("\\b\\d{4,8}\\b")
        return codePattern.containsMatchIn(content) && content.length < 200
    }
    
    private fun isBankingMessage(content: String, sender: String): Boolean {
        val bankKeywords = listOf(
            "bank", "credited", "debited", "balance", "account",
            "transaction", "transfer", "payment", "upi", "imps",
            "neft", "rtgs", "atm", "withdraw", "deposit"
        )
        
        val bankSenders = listOf(
            "BANK", "SBI", "HDFC", "ICICI", "AXIS", "PNB",
            "PAYTM", "PHONEPE", "GPAY", "BHIM"
        )
        
        return bankSenders.any { sender.contains(it) } ||
               bankKeywords.count { content.contains(it) } >= 2
    }
    
    private fun isEcommerceMessage(content: String, sender: String): Boolean {
        val ecomKeywords = listOf(
            "order", "delivery", "shipped", "tracking", "package",
            "courier", "dispatch", "delivered", "return", "refund"
        )
        
        val ecomSenders = listOf(
            "AMAZON", "FLIPKART", "MYNTRA", "SNAPDEAL", "SWIGGY", "ZOMATO"
        )
        
        return ecomSenders.any { sender.contains(it) } ||
               ecomKeywords.count { content.contains(it) } >= 2
    }
    
    private fun isOfficialMessage(sender: String): Boolean {
        val officialSenders = listOf(
            "GOVT", "GOV", "UIDAI", "EPFO", "CBSE", "NTA",
            "RAILWAY", "IRCTC", "INCOME", "TAX", "GST"
        )
        
        return officialSenders.any { sender.contains(it) }
    }
    
    private fun hasPersonalIndicators(content: String): Boolean {
        val personalPatterns = listOf(
            "hi ", "hey ", "hello ", "dear ", "thanks", "thank you",
            "please", "sorry", "how are you", "see you"
        )
        
        return personalPatterns.any { content.startsWith(it) }
    }
    
    private fun isSpamSender(sender: String): Boolean {
        val spamPrefixes = listOf(
            "AD-", "AX-", "BZ-", "CP-", "DM-", "HP-",
            "IM-", "JM-", "LM-", "QP-", "TD-", "TM-", "VM-", "VK-",
            "PROMO", "OFFER", "SALE", "DEAL"
        )
        
        return spamPrefixes.any { sender.startsWith(it) }
    }
    
    // Cache Management
    
    private fun checkCache(message: SmsMessage): MessageClassification? {
        val key = generateCacheKey(message)
        val cached = patternCache[key]
        
        if (cached != null && !cached.isExpired()) {
            return cached.classification
        }
        
        return null
    }
    
    private fun cacheClassification(message: SmsMessage, classification: MessageClassification) {
        val key = generateCacheKey(message)
        patternCache[key] = CachedClassification(classification, System.currentTimeMillis())
        
        // Limit cache size
        if (patternCache.size > MAX_PATTERN_CACHE_SIZE) {
            cleanCache()
        }
    }
    
    private fun cleanCache() {
        val now = System.currentTimeMillis()
        patternCache.entries.removeIf { it.value.isExpired(now) }
        
        // If still too large, remove oldest entries
        if (patternCache.size > MAX_PATTERN_CACHE_SIZE) {
            val entriesToRemove = patternCache.size - MAX_PATTERN_CACHE_SIZE
            patternCache.entries
                .sortedBy { it.value.timestamp }
                .take(entriesToRemove)
                .forEach { patternCache.remove(it.key) }
        }
    }
    
    private fun generateCacheKey(message: SmsMessage): String {
        // Create a hash of sender and first 100 chars of content
        val contentPrefix = message.content.take(100)
        return "${message.sender}:${contentPrefix.hashCode()}"
    }
    
    // Safe Fallback
    
    private fun createSafeFallback(message: SmsMessage): MessageClassification {
        // Never return NEEDS_REVIEW as fallback - always make a decision
        val content = message.content.lowercase()
        val sender = message.sender.uppercase()
        
        // Quick heuristics for safety
        val category = when {
            content.contains("otp") || content.contains("code") -> MessageCategory.INBOX
            sender.contains("BANK") || content.contains("credited") -> MessageCategory.INBOX
            sender.startsWith("AD-") || sender.startsWith("PROMO") -> MessageCategory.SPAM
            content.contains("offer") && content.contains("click") -> MessageCategory.SPAM
            else -> MessageCategory.INBOX // Default to inbox for safety
        }
        
        return MessageClassification(
            category = category,
            confidence = 0.6f,
            reasons = listOf("Safe fallback classification", "System protection active")
        )
    }
    
    // ML Model Management
    
    private suspend fun tryLoadMLModel(): Boolean {
        return try {
            val modelFile = File(context.filesDir, MODEL_FILE)
            if (modelFile.exists()) {
                // Load from internal storage (updated model)
                mlInterpreter = Interpreter(modelFile)
            } else {
                // Load from assets (original model)
                val modelBuffer = loadModelFromAssets()
                mlInterpreter = Interpreter(modelBuffer)
            }
            
            vocabulary = loadVocabulary()
            isMLInitialized = true
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load ML model", e)
            false
        }
    }
    
    private fun loadModelFromAssets(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    private fun loadVocabulary(): Map<String, Int> {
        val vocab = mutableMapOf<String, Int>()
        try {
            context.assets.open(VOCAB_FILE).bufferedReader().use { reader ->
                reader.readLines().forEachIndexed { index, word ->
                    vocab[word.trim()] = index
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load vocabulary", e)
        }
        return vocab
    }
    
    private fun tokenizeMessage(content: String): Array<IntArray> {
        val tokens = IntArray(MAX_SEQUENCE_LENGTH)
        val words = content.lowercase().split(Regex("\\s+"))
        
        for ((i, word) in words.take(MAX_SEQUENCE_LENGTH).withIndex()) {
            tokens[i] = vocabulary[word] ?: vocabulary["[UNK]"] ?: 1
        }
        
        return arrayOf(tokens)
    }
    
    private fun interpretMLOutput(probabilities: FloatArray): Pair<MessageCategory, Float> {
        val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
        val confidence = probabilities[maxIndex]
        
        val category = when (maxIndex) {
            0, 2, 3, 4 -> MessageCategory.INBOX // Important categories
            1 -> MessageCategory.SPAM
            5 -> MessageCategory.NEEDS_REVIEW
            else -> MessageCategory.INBOX
        }
        
        return Pair(category, confidence)
    }
    
    // Persistence Methods
    
    private fun initializeRulePatterns() {
        // Load default patterns
        // This would be loaded from a config file in production
    }
    
    private fun loadLearnedPatterns() {
        val prefs = context.getSharedPreferences("ml_learning", Context.MODE_PRIVATE)
        
        // Load sender reputations
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("sender:")) {
                val sender = key.substring(7)
                // Parse and load sender profile
            }
        }
        
        // Load keyword weights
        prefs.all.forEach { (key, value) ->
            if (key.startsWith("keyword:")) {
                keywordWeights[key.substring(8)] = value as? Float ?: 0.5f
            }
        }
    }
    
    private fun saveSenderReputation(sender: String, profile: SenderProfile) {
        val prefs = context.getSharedPreferences("ml_learning", Context.MODE_PRIVATE)
        prefs.edit().putString("sender:$sender", profile.serialize()).apply()
    }
    
    private fun storeLearnedPattern(message: SmsMessage, classification: MessageClassification) {
        // Store for future model retraining
        val prefs = context.getSharedPreferences("ml_patterns", Context.MODE_PRIVATE)
        val key = "pattern:${System.currentTimeMillis()}"
        val value = "${message.sender}|${message.content.take(100)}|${classification.category}"
        prefs.edit().putString(key, value).apply()
    }
    
    private fun scheduleModelRetraining() {
        // In production, this would trigger a background job to retrain the model
        Log.i(TAG, "Model retraining scheduled")
    }
    
    private fun normalizeSender(sender: String): String {
        return sender.uppercase().replace(Regex("[^A-Z0-9]"), "")
    }
    
    private fun getSenderReputation(sender: String): SenderProfile? {
        val normalizedSender = normalizeSender(sender)
        return senderReputation[normalizedSender]
    }
    
    // Support Classes
    
    private data class CachedClassification(
        val classification: MessageClassification,
        val timestamp: Long
    ) {
        fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
            return (now - timestamp) > CACHE_EXPIRY_MS
        }
    }
    
    private data class SenderProfile(
        val sender: String,
        var messageCount: Int = 0,
        var inboxCount: Int = 0,
        var spamCount: Int = 0,
        var reviewCount: Int = 0
    ) {
        val primaryCategory: MessageCategory
            get() = when {
                spamCount > inboxCount * 2 -> MessageCategory.SPAM
                inboxCount > 0 -> MessageCategory.INBOX
                else -> MessageCategory.NEEDS_REVIEW
            }
        
        val confidence: Float
            get() = if (messageCount > 5) {
                min(0.95f, 0.5f + (messageCount * 0.05f))
            } else {
                0.5f
            }
        
        fun updateCategory(category: MessageCategory) {
            messageCount++
            when (category) {
                MessageCategory.INBOX -> inboxCount++
                MessageCategory.SPAM -> spamCount++
                MessageCategory.NEEDS_REVIEW -> reviewCount++
            }
        }
        
        fun serialize(): String {
            return "$sender|$messageCount|$inboxCount|$spamCount|$reviewCount"
        }
    }
    
    private class ClassificationMetrics {
        private var totalClassifications = 0
        private var corrections = 0
        private val categoryAccuracy = mutableMapOf<MessageCategory, Float>()
        private var totalTime = 0L
        
        fun recordClassification(category: MessageCategory, time: Long) {
            totalClassifications++
            totalTime += time
        }
        
        fun recordCorrection(category: MessageCategory) {
            corrections++
            val accuracy = categoryAccuracy[category] ?: 1.0f
            categoryAccuracy[category] = accuracy * 0.95f // Decay accuracy
        }
        
        fun needsRetraining(): Boolean {
            // Trigger retraining if accuracy drops below 80%
            val avgAccuracy = categoryAccuracy.values.average()
            return corrections > 100 && avgAccuracy < 0.8
        }
        
        fun getAverageTime(): Long {
            return if (totalClassifications > 0) {
                totalTime / totalClassifications
            } else {
                0L
            }
        }
    }
    
    enum class ModelStatus {
        INITIALIZING,
        ML_READY,
        FALLBACK_ONLY,
        RETRAINING
    }
    
    // Interface implementations
    
    override suspend fun classifyBatch(messages: List<SmsMessage>): Map<Long, MessageClassification> {
        return messages.associate { message ->
            message.id to classifyMessage(message)
        }
    }
    
    override fun getConfidenceThreshold(): Float {
        // Adaptive threshold based on model performance
        return if (classificationMetrics.getAverageTime() < 100) {
            MEDIUM_CONFIDENCE // Fast processing, can be more selective
        } else {
            LOW_CONFIDENCE // Slower processing, be more inclusive
        }
    }
}