package com.smartsmsfilter.classification

import com.smartsmsfilter.data.preferences.PreferencesManager
import com.smartsmsfilter.data.preferences.FilteringMode
import com.smartsmsfilter.data.preferences.ImportantMessageType
import com.smartsmsfilter.data.preferences.SpamTolerance
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.MessageClassification
import com.smartsmsfilter.domain.model.SmsMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.LinkedHashMap

/**
 * Privacy-First Contextual Classifier
 * 
 * PRIVACY GUARANTEE:
 * - 100% on-device processing
 * - NO data collection
 * - NO network calls
 * - NO analytics or telemetry
 * - User data NEVER leaves the device
 * - All learning stays local
 */
@Singleton
/**
 * Contextual classifier (privacy-first). Uses only on-device, ephemeral context.
 */
class PrivateContextualClassifier @Inject constructor(
    preferencesSource: com.smartsmsfilter.data.preferences.PreferencesSource
) {
    private val preferencesFlow = preferencesSource.userPreferences
    
    // === LOCAL CONTEXT STORAGE (In-Memory Only) ===
    // These are cleared when app closes - no persistent tracking
    private val senderHistory = LinkedHashMap<String, SenderContext>(100, 0.75f, true)
    private val conversationThreads = LinkedHashMap<String, ConversationContext>(50, 0.75f, true)
    private val userCorrections = LinkedHashMap<String, Int>(100, 0.75f, true)
    
    // Maximum entries to prevent memory issues
    init {
        // Limit memory usage - old entries auto-removed
        if (senderHistory.size > 100) {
            val iterator = senderHistory.entries.iterator()
            repeat(20) { if (iterator.hasNext()) iterator.remove() }
        }
    }
    
    /**
     * Main classification method with contextual analysis
     * ALL PROCESSING HAPPENS ON-DEVICE
     */
    suspend fun classifyWithContext(
        message: SmsMessage,
        recentMessages: List<SmsMessage> = emptyList(),
        updateContext: Boolean = true  // Allow disabling context updates for explanation queries
    ): MessageClassification {
        val preferences = preferencesFlow.first()
        
        // Build context from recent messages (on-device only)
        val context = buildLocalContext(message, recentMessages)
        
        // Analyze with context
        val category = analyzeWithContext(message, context, preferences)
        val confidence = calculateContextualConfidence(message, context, category)
        val reasons = generateReasons(message, context, category)
        
        // Only update local context if this is a real classification, not an explanation query
        if (updateContext) {
            updateLocalContext(message, category)
        }
        
        return MessageClassification(
            category = category,
            confidence = confidence,
            reasons = reasons
        )
    }
    
    /**
     * Build context from local data only
     */
    private fun buildLocalContext(
        message: SmsMessage,
        recentMessages: List<SmsMessage>
    ): MessageContext {
        val senderCtx = senderHistory[message.sender]
        val conversationCtx = conversationThreads[message.sender]
        
        // Analyze recent patterns (last 24 hours only)
        val recentFromSameSender = recentMessages
            .filter { it.sender == message.sender }
            .take(10) // Limit for performance
        
        val timeOfDay = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        
        return MessageContext(
            sender = message.sender,
            senderHistory = senderCtx,
            conversationHistory = conversationCtx,
            recentMessages = recentFromSameSender,
            timeOfDay = timeOfDay,
            dayOfWeek = dayOfWeek,
            messageFrequency = calculateFrequency(recentFromSameSender),
            isFirstTimeSender = senderCtx == null
        )
    }
    
    /**
     * Analyze message with full context
     */
    private fun analyzeWithContext(
        message: SmsMessage,
        context: MessageContext,
        preferences: com.smartsmsfilter.data.preferences.UserPreferences
    ): MessageCategory {
        val content = message.content.lowercase()
        val sender = message.sender.uppercase()
        
        // === CONTEXTUAL RULES ===
        
        // 1. OTP Context - Time sensitive
        if (isOTP(content)) {
            // OTPs are always important regardless of sender history
            return MessageCategory.INBOX
        }
        
        // 2. Transaction Follow-ups
        if (isTransactionMessage(content)) {
            // Check if there was a recent OTP from same sender
            if (context.recentMessages.any { isOTP(it.content) }) {
                // Likely a transaction confirmation after OTP
                return MessageCategory.INBOX
            }
        }
        
        // 3. Conversation Threading
        if (context.conversationHistory != null) {
            // If user previously marked messages from this sender as important
            if (context.conversationHistory.userImportanceScore > 0.7) {
                return MessageCategory.INBOX
            }
            // If user consistently marks as spam
            if (context.conversationHistory.userSpamScore > 0.7) {
                return MessageCategory.SPAM
            }
        }
        
        // 4. Time-based Context
        if (context.timeOfDay in 9..18 && context.dayOfWeek in 2..6) {
            // Business hours - more likely to be important
            if (isBankingRelated(sender, content)) {
                return MessageCategory.INBOX
            }
        }
        
        // 5. Frequency Analysis
        if (context.messageFrequency > 5) {
            // Too many messages in short time = likely spam
            if (!isImportantSender(sender)) {
                return MessageCategory.SPAM
            }
        }
        
        // 6. First-time Sender Analysis
        if (context.isFirstTimeSender) {
            // Be more careful with unknown senders
            if (hasSpamIndicators(content)) {
                return MessageCategory.SPAM
            }
            // Short-link from unknown sender is highly suspicious
            if (hasShortLink(content)) {
                return MessageCategory.SPAM
            }
            // Unknown sender with unclear intent
            if (!hasImportantIndicators(content)) {
                return MessageCategory.NEEDS_REVIEW
            }
        }
        
        // 7. User Preference Based Classification
        return classifyByPreferences(message, preferences)
    }
    
    /**
     * Pattern detection methods (all local processing)
     */
    private fun isOTP(content: String): Boolean {
        return ClassificationConstants.OTP_REGEXES.any { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(content) }
    }
    
    private fun isTransactionMessage(content: String): Boolean {
        val keywords = listOf(
            "credited", "debited", "balance", "transaction",
            "payment", "transfer", "withdrawn", "deposited",
            "upi", "neft", "imps", "rtgs"
        )
        return keywords.any { content.contains(it, ignoreCase = true) }
    }
    
    private fun isBankingRelated(sender: String, content: String): Boolean {
        val bankKeywords = listOf(
            "bank", "hdfc", "icici", "sbi", "axis", "kotak",
            "pnb", "bob", "canara", "union", "federal"
        )
        return bankKeywords.any { 
            sender.contains(it, ignoreCase = true) || 
            content.contains(it, ignoreCase = true)
        }
    }
    
    private fun isImportantSender(sender: String): Boolean {
        val important = listOf(
            "bank", "gov", "uidai", "epfo", "irctc", "cowin",
            "police", "court", "hospital", "school", "college"
        )
        return important.any { sender.contains(it, ignoreCase = true) }
    }

    private fun isPhoneNumber(sender: String): Boolean {
        val digits = sender.replace(Regex("[+\\-\\s()]"), "")
        return digits.all { it.isDigit() } && digits.length >= 10
    }
    
    private fun hasSpamIndicators(content: String): Boolean {
        val spamCount = ClassificationConstants.PROMO_KEYWORDS.count { content.contains(it, ignoreCase = true) }
        return spamCount >= 2
    }

    private fun hasShortLink(content: String): Boolean {
        return ClassificationConstants.SHORT_LINK_DOMAINS.any { content.contains(it, ignoreCase = true) } || content.contains("http", ignoreCase = true)
    }
    
    private fun hasImportantIndicators(content: String): Boolean {
        val importantWords = listOf(
            "urgent", "important", "action required",
            "verify", "confirm", "appointment", "scheduled",
            "delivery", "order", "booking", "ticket"
        )
        return importantWords.any { content.contains(it, ignoreCase = true) }
    }
    
    /**
     * Calculate frequency of messages
     */
    private fun calculateFrequency(messages: List<SmsMessage>): Int {
        if (messages.isEmpty()) return 0
        
        val now = System.currentTimeMillis()
        val hourAgo = now - (60 * 60 * 1000)
        
        return messages.count { it.timestamp.time > hourAgo }
    }
    
    /**
     * Classify based on user preferences
     */
    private fun classifyByPreferences(
        message: SmsMessage,
        preferences: com.smartsmsfilter.data.preferences.UserPreferences
    ): MessageCategory {
        val content = message.content.lowercase()
        val sender = message.sender
        val userImportant = preferences.importantMessageTypes

        // Respect user-important types (align with onboarding)
        if (userImportant.contains(com.smartsmsfilter.data.preferences.ImportantMessageType.BANKING) && isBankingRelated(sender, content)) {
            return MessageCategory.INBOX
        }
        if (userImportant.contains(com.smartsmsfilter.data.preferences.ImportantMessageType.ECOMMERCE)) {
            if (ClassificationConstants.ECOMMERCE_KEYWORDS.any { content.contains(it) }) return MessageCategory.INBOX
        }
        if (userImportant.contains(com.smartsmsfilter.data.preferences.ImportantMessageType.TRAVEL)) {
            if (ClassificationConstants.TRAVEL_KEYWORDS.any { content.contains(it) }) return MessageCategory.INBOX
        }
        if (userImportant.contains(com.smartsmsfilter.data.preferences.ImportantMessageType.UTILITIES)) {
            if (ClassificationConstants.UTILITIES_KEYWORDS.any { content.contains(it) }) return MessageCategory.INBOX
        }
        if (userImportant.contains(com.smartsmsfilter.data.preferences.ImportantMessageType.PERSONAL) && isPhoneNumber(sender)) {
            return MessageCategory.INBOX
        }

        // Simplified classification based on preferences
        val score = calculateSpamScore(message.content)
        val base = when (preferences.spamTolerance) {
            SpamTolerance.LOW -> 30
            SpamTolerance.MODERATE -> 50
            SpamTolerance.HIGH -> 70
        }
        val modeAdj = when (preferences.filteringMode) {
            FilteringMode.STRICT -> -10
            FilteringMode.MODERATE -> 0
            FilteringMode.LENIENT -> 15
        }
        val threshold = (base + modeAdj).coerceAtLeast(10)
        
        return when {
            score >= threshold -> MessageCategory.SPAM
            score >= (threshold * 0.6) -> MessageCategory.NEEDS_REVIEW
            else -> MessageCategory.INBOX
        }
    }
    
    private fun calculateSpamScore(content: String): Int {
        var score = 0
        
        // Check various spam indicators
        if (content.contains("free", ignoreCase = true)) score += 20
        if (content.contains("offer", ignoreCase = true)) score += 15
        if (content.contains("discount", ignoreCase = true)) score += 12
        if (content.contains("deal", ignoreCase = true)) score += 10
        if (content.contains("win", ignoreCase = true) || content.contains("prize", ignoreCase = true) || content.contains("lottery", ignoreCase = true)) score += 18
        if (content.contains("congratulations", ignoreCase = true)) score += 12
        if (content.contains("limited time", ignoreCase = true)) score += 10
        if (content.contains("unsubscribe", ignoreCase = true)) score += 10
        if (content.contains("click", ignoreCase = true)) score += 25
        if (content.contains("call now", ignoreCase = true)) score += 30
        if (content.count { it == '!' } > 2) score += 15

        // Links
        if (content.contains("http", ignoreCase = true)) score += 20
        val shortDomains = listOf("bit.ly", "tinyurl", "t.co", "goo.gl", "ow.ly")
        if (shortDomains.any { content.contains(it, ignoreCase = true) }) score += 15
        
        return minOf(score, 100)
    }
    
    /**
     * Calculate confidence based on context
     */
    private fun calculateContextualConfidence(
        message: SmsMessage,
        context: MessageContext,
        category: MessageCategory
    ): Float {
        var confidence = 0.5f
        
        // Increase confidence based on context signals
        if (!context.isFirstTimeSender) confidence += 0.1f
        if (context.senderHistory?.consistentCategory == category) confidence += 0.2f
        if (context.conversationHistory != null) confidence += 0.15f
        
        // Specific category confidence
        when (category) {
            MessageCategory.INBOX -> {
                if (isOTP(message.content)) confidence = 0.95f
                else if (isTransactionMessage(message.content)) confidence = 0.85f
            }
            MessageCategory.SPAM -> {
                if (hasSpamIndicators(message.content)) confidence += 0.2f
                if (hasShortLink(message.content)) confidence += 0.2f
                if (context.messageFrequency > 5) confidence += 0.15f
            }
            MessageCategory.NEEDS_REVIEW -> {
                confidence = 0.4f // Lower confidence for uncertain
            }
        }
        
        return minOf(confidence, 1.0f)
    }
    
    /**
     * Generate human-readable reasons for classification
     */
    private fun generateReasons(
        message: SmsMessage,
        context: MessageContext,
        category: MessageCategory
    ): List<String> {
        val reasons = mutableListOf<String>()
        
        when (category) {
            MessageCategory.INBOX -> {
                if (isOTP(message.content)) reasons.add("OTP detected")
                if (isTransactionMessage(message.content)) reasons.add("Transaction message")
                if (!context.isFirstTimeSender) reasons.add("Known sender")
            }
            MessageCategory.SPAM -> {
                if (hasSpamIndicators(message.content)) reasons.add("Spam keywords found")
                if (hasShortLink(message.content)) reasons.add("Short link detected")
                if (context.messageFrequency > 5) reasons.add("High message frequency")
                if (context.isFirstTimeSender) reasons.add("Unknown promotional sender")
            }
            MessageCategory.NEEDS_REVIEW -> {
                reasons.add("Uncertain classification")
                if (context.isFirstTimeSender) reasons.add("First-time sender")
            }
        }
        
        return reasons
    }
    
    /**
     * Update local context (memory only, no persistence)
     */
    private fun updateLocalContext(message: SmsMessage, category: MessageCategory) {
        // Update sender history (in-memory only)
        val existing = senderHistory[message.sender]
        senderHistory[message.sender] = SenderContext(
            lastSeen = Date(),
            messageCount = (existing?.messageCount ?: 0) + 1,
            consistentCategory = if (existing?.consistentCategory == category) category else null,
            lastCategory = category
        )
        
        // Limit memory usage
        if (senderHistory.size > 100) {
            senderHistory.remove(senderHistory.keys.first())
        }
    }
    
    /**
     * Learn from user correction (local only, no data sent)
     */
    fun learnFromUserCorrection(
        message: SmsMessage,
        originalCategory: MessageCategory,
        correctedCategory: MessageCategory
    ) {
        // Store correction locally (memory only)
        val key = "${message.sender}_${originalCategory}_${correctedCategory}"
        userCorrections[key] = (userCorrections[key] ?: 0) + 1
        
        // Update sender context
        val senderCtx = senderHistory[message.sender]
        if (senderCtx != null) {
            // Adjust importance scores based on correction
            if (correctedCategory == MessageCategory.INBOX) {
                conversationThreads[message.sender] = ConversationContext(
                    userImportanceScore = 0.8f,
                    userSpamScore = 0.2f
                )
            } else if (correctedCategory == MessageCategory.SPAM) {
                conversationThreads[message.sender] = ConversationContext(
                    userImportanceScore = 0.2f,
                    userSpamScore = 0.8f
                )
            }
        }
    }
    
    /**
     * Clear all context (privacy feature)
     */
    fun clearAllContext() {
        senderHistory.clear()
        conversationThreads.clear()
        userCorrections.clear()
    }
    
    // === DATA CLASSES (Local Only) ===
    
    private data class MessageContext(
        val sender: String,
        val senderHistory: SenderContext?,
        val conversationHistory: ConversationContext?,
        val recentMessages: List<SmsMessage>,
        val timeOfDay: Int,
        val dayOfWeek: Int,
        val messageFrequency: Int,
        val isFirstTimeSender: Boolean
    )
    
    private data class SenderContext(
        val lastSeen: Date,
        val messageCount: Int,
        val consistentCategory: MessageCategory?,
        val lastCategory: MessageCategory
    )
    
    private data class ConversationContext(
        val userImportanceScore: Float,
        val userSpamScore: Float
    )
}
