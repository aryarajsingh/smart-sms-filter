package com.smartsmsfilter.domain.usecase

import com.smartsmsfilter.classification.ClassificationConstants
import com.smartsmsfilter.classification.PrivateContextualClassifier
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.repository.SmsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class ExplainMessageUseCase @Inject constructor(
    private val contextual: PrivateContextualClassifier,
    private val repository: SmsRepository
) {
    /**
     * Cohesive explanation generator combining:
     * - Manual/user overrides and sender preferences
     * - Hard rules (OTP)
     * - Contextual classifier reasons (local, on-demand)
     */
    suspend operator fun invoke(message: SmsMessage): List<String> {
        val reasons = mutableListOf<String>()

        // Manual override
        message.manualCategoryOverride?.let { overrideCat ->
            reasons += "Manually moved to ${overrideCat.name.lowercase()}"
        }
        // Marked important flag
        if (message.isImportant) reasons += "Marked important"

        // Sender preferences (pinned/auto-spam)
        try {
            val sp = repository.getSenderPreferences(message.sender)
            if (sp?.pinnedToInbox == true) reasons += "Pinned sender"
            if (sp?.autoSpam == true) reasons += "Sender marked auto-spam"
        } catch (_: Exception) {}

        // OTP hard rule
        val isOtp = ClassificationConstants.OTP_REGEXES.any { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(message.content) }
        if (isOtp) reasons += "OTP detected"

        // Contextual reasons (on-demand, not persisted)
        // IMPORTANT: Set updateContext=false to prevent explanation queries from modifying sender history
        val recent = try { repository.getMessagesByAddress(message.sender).first() } catch (_: Exception) { emptyList() }
        val contextualResult = contextual.classifyWithContext(message, recent, updateContext = false)
        contextualResult.reasons.forEach { r -> if (r.isNotBlank() && r !in reasons) reasons += r }

        // Map to human-friendly, deduped, and ordered copy
        val ordered = orderAndMap(reasons, message.category)
        return ordered
    }

    private fun orderAndMap(raw: List<String>, category: MessageCategory): List<String> {
        if (raw.isEmpty()) {
            val cat = when (category) {
                MessageCategory.INBOX -> "inbox"
                MessageCategory.SPAM -> "spam"
                MessageCategory.NEEDS_REVIEW -> "needs review"
            }
            return listOf("Placed in $cat based on available signals")
        }
        // Normalize and dedupe (case-insensitive)
        val seen = mutableSetOf<String>()
        val mapped = raw.mapNotNull { mapCopy(it.trim()) }
            .filter { it.isNotBlank() }
            .filter { seen.add(it.lowercase()) }

        // Order by priority
        val priority = listOf(
            "Manually moved" to 1,
            "Marked important" to 2,
            "Pinned sender" to 3,
            "Sender marked auto-spam" to 4,
            "OTP detected" to 5,
            "Transaction" to 6,
            "Known sender" to 7,
            "First-time sender" to 8,
            "Spam keywords" to 9,
            "High message frequency" to 10,
            "Promotional signals" to 11,
            "Uncertain classification" to 99
        )
        val score = { s: String -> priority.firstOrNull { s.startsWith(it.first, ignoreCase = true) }?.second ?: 50 }
        return mapped.sortedBy { score(it) }
    }

    private fun mapCopy(input: String): String? {
        val s = input.lowercase()
        return when {
            s.startsWith("manually moved") -> "Manually moved"
            s.contains("marked as important") || s.contains("marked important") -> "Marked important"
            s.contains("pinned sender") || s.contains("pinned to inbox") -> "Pinned sender"
            s.contains("auto-spam") -> "Sender marked auto-spam"
            s.contains("otp") -> "OTP detected"
            s.contains("transaction") -> "Transaction message"
            s.contains("known sender") -> "Known sender"
            s.contains("first-time sender") || s.contains("first time sender") -> "First-time sender"
            s.contains("spam keywords") -> "Spam keywords found"
            s.contains("high message frequency") || s.contains("frequency") -> "High message frequency"
            s.contains("unknown promotional sender") || s.contains("promo") -> "Promotional signals"
            s.contains("uncertain classification") -> "Uncertain classification"
            s.startsWith("placed in") -> input.replaceFirstChar { it.uppercase() }
            else -> input.replaceFirstChar { it.uppercase() }
        }
    }
}
