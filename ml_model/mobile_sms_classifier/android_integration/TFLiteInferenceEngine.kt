/**
 * TensorFlow Lite Inference Engine for Android SMS Classification
 * Optimized for on-device processing with minimal memory footprint
 * 
 * This class provides the bridge between the quantized ML model and Android app
 * Target Performance: <100ms inference, <256MB memory usage
 */

package com.smartsmsfilter.ml

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import androidx.annotation.WorkerThread
import kotlinx.coroutines.*
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.TensorProcessor
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.exp

/**
 * Result of SMS classification
 */
data class ClassificationResult(
    val category: SmsCategory,
    val confidence: Float,
    val probabilities: Map<SmsCategory, Float>,
    val processingTimeMs: Long,
    val modelVersion: String
)

/**
 * SMS Categories for classification
 */
enum class SmsCategory(val id: Int, val displayName: String) {
    INBOX(0, "Important"),
    SPAM(1, "Spam"),
    OTP(2, "OTP"),
    BANKING(3, "Banking"),
    ECOMMERCE(4, "Shopping"),
    NEEDS_REVIEW(5, "Review Needed");
    
    companion object {
        fun fromId(id: Int): SmsCategory = values().first { it.id == id }
    }
}

/**
 * Simple tokenizer for SMS text preprocessing
 * Mimics the behavior of DistilBERT tokenizer for mobile inference
 */
class SimpleSmsTokenizer(context: Context) {
    
    companion object {
        private const val TAG = "SmsTokenizer"
        private const val MAX_LENGTH = 128
        private const val VOCAB_FILE = "vocab.txt"
        
        // Special tokens
        private const val PAD_TOKEN = "[PAD]"
        private const val UNK_TOKEN = "[UNK]"
        private const val CLS_TOKEN = "[CLS]"
        private const val SEP_TOKEN = "[SEP]"
    }
    
    private val vocabulary: Map<String, Int>
    private val reverseVocab: Map<Int, String>
    
    init {
        // Load vocabulary from assets
        val vocabList = try {
            context.assets.open(VOCAB_FILE).bufferedReader().readLines()
        } catch (e: IOException) {
            Log.w(TAG, "Could not load vocabulary file, using default vocab")
            getDefaultVocabulary()
        }
        
        vocabulary = vocabList.mapIndexed { index, token -> token to index }.toMap()
        reverseVocab = vocabulary.map { (token, id) -> id to token }.toMap()
        
        Log.i(TAG, "Loaded vocabulary with ${vocabulary.size} tokens")
    }
    
    /**
     * Tokenize SMS text and return input IDs and attention mask
     */
    fun tokenize(text: String): TokenizationResult {
        // Preprocess text
        val preprocessedText = preprocessText(text)
        
        // Simple whitespace tokenization (simplified for mobile)
        val tokens = mutableListOf<String>()
        tokens.add(CLS_TOKEN)
        
        // Split text into tokens
        val words = preprocessedText.lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        
        for (word in words) {
            if (tokens.size >= MAX_LENGTH - 1) break
            
            // Simple word-level tokenization
            val token = if (vocabulary.containsKey(word)) {
                word
            } else {
                UNK_TOKEN
            }
            tokens.add(token)
        }
        
        tokens.add(SEP_TOKEN)
        
        // Convert to IDs
        val inputIds = IntArray(MAX_LENGTH)
        val attentionMask = IntArray(MAX_LENGTH)
        
        for (i in 0 until MAX_LENGTH) {
            if (i < tokens.size) {
                inputIds[i] = vocabulary[tokens[i]] ?: vocabulary[UNK_TOKEN]!!
                attentionMask[i] = 1
            } else {
                inputIds[i] = vocabulary[PAD_TOKEN]!!
                attentionMask[i] = 0
            }
        }
        
        return TokenizationResult(inputIds, attentionMask)
    }
    
    private fun preprocessText(text: String): String {
        return text
            .replace(Regex("[₹Rs.?]"), "Rs") // Normalize currency
            .replace(Regex("\\b\\d{10,12}\\b"), "PHONE") // Normalize phone numbers
            .replace(Regex("A/c\\s*\\w*(\\d{4})\\d*"), "A/c XX$1") // Normalize account numbers
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()
    }
    
    private fun getDefaultVocabulary(): List<String> {
        // Minimal vocabulary for SMS classification
        return listOf(
            PAD_TOKEN, UNK_TOKEN, CLS_TOKEN, SEP_TOKEN,
            // Common SMS words
            "otp", "code", "verification", "bank", "account", "amount",
            "rs", "credited", "debited", "upi", "transaction", "balance",
            "order", "delivered", "amazon", "flipkart", "offer", "discount",
            "winner", "prize", "congratulations", "click", "call", "urgent",
            "meeting", "lunch", "dinner", "birthday", "thanks", "please"
        )
    }
    
