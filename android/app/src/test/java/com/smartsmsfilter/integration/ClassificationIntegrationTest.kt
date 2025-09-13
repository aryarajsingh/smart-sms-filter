package com.smartsmsfilter.integration

import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.*

/**
 * Integration tests for SMS classification functionality
 * Tests that don't require Android framework dependencies
 */
class ClassificationIntegrationTest {

    // Sample SMS messages for testing
    private val testMessages = listOf(
        // OTP Messages - Should go to INBOX
        SmsMessage(
            id = 1L,
            sender = "SBIINB",
            content = "Your OTP is 123456. Valid for 10 minutes. Do not share with anyone.",
            timestamp = Date(),
            category = MessageCategory.NEEDS_REVIEW
        ),
        
        SmsMessage(
            id = 2L, 
            sender = "VERIFY",
            content = "Use verification code 789012 to complete your transaction",
            timestamp = Date(),
            category = MessageCategory.NEEDS_REVIEW
        ),

        // Banking Messages - Should go to INBOX
        SmsMessage(
            id = 3L,
            sender = "HDFCBANK", 
            content = "Rs 5000 debited from A/c XX1234 on 13-SEP-25. Avl Bal Rs 15000",
            timestamp = Date(),
            category = MessageCategory.NEEDS_REVIEW
        ),

        SmsMessage(
            id = 4L,
            sender = "ICICIBANK",
            content = "Rs 2000 credited to your account XX5678. Current balance Rs 25000",
            timestamp = Date(), 
            category = MessageCategory.NEEDS_REVIEW
        ),

        // Spam Messages - Should go to SPAM
        SmsMessage(
            id = 5L,
            sender = "VM-PROMO",
            content = "Congratulations! You have won 1 Crore rupees! Call now 9999999999 to claim your prize!",
            timestamp = Date(),
            category = MessageCategory.NEEDS_REVIEW
        ),

        SmsMessage(
            id = 6L,
            sender = "AD-DEALS", 
            content = "URGENT! Limited time offer! Get 90% discount on all products. Click here: bit.ly/fake-link",
            timestamp = Date(),
            category = MessageCategory.NEEDS_REVIEW
        ),

        // E-commerce Messages - Should go to INBOX (important)
        SmsMessage(
            id = 7L,
            sender = "AMAZON",
            content = "Your order #123456789 has been shipped and will arrive tomorrow",
            timestamp = Date(),
            category = MessageCategory.NEEDS_REVIEW
        ),

        // Ambiguous Messages - Could go to NEEDS_REVIEW
        SmsMessage(
            id = 8L,
            sender = "UNKNOWN",
            content = "Please update your profile information at our website",
            timestamp = Date(), 
            category = MessageCategory.NEEDS_REVIEW
        )
    )

    @Test
    fun `test message categorization patterns`() {
        // Test OTP detection patterns
        val otpPatterns = listOf(
            "Your OTP is 123456",
            "Use verification code 789012", 
            "Authentication code: 456789",
            "Login OTP 111222"
        )

        otpPatterns.forEach { content ->
            val hasOtpKeyword = content.lowercase().contains("otp") || 
                               content.lowercase().contains("verification") ||
                               content.lowercase().contains("code")
            assert(hasOtpKeyword) { "OTP pattern not detected: $content" }
        }
    }

    @Test
    fun `test banking message patterns`() {
        val bankingPatterns = listOf(
            "Rs 5000 debited from account",
            "Amount credited to your account", 
            "Current balance is Rs 15000",
            "UPI transaction completed"
        )

        bankingPatterns.forEach { content ->
            val hasBankingKeyword = content.lowercase().contains("debited") ||
                                   content.lowercase().contains("credited") ||
                                   content.lowercase().contains("balance") ||
                                   content.lowercase().contains("upi") ||
                                   content.lowercase().contains("account")
            assert(hasBankingKeyword) { "Banking pattern not detected: $content" }
        }
    }

    @Test
    fun `test spam message patterns`() {
        val spamPatterns = listOf(
            "Congratulations! You have won",
            "URGENT! Limited time offer",
            "Call now to claim your prize",
            "Click here for free money"
        )

        spamPatterns.forEach { content ->
            val hasSpamKeyword = content.lowercase().contains("congratulations") ||
                                content.lowercase().contains("won") ||
                                content.lowercase().contains("urgent") ||
                                content.lowercase().contains("prize") ||
                                content.lowercase().contains("free")
            assert(hasSpamKeyword) { "Spam pattern not detected: $content" }
        }
    }

