package com.smartsmsfilter.domain.classifier

import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.MessageClassification
import com.smartsmsfilter.domain.model.SmsMessage
import kotlinx.coroutines.flow.Flow

/**
 * High-level service for SMS classification that orchestrates
 * multiple classifiers and handles the complete classification pipeline
 */
interface ClassificationService {
    
    /**
     * Classifies a newly received SMS message and updates the database
     * This is the main entry point for real-time classification
     */
    suspend fun classifyAndStore(message: SmsMessage): MessageClassification
    
    /**
     * Re-classifies existing messages (useful for improving classifications)
     */
    suspend fun reclassifyExistingMessages(): Flow<ClassificationProgress>
    
    /**
     * Handles user feedback and triggers learning
     */
    suspend fun handleUserCorrection(
        messageId: Long, 
        correctedCategory: MessageCategory,
        reason: String? = null
    )
    
    /**
     * Gets classification statistics for analytics
     */
    suspend fun getClassificationStats(): ClassificationStats
    
    /**
     * Updates the active classifier configuration
     */
    suspend fun updateClassifierConfig(config: ClassifierConfig)
}

/**
 * Progress tracking for batch classification operations
 */
data class ClassificationProgress(
    val processed: Int,
    val total: Int,
    val currentMessage: String? = null,
    val errors: List<String> = emptyList()
)

/**
 * Classification statistics for monitoring and analytics
 */
data class ClassificationStats(
    val totalClassified: Int,
    val accuracyRate: Float,
    val confidenceDistribution: Map<String, Int>, // Low, Medium, High confidence counts
    val categoryDistribution: Map<MessageCategory, Int>,
    val userCorrections: Int,
    val averageProcessingTime: Long // in milliseconds
)

/**
 * Configuration for the classification system
 */
data class ClassifierConfig(
    val enableRuleBasedClassifier: Boolean = true,
    val enableAiClassifier: Boolean = true,
    val confidenceThreshold: Float = 0.7f,
    val fallbackToRules: Boolean = true,
    val learningEnabled: Boolean = true,
    val batchSize: Int = 50
)
