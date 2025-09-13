
package com.example.smartsmsfilter

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.MappedByteBuffer
import java.nio.ByteOrder
import java.io.File
import java.io.IOException

/**
 * TensorFlow Lite Inference Engine for SMS Classification
 */
class TFLiteInferenceEngine(private val context: Context) {

    private var interpreter: Interpreter? = null
    private val maxWords = 5000
    private val maxLength = 60
    private val categories = listOf("INBOX", "SPAM", "OTP", "BANKING", "ECOMMERCE", "NEEDS_REVIEW")
    
    /**
     * Initialize the TFLite interpreter
     */
    init {
        try {
            val modelBuffer = loadModelFile("fixed_sms_classifier.tflite")
            val options = Interpreter.Options()
            interpreter = Interpreter(modelBuffer, options)
            println("TFLite model loaded successfully")
        } catch (e: Exception) {
            println("Error loading TFLite model: ${e.message}")
            // Fallback to rule-based classification if TFLite fails
        }
    }
    
    /**
     * Load TFLite model from assets
     */
    @Throws(IOException::class)
    private fun loadModelFile(modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    /**
     * Classify SMS message
     * @param message SMS text to classify
     * @return Pair of category and confidence score
     */
    fun classifySMS(message: String): Pair<String, Float> {
        // If TFLite model failed to load, use rule-based fallback
        if (interpreter == null) {
            return classifyWithRules(message)
        }
        
        try {
            // Preprocess input (tokenization would need custom implementation)
            // Here we're using a simplified approach for demonstration
            val input = preprocessInput(message)
            
            // Output buffer
            val outputBuffer = Array(1) { FloatArray(categories.size) }
            
            // Run inference
            interpreter?.run(input, outputBuffer)
            
            // Find category with highest confidence
            var maxIndex = 0
            var maxConfidence = outputBuffer[0][0]
            
            for (i in 1 until categories.size) {
                if (outputBuffer[0][i] > maxConfidence) {
                    maxConfidence = outputBuffer[0][i]
                    maxIndex = i
                }
            }
            
            return Pair(categories[maxIndex], maxConfidence)
            
        } catch (e: Exception) {
            println("Error during inference: ${e.message}")
            // Fallback to rule-based classification
            return classifyWithRules(message)
        }
    }
    
    /**
     * Preprocess input text for the model
     * Note: In a real implementation, you would use the same tokenizer used during training
     */
    private fun preprocessInput(message: String): Array<ByteBuffer> {
        // This is a placeholder for actual tokenization logic
        // You would need to implement proper tokenization matching your Python code
        
        val inputBuffer = ByteBuffer.allocateDirect(4 * maxLength)
        inputBuffer.order(ByteOrder.nativeOrder())
        
        // Clear buffer
        inputBuffer.rewind()
        
        // For simplicity, just populate with dummy data
        // In reality, you would tokenize the text using the same approach as in training
        for (i in 0 until maxLength) {
            inputBuffer.putFloat(0f)
        }
        
        // Prepare input array
        return Array(1) { inputBuffer }
    }
    
    /**
     * Rule-based classification as fallback
     */
    private fun classifyWithRules(message: String): Pair<String, Float> {
        val lowerMessage = message.toLowerCase()
        
        // Simple rule-based classification
        return when {
            lowerMessage.contains("otp") || lowerMessage.contains("code") || lowerMessage.contains("verification") -> 
                Pair("OTP", 0.8f)
                
            lowerMessage.contains("congratulations") || lowerMessage.contains("won") || 
                    lowerMessage.contains("offer") || lowerMessage.contains("discount") || 
                    lowerMessage.contains("free") || lowerMessage.contains("cash") -> 
                Pair("SPAM", 0.8f)
                
            lowerMessage.contains("debited") || lowerMessage.contains("credited") || 
                    lowerMessage.contains("account") || lowerMessage.contains("bank") || 
                    lowerMessage.contains("balance") -> 
                Pair("BANKING", 0.8f)
                
            lowerMessage.contains("order") || lowerMessage.contains("delivery") || 
                    lowerMessage.contains("shipped") || lowerMessage.contains("amazon") || 
                    lowerMessage.contains("flipkart") -> 
                Pair("ECOMMERCE", 0.8f)
                
            lowerMessage.contains("update") || lowerMessage.contains("profile") || 
                    lowerMessage.contains("service") || lowerMessage.contains("details") -> 
                Pair("NEEDS_REVIEW", 0.6f)
                
            else -> Pair("INBOX", 0.5f)
        }
    }
    
    /**
     * Close the interpreter when done
     */
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