    data class TokenizationResult(
        val inputIds: IntArray,
        val attentionMask: IntArray
    )
}

/**
 * TensorFlow Lite inference engine for SMS classification
 */
class TFLiteInferenceEngine private constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "TFLiteInferenceEngine"
        private const val MODEL_FILE = "mobile_sms_classifier.tflite"
        private const val MODEL_VERSION = "1.0.0"
        
        // Thread-safe singleton
        @Volatile
        private var INSTANCE: TFLiteInferenceEngine? = null
        
        fun getInstance(context: Context): TFLiteInferenceEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TFLiteInferenceEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private var interpreter: Interpreter? = null
    private var tokenizer: SimpleSmsTokenizer? = null
    private var isModelLoaded = false
    
    // Performance monitoring
    private val inferenceStats = ConcurrentHashMap<String, Long>()
    private var totalInferences = 0L
    
    // Inference scope for background processing
    private val inferenceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    /**
     * Initialize the inference engine asynchronously
     */
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Initializing TensorFlow Lite inference engine...")
            
            // Load model
            val modelBuffer = loadModelFromAssets()
            if (modelBuffer == null) {
                Log.e(TAG, "Failed to load model from assets")
                return@withContext false
            }
            
            // Create interpreter with optimized options
            val options = Interpreter.Options().apply {
                setNumThreads(2) // Conservative threading for mobile
                setUseXNNPACK(true) // Enable XNNPACK delegate for better performance
            }
            
            interpreter = Interpreter(modelBuffer, options)
            
            // Initialize tokenizer
            tokenizer = SimpleSmsTokenizer(context)
            
            isModelLoaded = true
            Log.i(TAG, "TensorFlow Lite engine initialized successfully")
            
            // Log model information
            logModelInfo()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TensorFlow Lite engine", e)
            false
        }
    }
    
    /**
     * Classify SMS message asynchronously
     */
    suspend fun classifySms(smsText: String, sender: String = "Unknown"): ClassificationResult? {
        if (!isModelLoaded || interpreter == null || tokenizer == null) {
            Log.w(TAG, "Model not loaded. Call initialize() first.")
            return null
        }
        
        return withContext(Dispatchers.Default) {
            try {
                val startTime = System.currentTimeMillis()
                
                // Tokenize input
                val tokenization = tokenizer!!.tokenize(smsText)
                
                // Prepare input tensors
                val inputIds = Array(1) { tokenization.inputIds }
                val attentionMask = Array(1) { tokenization.attentionMask }
                
                // Prepare output tensor
                val outputArray = Array(1) { FloatArray(SmsCategory.values().size) }
                
                // Run inference
                interpreter!!.runForMultipleInputsOutputs(
                    arrayOf(inputIds, attentionMask),
                    mapOf(0 to outputArray)
                )
                
                val processingTime = System.currentTimeMillis() - startTime
                
                // Process results
                val probabilities = softmax(outputArray[0])
                val maxIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: 0
                val confidence = probabilities[maxIndex]
                val predictedCategory = SmsCategory.fromId(maxIndex)
                
                // Create probability map
                val probabilityMap = SmsCategory.values().associateWith { category ->
                    probabilities[category.id]
                }
                
                // Update statistics
                updateInferenceStats(processingTime, predictedCategory)
                
                ClassificationResult(
                    category = predictedCategory,
                    confidence = confidence,
                    probabilities = probabilityMap,
                    processingTimeMs = processingTime,
                    modelVersion = MODEL_VERSION
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during SMS classification", e)
                null
            }
        }
    }
    
    /**
     * Classify multiple SMS messages in batch
     */
    suspend fun classifySmsBatch(messages: List<Pair<String, String>>): List<ClassificationResult?> {
        return withContext(Dispatchers.Default) {
            messages.map { (text, sender) ->
                classifySms(text, sender)
            }
        }
    }
    
    /**
     * Get inference performance statistics
     */
    fun getPerformanceStats(): Map<String, Any> {
        val avgInferenceTime = if (totalInferences > 0) {
            inferenceStats.values.sum() / totalInferences
        } else 0L
        
        return mapOf(
            "totalInferences" to totalInferences,
            "averageInferenceTimeMs" to avgInferenceTime,
            "modelVersion" to MODEL_VERSION,
            "isModelLoaded" to isModelLoaded,
            "categoryStats" to inferenceStats.toMap()
        )
    }
    
    /**
     * Warm up the model with dummy inference
     */
    suspend fun warmUp() {
        Log.i(TAG, "Warming up model...")
        repeat(3) {
            classifySms("Test message for warm up")
        }
        Log.i(TAG, "Model warm-up completed")
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        interpreter?.close()
        interpreter = null
        tokenizer = null
        isModelLoaded = false
        inferenceScope.cancel()
        Log.i(TAG, "TensorFlow Lite engine cleaned up")
    }
    
    private fun loadModelFromAssets(): MappedByteBuffer? {
        return try {
            context.assets.openFd(MODEL_FILE).use { fileDescriptor ->
                FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                    val fileChannel = inputStream.channel
                    val startOffset = fileDescriptor.startOffset
                    val declaredLength = fileDescriptor.declaredLength
                    
                    fileChannel.map(
                        FileChannel.MapMode.READ_ONLY,
                        startOffset,
                        declaredLength
                    )
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error loading model from assets", e)
            null
        }
    }
    
    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f
        val expValues = logits.map { exp(it - maxLogit) }.toFloatArray()
        val sumExp = expValues.sum()
        return expValues.map { it / sumExp }.toFloatArray()
    }
    
    private fun updateInferenceStats(processingTime: Long, category: SmsCategory) {
        totalInferences++
        inferenceStats[category.name] = inferenceStats.getOrDefault(category.name, 0L) + processingTime
    }
    
    private fun logModelInfo() {
        interpreter?.let { interp ->
            val inputTensor = interp.getInputTensor(0)
            val outputTensor = interp.getOutputTensor(0)
            
            Log.i(TAG, "Model Info:")
            Log.i(TAG, "  Input shape: ${inputTensor.shape().contentToString()}")
            Log.i(TAG, "  Input type: ${inputTensor.dataType()}")
            Log.i(TAG, "  Output shape: ${outputTensor.shape().contentToString()}")
            Log.i(TAG, "  Output type: ${outputTensor.dataType()}")
        }
    }
}

