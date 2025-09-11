package com.smartsmsfilter.domain.classifier

import com.smartsmsfilter.domain.model.MessageClassification
import com.smartsmsfilter.domain.model.SmsMessage

/**
 * Core interface for SMS message classification
 */
interface SmsClassifier {
    
    /**
     * Classifies a single SMS message
     * @param message The SMS message to classify
     * @return Classification result with category, confidence, and reasoning
     */
    suspend fun classifyMessage(message: SmsMessage): MessageClassification
    
    /**
     * Batch classify multiple messages (useful for initial setup)
     * @param messages List of SMS messages to classify
     * @return Map of message IDs to their classifications
     */
    suspend fun classifyBatch(messages: List<SmsMessage>): Map<Long, MessageClassification>
    
    /**
     * Learn from user corrections to improve future classifications
     * @param message The original message
     * @param userCorrection The category the user assigned
     */
    suspend fun learnFromCorrection(message: SmsMessage, userCorrection: MessageClassification)
    
    /**
     * Get the classifier's current confidence threshold
     * Messages below this threshold should go to "Needs Review"
     */
    fun getConfidenceThreshold(): Float
}

/**
 * Enum for different types of classifiers
 */
enum class ClassifierType {
    RULE_BASED,     // Pattern and keyword-based classification
    AI_MODEL,       // TensorFlow Lite neural network
    HYBRID          // Combination of rule-based and AI
}

/**
 * Classification context provides additional information
 * that might be useful for classification decisions
 */
data class ClassificationContext(
    val isFirstMessage: Boolean = false,
    val senderHistory: List<SmsMessage> = emptyList(),
    val timeOfDay: Int = 0, // Hour of day (0-23)
    val dayOfWeek: Int = 0, // Day of week (1-7)
    val userFeedbackHistory: Map<String, Int> = emptyMap() // Sender -> correction count
)
