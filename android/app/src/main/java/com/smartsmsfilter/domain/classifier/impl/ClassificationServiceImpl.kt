package com.smartsmsfilter.domain.classifier.impl

import com.smartsmsfilter.domain.classifier.ClassificationService
import com.smartsmsfilter.domain.classifier.ClassificationProgress
import com.smartsmsfilter.domain.classifier.ClassifierConfig
import com.smartsmsfilter.domain.classifier.ClassificationStats
import com.smartsmsfilter.domain.classifier.SmsClassifier
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.MessageClassification
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.repository.SmsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation that orchestrates SMS classification using the injected classifier.
 * The specific classifier implementation (rule-based or ML) is determined by build variant.
 */
@Singleton
class ClassificationServiceImpl @Inject constructor(
    private val classifier: SmsClassifier,
    private val preferencesSource: com.smartsmsfilter.data.preferences.PreferencesSource,
    private val repository: SmsRepository
) : ClassificationService {

/**
     * Classify a single message using the injected classifier, honor sender preferences,
     * persist result, and write audit reasons.
     */
    override suspend fun classifyAndStore(message: SmsMessage): MessageClassification {
        try {
            // 1) Get base classification from the injected classifier (rule-based or ML)
            var classification = classifier.classifyMessage(message)
            val reasons = classification.reasons.toMutableList()
            var finalCategory = classification.category
            var confidence = classification.confidence
            
            // 2) Check for sender preference overrides
            try {
                val sp = repository.getSenderPreferences(message.sender)
                if (sp?.pinnedToInbox == true) {
                    finalCategory = MessageCategory.INBOX
                    confidence = 0.95f
                    reasons.add(0, "Sender pinned to inbox (user preference)")
                } else if (sp?.autoSpam == true) {
                    finalCategory = MessageCategory.SPAM
                    confidence = 0.95f
                    reasons.add(0, "Sender marked as auto-spam (user preference)")
                } else if (sp != null) {
                    // Apply reputation score soft adjustments
                    if (sp.importanceScore >= 0.75f && finalCategory != MessageCategory.SPAM) {
                        finalCategory = MessageCategory.INBOX
                        confidence = minOf(0.9f, confidence + 0.1f)
                        reasons.add("High sender importance score (${String.format("%.2f", sp.importanceScore)})")
                    } else if (sp.spamScore >= 0.75f) {
                        finalCategory = MessageCategory.SPAM
                        confidence = minOf(0.9f, confidence + 0.1f)
                        reasons.add("High sender spam score (${String.format("%.2f", sp.spamScore)})")
                    }
                }
            } catch (_: Exception) {
                // Sender preferences lookup failed, continue with classifier result
                reasons.add("Could not load sender preferences")
            }
            
            // 3) Store message with final category
            val result = repository.insertMessage(message.copy(category = finalCategory))
            val messageId = result.getOrNull()
            
            val finalClassification = MessageClassification(
                category = finalCategory,
                confidence = confidence,
                reasons = reasons,
                messageId = messageId
            )
            
            // 4) Record audit trail (best-effort)
            try {
                if (messageId != null && messageId > 0) {
                    repository.insertClassificationAudit(
                        messageId = messageId,
                        classifier = MessageCategory.INBOX, // Using a dummy value as classifier expects MessageCategory
                        category = finalCategory,
                        confidence = finalClassification.confidence,
                        reasons = finalClassification.reasons
                    )
                }
            } catch (_: Exception) {
                // Audit logging failure should not break the main flow
            }
            
            return finalClassification
            
        } catch (e: Exception) {
            android.util.Log.e("ClassificationServiceImpl", "Error in classifyAndStore", e)
            
            // Fallback: store as needs review
            val fallbackResult = repository.insertMessage(message.copy(category = MessageCategory.NEEDS_REVIEW))
            return MessageClassification(
                category = MessageCategory.NEEDS_REVIEW,
                confidence = 0.1f,
                reasons = listOf("Classification failed: ${e.message}"),
                messageId = fallbackResult.getOrNull()
            )
        }
    }

    override suspend fun reclassifyExistingMessages(): Flow<ClassificationProgress> = flow {
        // Placeholder: emit a simple progress once for now
        emit(ClassificationProgress(processed = 0, total = 0))
    }

    /**
     * Handle user correction by updating the stored category and triggering learning.
     * Delegates learning to the injected classifier implementation.
     */
    override suspend fun handleUserCorrection(
        messageId: Long,
        correctedCategory: MessageCategory,
        reason: String?
    ) {
        // Update category in DB via repository
        repository.moveToCategory(messageId, correctedCategory)

        val prefs = preferencesSource.userPreferences.first()
        if (prefs.enableLearningFromFeedback) {
            // Fetch the original message to enable learning from user correction
            try {
                val message = repository.getMessageById(messageId)
                if (message != null) {
                    // Delegate learning to the classifier implementation
                    classifier.learnFromCorrection(
                        message = message,
                        userCorrection = MessageClassification(
                            category = correctedCategory,
                            confidence = 1.0f,
                            reasons = listOfNotNull(reason ?: "User correction")
                        )
                    )
                }
            } catch (e: Exception) {
                // Learning failure should not break the correction flow
                android.util.Log.w("ClassificationServiceImpl", "Failed to learn from user correction", e)
            }
        }
    }

    override suspend fun getClassificationStats(): ClassificationStats {
        // Placeholder for now
        return ClassificationStats(
            totalClassified = 0,
            accuracyRate = 0f,
            confidenceDistribution = emptyMap(),
            categoryDistribution = emptyMap(),
            userCorrections = 0,
            averageProcessingTime = 0
        )
    }

    override suspend fun updateClassifierConfig(config: ClassifierConfig) {
        // No-op placeholder for now
    }
}