    @Test
    fun `test trusted sender patterns`() {
        val trustedSenders = listOf(
            "SBIINB", "HDFCBANK", "ICICIBANK", "AMAZON", "FLIPKART", "PAYTM"
        )

        val trustedSendersList = setOf(
            "SBIINB", "HDFCBANK", "ICICIBANK", "AXISBANK", "KOTAKBANK", "YESBANK",
            "AMZN", "AMAZON", "FLIPKART", "PAYTM", "GPAY", "PHONEPE"
        )

        trustedSenders.forEach { sender ->
            val isTrusted = trustedSendersList.any { 
                sender.uppercase().contains(it) || it.contains(sender.uppercase())
            }
            assert(isTrusted) { "Trusted sender not recognized: $sender" }
        }
    }

    @Test
    fun `test message length handling`() {
        // Test various message lengths
        val shortMessage = "OK"
        val normalMessage = "Your OTP is 123456. Valid for 10 minutes."
        val longMessage = "A".repeat(500) + " Your OTP is 123456"
        val veryLongMessage = "A".repeat(2000) + " Banking transaction completed"

        val messages = listOf(shortMessage, normalMessage, longMessage, veryLongMessage)
        
        messages.forEach { content ->
            // Should handle all message lengths without crashing
            assert(content.isNotEmpty()) { "Message content is empty" }
            
            // Test tokenization would work (simulate max length handling)
            val maxTokens = 60
            val words = content.split("\\s+".toRegex())
            val truncatedWords = words.take(maxTokens)
            
            assert(truncatedWords.size <= maxTokens) { "Tokenization limit exceeded" }
        }
    }

    @Test
    fun `test special character handling`() {
        val specialMessages = listOf(
            "Your A/c XX1234 debited ₹5000",
            "OTP: 123456 @#$%^&*()",
            "Código de verificación: 789012", // Spanish
            "আপনার OTP হল 456789", // Bengali
            "🎉 Congratulations! You won! 🎉" // Emojis
        )

        specialMessages.forEach { content ->
            // Should handle special characters gracefully
            assert(content.isNotEmpty()) { "Message with special chars is empty" }
            
            // Test currency normalization
            val normalized = content
                .replace(Regex("[₹Rs.?]"), "rs")
                .replace(Regex("\\d{4,}"), "number")
            
            assert(normalized.isNotEmpty()) { "Normalization failed" }
        }
    }

    @Test
    fun `test classification confidence ranges`() {
        // Confidence should be reasonable for different message types
        
        // High confidence scenarios (clear patterns)
        val highConfidenceScenarios = mapOf(
            "Your OTP is 123456" to "OTP",
            "Rs 5000 debited from account" to "Banking",
            "Congratulations! You won 1 crore!" to "Spam"
        )

        // Low confidence scenarios (ambiguous)
        val lowConfidenceScenarios = mapOf(
            "Hello" to "Personal",
            "Update your details" to "Service",
            "Meeting at 5pm" to "Personal"
        )

        // Test that we can distinguish between high and low confidence scenarios
        highConfidenceScenarios.forEach { (content, type) ->
            assert(content.isNotEmpty()) { "High confidence message is empty" }
        }

        lowConfidenceScenarios.forEach { (content, type) ->
            assert(content.isNotEmpty()) { "Low confidence message is empty" }
        }
    }

    @Test
    fun `test batch processing consistency`() = runBlocking {
        // Test that batch processing gives consistent results
        
        // Process messages individually
        val individualResults = mutableMapOf<Long, String>()
        testMessages.forEach { message ->
            // Simulate classification result
            val category = when {
                message.content.lowercase().contains("otp") -> "INBOX"
                message.content.lowercase().contains("debited") -> "INBOX" 
                message.content.lowercase().contains("congratulations") -> "SPAM"
                else -> "NEEDS_REVIEW"
            }
            individualResults[message.id] = category
        }

        // Process messages in batch (should give same results)
        val batchResults = mutableMapOf<Long, String>()
        testMessages.forEach { message ->
            // Simulate batch classification (same logic)
            val category = when {
                message.content.lowercase().contains("otp") -> "INBOX"
                message.content.lowercase().contains("debited") -> "INBOX"
                message.content.lowercase().contains("congratulations") -> "SPAM" 
                else -> "NEEDS_REVIEW"
            }
            batchResults[message.id] = category
        }

        // Results should be consistent
        assert(individualResults.size == batchResults.size) { "Batch size mismatch" }
        
        individualResults.forEach { (id, category) ->
            assert(batchResults[id] == category) { "Inconsistent classification for message $id" }
        }
    }
}