/**
 * Factory class for creating inference engine instances
 */
object InferenceEngineFactory {
    
    /**
     * Create and initialize inference engine
     */
    suspend fun createEngine(context: Context): TFLiteInferenceEngine? {
        val engine = TFLiteInferenceEngine.getInstance(context)
        return if (engine.initialize()) {
            engine.warmUp()
            engine
        } else {
            null
        }
    }
}

/**
 * Extension functions for easier integration
 */

/**
 * Classify SMS with automatic fallback to rule-based classification
 */
suspend fun TFLiteInferenceEngine.classifyWithFallback(
    smsText: String,
    sender: String = "Unknown"
): ClassificationResult {
    
    // Try ML-based classification first
    classifySms(smsText, sender)?.let { result ->
        // If confidence is high enough, return ML result
        if (result.confidence > 0.7f) {
            return result
        }
    }
    
    // Fallback to rule-based classification
    return classifyWithRules(smsText, sender)
}

/**
 * Simple rule-based fallback classification
 */
private fun classifyWithRules(smsText: String, sender: String): ClassificationResult {
    val text = smsText.lowercase()
    val startTime = System.currentTimeMillis()
    
    val category = when {
        // OTP patterns
        text.contains("otp") || text.contains("verification") || 
        text.contains("code") && text.contains(Regex("\\b\\d{4,6}\\b")) -> SmsCategory.OTP
        
        // Banking patterns
        text.contains("debited") || text.contains("credited") || 
        text.contains("balance") || text.contains("upi") -> SmsCategory.BANKING
        
        // Spam patterns
        text.contains("congratulations") || text.contains("winner") ||
        text.contains("prize") || text.contains("urgent") -> SmsCategory.SPAM
        
        // E-commerce patterns
        text.contains("order") || text.contains("delivered") ||
        text.contains("amazon") || text.contains("flipkart") -> SmsCategory.ECOMMERCE
        
        // Personal patterns (simple heuristic)
        sender.startsWith("+") || text.length < 50 -> SmsCategory.INBOX
        
        // Default to needs review
        else -> SmsCategory.NEEDS_REVIEW
    }
    
    val processingTime = System.currentTimeMillis() - startTime
    
    // Create mock probabilities (rule-based has lower confidence)
    val probabilities = SmsCategory.values().associateWith { cat ->
        if (cat == category) 0.6f else 0.08f
    }
    
    return ClassificationResult(
        category = category,
        confidence = 0.6f,
        probabilities = probabilities,
        processingTimeMs = processingTime,
        modelVersion = "rule-based-fallback"
    )
}