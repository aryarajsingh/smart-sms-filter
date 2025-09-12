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

        // 2) Contextual pass with recent messages for this sender
        val recent = try { repository.getMessagesByAddress(message.sender).first() } catch (_: Exception) { emptyList() }
        val contextualResult = contextual.classifyWithContext(
            message.copy(category = ruleCategory),
            recent
        )

        // 3) Blend with preference-aware threshold
        val threshold = when (prefs.filteringMode) {
            com.smartsmsfilter.data.preferences.FilteringMode.STRICT -> 0.5f
            com.smartsmsfilter.data.preferences.FilteringMode.MODERATE -> 0.6f
            com.smartsmsfilter.data.preferences.FilteringMode.LENIENT -> 0.7f
        }
        var usedContextual = false
        var finalCategory = if (contextualResult.confidence >= threshold) {
            usedContextual = true
            contextualResult.category
        } else {
            ruleCategory
        }

        // Prepare reasons, merging context with decision reason
        val reasons = mutableListOf<String>().apply { addAll(contextualResult.reasons) }
        if (!usedContextual) {
            reasons.add("Rule-based decision (low context confidence ${"%.2f".format(contextualResult.confidence)})")
        }

        // OTP guardrail: ensure OTPs never classified as spam
        val isOtp = ClassificationConstants.OTP_REGEXES.any { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(message.content) }
        if (isOtp) {
            if (finalCategory != MessageCategory.INBOX) {
                finalCategory = MessageCategory.INBOX
                if (reasons.none { it.contains("OTP", true) }) reasons.add("OTP detected")
            }
        }

        // Sender preferences override (pinned/auto-spam)
        try {
            val sp = repository.getSenderPreferences(message.sender)
            if (sp?.pinnedToInbox == true) {
                finalCategory = MessageCategory.INBOX
                reasons.add("Pinned sender")
            } else if (sp?.autoSpam == true && !isOtp) {
                finalCategory = MessageCategory.SPAM
                reasons.add("Sender marked auto-spam")
            }
            // Soft biasing based on reputation scores
            if (sp != null) {
                if (sp.importanceScore >= 0.75f && finalCategory != MessageCategory.SPAM) {
                    finalCategory = MessageCategory.INBOX
                    if (reasons.none { it.contains("importance", true) }) reasons.add("High sender importance")
                }
                if (sp.spamScore >= 0.75f && !isOtp) {
                    finalCategory = MessageCategory.SPAM
                    if (reasons.none { it.contains("spam score", true) }) reasons.add("High sender spam score")
                }
            }
        } catch (_: Exception) {}

        val classification = MessageClassification(
            category = finalCategory,
            confidence = contextualResult.confidence,
            reasons = reasons
        )

        // Store with final category
        val result = repository.insertMessage(message.copy(category = finalCategory))
        // Record audit (best-effort)
        try {
            val messageId = result.getOrNull()
            if (messageId != null) {
                repository.insertClassificationAudit(
                    messageId = messageId,
                    classifier = finalCategory,
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
     * Handle user correction by updating the stored category.
     * Learning hook can be implemented when message payload is available for local adaptation.
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
            // Try to fetch the message (best-effort: not exposed in repository; rely on caller to supply
            // or skip providing message content to contextual classifier learning in this pass.)
            // For now, learning requires original message payload; we cannot fetch by ID here without a method.
            // So this is a no-op placeholder unless caller provides full message.
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
