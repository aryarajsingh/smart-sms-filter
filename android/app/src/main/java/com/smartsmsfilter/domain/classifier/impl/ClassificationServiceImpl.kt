package com.smartsmsfilter.domain.classifier.impl

import com.smartsmsfilter.classification.PrivateContextualClassifier
import com.smartsmsfilter.classification.SimpleMessageClassifier
import com.smartsmsfilter.classification.ClassificationConstants
import com.smartsmsfilter.data.preferences.PreferencesManager
import com.smartsmsfilter.domain.classifier.ClassificationService
import com.smartsmsfilter.domain.classifier.ClassificationProgress
import com.smartsmsfilter.domain.classifier.ClassifierConfig
import com.smartsmsfilter.domain.classifier.ClassificationStats
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
 * Concrete implementation that orchestrates rule-based and contextual classification.
 * - Rule-based classifier provides a fast baseline.
 * - Contextual classifier can adjust with recent-history context.
 */
@Singleton
class ClassificationServiceImpl @Inject constructor(
    private val simple: SimpleMessageClassifier,
    private val contextual: PrivateContextualClassifier,
    private val preferencesSource: com.smartsmsfilter.data.preferences.PreferencesSource,
    private val repository: SmsRepository
) : ClassificationService {

/**
     * Classify a single message using rule-based + contextual logic, honor OTP guardrails and
     * sender overrides, persist result, and write audit reasons.
     */
    override suspend fun classifyAndStore(message: SmsMessage): MessageClassification {
        // User prefs to tune blending
        val prefs = preferencesSource.userPreferences.first()

        // 1) Fast rule-based category
        val ruleCategory = simple.classifyMessage(message.sender, message.content)
        
        // 2) Check for hard overrides first (OTP, sender preferences)
        val reasons = mutableListOf<String>()
        var finalCategory = ruleCategory
        
        // OTP guardrail: ensure OTPs never classified as spam (highest priority)
        val isOtp = ClassificationConstants.OTP_REGEXES.any { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(message.content) }
        if (isOtp) {
            finalCategory = MessageCategory.INBOX
            reasons.add("OTP detected")
        } else {
            // Sender preferences override (pinned/auto-spam) - only if not OTP
            try {
                val sp = repository.getSenderPreferences(message.sender)
                if (sp?.pinnedToInbox == true) {
                    finalCategory = MessageCategory.INBOX
                    reasons.add("Pinned sender")
                } else if (sp?.autoSpam == true) {
                    finalCategory = MessageCategory.SPAM
                    reasons.add("Sender marked auto-spam")
                } else if (sp != null) {
                    // Soft biasing based on reputation scores if no hard override
                    if (sp.importanceScore >= 0.75f && finalCategory != MessageCategory.SPAM) {
                        finalCategory = MessageCategory.INBOX
                        reasons.add("High sender importance")
                    } else if (sp.spamScore >= 0.75f) {
                        finalCategory = MessageCategory.SPAM
                        reasons.add("High sender spam score")
                    }
                }
            } catch (_: Exception) {}
        }
        
        // 3) Only use contextual classification if no hard overrides applied
        if (reasons.isEmpty()) {
            // No hard overrides, proceed with contextual analysis
            val recent = try { repository.getMessagesByAddress(message.sender).first() } catch (_: Exception) { emptyList() }
            val contextualResult = contextual.classifyWithContext(
                message.copy(category = ruleCategory),
                recent
            )
            
            // Blend with preference-aware threshold
            val threshold = when (prefs.filteringMode) {
                com.smartsmsfilter.data.preferences.FilteringMode.STRICT -> 0.5f
                com.smartsmsfilter.data.preferences.FilteringMode.MODERATE -> 0.6f
                com.smartsmsfilter.data.preferences.FilteringMode.LENIENT -> 0.7f
            }
            
            if (contextualResult.confidence >= threshold) {
                finalCategory = contextualResult.category
                reasons.addAll(contextualResult.reasons)
            } else {
                // Use rule-based decision
                finalCategory = ruleCategory
                reasons.add("Rule-based decision (low context confidence ${"%.2f".format(contextualResult.confidence)})")
            }
        }

        val classification = MessageClassification(
            category = finalCategory,
            confidence = if (reasons.isEmpty()) 1.0f else 0.9f, // High confidence for hard overrides
            reasons = reasons
        )

        // Store with final category
        val result = repository.insertMessage(message.copy(category = finalCategory))
        // Record audit (best-effort)
        try {
            val messageId = result.getOrNull()
            if (messageId != null && messageId > 0) {
                repository.insertClassificationAudit(
                    messageId = messageId,
                    classifier = MessageCategory.INBOX, // Using a dummy value as classifier expects MessageCategory
                    category = finalCategory,
                    confidence = classification.confidence,
                    reasons = classification.reasons
                )
            }
        } catch (_: Exception) {}
        return classification
    }

    override suspend fun reclassifyExistingMessages(): Flow<ClassificationProgress> = flow {
        // Placeholder: emit a simple progress once for now
        emit(ClassificationProgress(processed = 0, total = 0))
    }

    /**
     * Handle user correction by updating the stored category and triggering learning.
     * Integrates with PrivateContextualClassifier for content-based learning.
     */
    override suspend fun handleUserCorrection(
        messageId: Long,
        correctedCategory: MessageCategory,
        reason: String?
    ) {
        // Update category in DB via repository (the use case usually does this; safe to call here too)
        repository.moveToCategory(messageId, correctedCategory)

        val prefs = preferencesSource.userPreferences.first()
        if (prefs.enableLearningFromFeedback) {
            // Fetch the original message to enable learning from user correction
            try {
                val message = repository.getMessageById(messageId)
                if (message != null) {
                    // Learn from the user correction for future similar messages
                    contextual.learnFromUserCorrection(
                        message = message,
                        originalCategory = message.category,
                        correctedCategory = correctedCategory
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
