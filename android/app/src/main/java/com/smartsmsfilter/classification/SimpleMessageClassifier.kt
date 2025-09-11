package com.smartsmsfilter.classification

import com.smartsmsfilter.data.preferences.PreferencesManager
import com.smartsmsfilter.data.preferences.FilteringMode
import com.smartsmsfilter.data.preferences.ImportantMessageType
import com.smartsmsfilter.data.preferences.SpamTolerance
import com.smartsmsfilter.domain.model.MessageCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimpleMessageClassifier @Inject constructor(
    private val preferencesManager: PreferencesManager
) {

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
    fun classifyMessage(sender: String, content: String): MessageCategory {
        return runBlocking {
            val preferences = preferencesManager.userPreferences.first()
            classifyWithPreferences(sender, content, preferences)
        }
    }
    
    private fun classifyWithPreferences(
        sender: String, 
        content: String, 
        preferences: com.smartsmsfilter.data.preferences.UserPreferences
    ): MessageCategory {
        val contentLower = content.lowercase()
        val senderUpper = sender.uppercase()
        
        // Always check for trusted senders first (regardless of preferences)
        if (TRUSTED_SENDERS.any { senderUpper.contains(it) }) {
            return MessageCategory.INBOX
        }
        
        // Always block obvious spam senders (regardless of tolerance)
        if (SPAM_SENDERS.any { senderUpper.startsWith(it) }) {
            return MessageCategory.FILTERED
        }
        
        // Check user's selected important message types
        val userImportantTypes = preferences.importantMessageTypes
        
        // OTPs - check if user considers them important
        if (userImportantTypes.contains(ImportantMessageType.OTPS) && 
            containsAnyKeyword(contentLower, OTP_KEYWORDS)) {
            return MessageCategory.INBOX
        }
        
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
        
        // Personal messages from phone numbers
        if (userImportantTypes.contains(ImportantMessageType.PERSONAL) && 
            isPhoneNumber(sender)) {
            return MessageCategory.INBOX
        }
        
        // Apply spam filtering based on user's spam tolerance and filtering mode
        val spamScore = calculateSpamScore(contentLower, senderUpper)
        val threshold = getSpamThreshold(preferences.spamTolerance, preferences.filteringMode)
        
        if (spamScore >= threshold) {
            return MessageCategory.FILTERED
        }
        
        // If we reach here, the message is uncertain
        return MessageCategory.NEEDS_REVIEW
    }

    private fun containsAnyKeyword(text: String, keywords: Set<String>): Boolean {
        return keywords.any { keyword ->
            text.contains(keyword)
        }
    }

    private fun isPhoneNumber(sender: String): Boolean {
        // Simple check for phone number format
        return sender.replace(Regex("[+\\-\\s()]"), "").all { it.isDigit() } && 
               sender.replace(Regex("[+\\-\\s()]"), "").length >= 10
    }
    
    /**
     * Calculate a spam score for the message (0-100)
     * Higher score = more likely to be spam
     */
    private fun calculateSpamScore(contentLower: String, senderUpper: String): Int {
        var score = 0
        
        // Check spam keywords (each adds points)
        val spamKeywordCount = SPAM_KEYWORDS.count { contentLower.contains(it) }
        score += spamKeywordCount * 25 // Each spam keyword = 25 points
        
        // Check promotional keywords (lighter penalty)
        val promoKeywordCount = PROMOTIONAL_KEYWORDS.count { contentLower.contains(it) }
        score += promoKeywordCount * 15 // Each promo keyword = 15 points
        
        // Suspicious sender patterns
        if (senderUpper.startsWith("AD-") || senderUpper.startsWith("DM-")) {
            score += 30
        }
        
        // All caps content (often spam)
        if (contentLower != contentLower.lowercase() && contentLower.length > 10) {
            val capsRatio = contentLower.count { it.isUpperCase() }.toFloat() / contentLower.length
            if (capsRatio > 0.7f) score += 20
        }
        
        // Multiple exclamation marks
        val exclamationCount = contentLower.count { it == '!' }
        if (exclamationCount > 2) score += 15
        
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
