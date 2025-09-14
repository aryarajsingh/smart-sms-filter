package com.smartsmsfilter.classification

import com.smartsmsfilter.data.preferences.PreferencesManager
import com.smartsmsfilter.data.preferences.FilteringMode
import com.smartsmsfilter.data.preferences.ImportantMessageType
import com.smartsmsfilter.data.preferences.SpamTolerance
import com.smartsmsfilter.domain.model.MessageCategory
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
/**
 * Fast, rule-based classifier leveraging onboarding preferences.
 * Keeps logic simple, with OTP and important-type short-circuits.
 * Integrates with contact manager to trust known contacts.
 */
class SimpleMessageClassifier @Inject constructor(
    preferencesSource: com.smartsmsfilter.data.preferences.PreferencesSource,
    private val contactManager: com.smartsmsfilter.data.contacts.ContactManager
) {

    private val preferencesFlow = preferencesSource.userPreferences

    companion object {
        // Banking and financial keywords - usually important
        private val BANKING_KEYWORDS = setOf(
            "bank", "banking", "account", "balance", "transaction", "payment", "transfer", 
            "atm", "card", "credit", "debit", "loan", "emi", "interest", "deposit", "withdraw"
        )
        
        // OTP and verification - always important
        private val OTP_KEYWORDS = setOf(
            "otp", "verification", "verify", "code", "authenticate", "confirm", "activation",
            "pin", "password", "secure", "login", "access"
        )
        
        // Spam indicators - usually filtered
        private val SPAM_KEYWORDS = setOf(
            "win", "winner", "congratulations", "free", "offer", "discount", "sale", "deal",
            "limited time", "hurry", "click here", "call now", "urgent", "prize", "lottery",
            "cash", "money back", "guarantee", "risk free", "no cost", "bonus"
        )
        
        // Promotional keywords - usually filtered  
        private val PROMOTIONAL_KEYWORDS = setOf(
            "promotion", "promotional", "marketing", "advertisement", "ad-", "unsubscribe",
            "subscribe", "offer", "deals", "shopping", "buy now", "order now", "cashback"
        )
        
        // Trusted sender patterns
        private val TRUSTED_SENDERS = setOf(
            "SBIINB", "HDFCBANK", "ICICIBANK", "AXISBANK", "KOTAKBANK", "YESBANK", "PNBINB",
            "UNIONBANK", "CANBANK", "BOBCARD", "AMZN", "FLIPKART", "PAYTM", "GPAY", "PHONEPE",
            "UBER", "OLA", "SWIGGY", "ZOMATO", "AIRTEL", "JIO", "VODAFONE", "BSNL"
        )
        
        // Spam sender patterns
        private val SPAM_SENDERS = setOf(
            "VM-", "VK-", "VL-", "VN-", "VP-", "VR-", "VS-", "VT-", "VU-", "VV-", "VW-", "VX-", "VY-", "VZ-",
            "RM-", "RN-", "RP-", "RQ-", "RR-", "RS-", "RT-", "RU-", "RV-", "RW-", "RX-", "RY-", "RZ-"
        )
        
        // E-commerce keywords
        private val ECOMMERCE_KEYWORDS = setOf(
            "order", "delivery", "shipped", "dispatch", "courier", "tracking", "cod", 
            "flipkart", "amazon", "myntra", "ajio", "cart", "purchase", "refund"
        )
        
        // Travel keywords
        private val TRAVEL_KEYWORDS = setOf(
            "flight", "pnr", "booking", "ticket", "travel", "hotel", "train", "bus", 
            "makemytrip", "goibibo", "irctc", "redbus", "confirmation", "boarding"
        )
        
        // Utilities keywords
        private val UTILITIES_KEYWORDS = setOf(
            "electricity", "water", "gas", "internet", "mobile", "recharge", "bill", "due", 
            "payment", "reminder", "service", "maintenance", "outage", "disconnection"
        )
    }

    /**
     * Classify a message based on user preferences from onboarding
     */
    suspend fun classifyMessage(sender: String, content: String): MessageCategory {
        return try {
            val preferences = preferencesFlow.first()
            classifyWithPreferences(sender, content, preferences)
        } catch (e: Exception) {
            // Fallback to INBOX on error (safer than spam or review)
            MessageCategory.INBOX
        }
    }
    
    /**
     * Check if sender is a known contact by looking up in phone contacts
     */
    private suspend fun isKnownContact(sender: String): Boolean {
        return try {
            val contact = contactManager.getContactByPhoneNumber(sender)
            // If we get a contact back with a real ID (not 0), it's a known contact
            contact?.id != null && contact.id > 0
        } catch (e: Exception) {
            // If contact lookup fails, assume NOT a known contact
            // We don't want to treat all phone numbers as known contacts
            false
        }
    }

    private suspend fun classifyWithPreferences(
        sender: String, 
        content: String, 
        preferences: com.smartsmsfilter.data.preferences.UserPreferences
    ): MessageCategory {
        // Guard against empty strings
        if (sender.isBlank() && content.isBlank()) {
            return MessageCategory.SPAM
        }
        
        val contentLower = content.lowercase()
        val senderUpper = sender.uppercase()

        // === CLASSIFICATION PRIORITY ORDER (HIGHEST TO LOWEST) ===
        
        // 1. EXPLICIT SPAM WARNINGS - Always block these first
        if (contentLower.contains("airtel warning: spam") || 
            contentLower.contains("airtel warning : spam") ||
            contentLower.contains("warning: spam") ||
            contentLower.contains("spam alert")) {
            return MessageCategory.SPAM
        }
        
        // 2. KNOWN CONTACTS - Always trust people in user's phonebook (unless explicit spam warning above)
        if (isKnownContact(sender)) {
            return MessageCategory.INBOX
        }
        
        // 3. OTP MESSAGES - Always critical for security
        if (isOtp(content)) {
            return MessageCategory.INBOX
        }
        
        // 4. TRUSTED SERVICE SENDERS - Known legitimate services
        if (TRUSTED_SENDERS.any { senderUpper.contains(it) }) {
            return MessageCategory.INBOX
        }
        
        // 5. USER PREFERENCE CATEGORIES - Messages user wants in inbox
        val userImportantTypes = preferences.importantMessageTypes
        
        // Note: Sender preferences (pinned/auto-spam) are checked in ClassificationServiceImpl
        // This keeps the rule-based classifier focused on content and the service layer handles user learning
        
        // Banking - check if user considers banking messages important
        if (userImportantTypes.contains(ImportantMessageType.BANKING) && 
            containsAnyKeyword(contentLower, BANKING_KEYWORDS)) {
            return MessageCategory.INBOX
        }
        
        // E-commerce - check if user wants e-commerce notifications
        if (userImportantTypes.contains(ImportantMessageType.ECOMMERCE) && 
            containsAnyKeyword(contentLower, ECOMMERCE_KEYWORDS)) {
            return MessageCategory.INBOX
        }
        
        // Travel - check if user wants travel updates
        if (userImportantTypes.contains(ImportantMessageType.TRAVEL) && 
            containsAnyKeyword(contentLower, TRAVEL_KEYWORDS)) {
            return MessageCategory.INBOX
        }
        
        // Utilities - check if user wants service notifications
        if (userImportantTypes.contains(ImportantMessageType.UTILITIES) && 
            containsAnyKeyword(contentLower, UTILITIES_KEYWORDS)) {
            return MessageCategory.INBOX
        }
        
        // Personal messages from phone numbers (if user selected this preference)
        if (userImportantTypes.contains(ImportantMessageType.PERSONAL) && 
            isPhoneNumber(sender)) {
            return MessageCategory.INBOX
        }
        
        // 6. SPAM DETECTION - Apply content-based spam filtering
        val spamScore = calculateSpamScore(contentLower, senderUpper, content)
        val threshold = getSpamThreshold(preferences.spamTolerance, preferences.filteringMode)
        
        if (spamScore >= threshold) {
            return MessageCategory.SPAM
        }
        
        // 7. DEFAULT - If no rules match, needs human review
        return MessageCategory.NEEDS_REVIEW
    }

    private fun containsAnyKeyword(text: String, keywords: Set<String>): Boolean {
        return keywords.any { keyword ->
            text.contains(keyword)
        }
    }

    private fun isOtp(content: String): Boolean {
        return try {
            ClassificationConstants.OTP_REGEXES.any { 
                Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(content) 
            }
        } catch (e: Exception) {
            // If regex fails, check for simple patterns
            content.lowercase().contains("otp") || 
            content.lowercase().contains("verification") ||
            content.lowercase().contains("code")
        }
    }

    private fun isPhoneNumber(sender: String): Boolean {
        // Simple check for phone number format
        if (sender.isBlank()) return false
        val digitsOnly = sender.replace(Regex("[+\\-\\s()]"), "")
        return digitsOnly.isNotEmpty() && 
               digitsOnly.all { it.isDigit() } && 
               digitsOnly.length >= 10
    }
    
    /**
     * Calculate a spam score for the message (0-100)
     * Higher score = more likely to be spam
     */
    private fun calculateSpamScore(contentLower: String, senderUpper: String, originalContent: String): Int {
        var score = 0
        
        // Check spam keywords (each adds points)
        val spamKeywordCount = SPAM_KEYWORDS.count { contentLower.contains(it) }
        score += spamKeywordCount * 25 // Each spam keyword = 25 points
        
        // Check promotional keywords (lighter penalty)
        val promoKeywordCount = PROMOTIONAL_KEYWORDS.count { contentLower.contains(it) }
        score += promoKeywordCount * 15 // Each promo keyword = 15 points
        
        // Suspicious sender patterns (DLT route prefixes like AD-/DM-/VM- should NOT be treated as spam by default)
        // Only lightly penalize classic ad routes while avoiding false positives for legitimate DLT senders
        if (senderUpper.startsWith("AD-") || senderUpper.startsWith("DM-")) {
            score += 10 // reduced from 30 to 10 to avoid over-filtering
        }
        
        // All caps content (often spam) - compute on ORIGINAL content, not lowercased
        if (originalContent.length > 10) {
            val uppercaseCount = originalContent.count { it.isUpperCase() }
            val capsRatio = if (originalContent.isNotEmpty()) uppercaseCount.toFloat() / originalContent.length else 0f
            if (capsRatio > 0.7f) score += 20
        }
        
        // Multiple exclamation marks
        val exclamationCount = originalContent.count { it == '!' }
        if (exclamationCount > 2) score += 15
        
        // Links (promotional often include links)
        if (contentLower.contains("http")) score += 15
        try {
            if (ClassificationConstants.SHORT_LINK_DOMAINS.any { contentLower.contains(it) }) score += 10
        } catch (e: Exception) {
            // If check fails, ignore it
        }
        
        return minOf(score, 100) // Cap at 100
    }
    
    /**
     * Get spam threshold based on user preferences
     * Higher threshold = more tolerant (less filtering)
     */
    private fun getSpamThreshold(spamTolerance: SpamTolerance, filteringMode: FilteringMode): Int {
        val baseThreshold = when (spamTolerance) {
            SpamTolerance.LOW -> 30      // Aggressive filtering
            SpamTolerance.MODERATE -> 50 // Balanced filtering  
            SpamTolerance.HIGH -> 70     // Lenient filtering
        }
        
        val modeAdjustment = when (filteringMode) {
            FilteringMode.STRICT -> -10   // Lower threshold (more filtering)
            FilteringMode.MODERATE -> 0   // No adjustment
            FilteringMode.LENIENT -> +15  // Higher threshold (less filtering)
        }
        
        return maxOf(baseThreshold + modeAdjustment, 10) // Minimum threshold of 10
    }
}
