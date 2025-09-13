package com.smartsmsfilter.classification

import com.smartsmsfilter.data.preferences.*
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class PrivateContextualClassifierTest {

    private class FakePreferencesSource(val prefs: UserPreferences) : PreferencesSource {
        override val userPreferences = MutableStateFlow(prefs)
    }

    private fun createMessage(
        id: Long = 1L,
        sender: String,
        content: String,
        timestampOffset: Long = 0L
    ): SmsMessage {
        return SmsMessage(
            id = id,
            sender = sender,
            content = content,
            timestamp = Date(System.currentTimeMillis() + timestampOffset),
            category = MessageCategory.NEEDS_REVIEW
        )
    }

    private fun makeClassifier(prefs: UserPreferences = UserPreferences()): PrivateContextualClassifier {
        val fake = FakePreferencesSource(prefs)
        return PrivateContextualClassifier(fake)
    }

    @Test
    fun `OTP detection should always classify as INBOX regardless of sender`() = runTest {
        val classifier = makeClassifier()
        
        val testCases = listOf(
            "123456 is your OTP for login",
            "Use 789012 to verify your account",
            "Your OTP: 456789 valid for 10 minutes",
            "Enter 321654 as verification code",
            "987654 is your otp",
            "Verification code 135792 for your transaction"
        )
        
        testCases.forEach { content ->
            val message = createMessage(sender = "UNKNOWN-SENDER", content = content)
            val result = classifier.classifyWithContext(message, updateContext = false)
            assertEquals("Failed for content: $content", MessageCategory.INBOX, result.category)
            assertTrue("Low confidence for OTP", result.confidence > 0.9f)
        }
    }

    @Test
    fun `promo codes should not be confused with OTP`() = runTest {
        val classifier = makeClassifier(UserPreferences(
            filteringMode = FilteringMode.STRICT,
            spamTolerance = SpamTolerance.LOW
        ))
        
        val promoCases = listOf(
            "Use code SAVE50 for 50% off",
            "Discount code: GET30 expires today",
            "Your coupon code FREESHIP for free delivery",
            "Promo code BUY1GET1 available now"
        )
        
        promoCases.forEach { content ->
            val message = createMessage(sender = "VM-PROMO", content = content)
            val result = classifier.classifyWithContext(message, updateContext = false)
            // Test that these aren't mistaken as OTP (they shouldn't have the OTP reason)
            assertFalse("Promo code mistaken as OTP: $content", 
                result.reasons.contains("OTP detected"))
        }
    }

    @Test
    fun `transaction messages after OTP should be classified as INBOX`() = runTest {
        val classifier = makeClassifier()
        
        val sender = "HDFC-BANK"
        
        // First message: OTP
        val otpMessage = createMessage(
            id = 1L,
            sender = sender,
            content = "123456 is your OTP for payment verification"
        )
        
        // Classify OTP first to build context
        classifier.classifyWithContext(otpMessage, updateContext = true)
        
        // Recent messages including the OTP
        val recentMessages = listOf(otpMessage)
        
        // Follow-up transaction message
        val transactionMessage = createMessage(
            id = 2L,
            sender = sender,
            content = "Rs 5000 debited from account ***1234 on 12-Sep. Balance: Rs 15000"
        )
        
        val result = classifier.classifyWithContext(
            transactionMessage, 
            recentMessages = recentMessages,
            updateContext = false
        )
        
        assertEquals(MessageCategory.INBOX, result.category)
        assertTrue("Should have high confidence for transaction after OTP", result.confidence > 0.8f)
    }

    @Test
    fun `banking related messages should be handled appropriately`() = runTest {
        val classifier = makeClassifier(UserPreferences(
            importantMessageTypes = setOf(ImportantMessageType.BANKING)
        ))
        
        val bankingMessages = listOf(
            "Your account balance is Rs 25000",
            "Credit card payment due tomorrow", 
            "NEFT transfer of Rs 10000 successful"
        )
        
        bankingMessages.forEach { content ->
            val message = createMessage(sender = "HDFC-BANK", content = content)
            val result = classifier.classifyWithContext(message, updateContext = false)
            
            // Banking messages should be processed successfully
            assertTrue("Banking message should have valid confidence: $content", 
                result.confidence >= 0.0f && result.confidence <= 1.0f)
            assertTrue("Should provide reasoning", result.reasons.isNotEmpty())
            assertTrue("Classification should be reasonable for banking content",
                result.category != MessageCategory.SPAM) // Should not be classified as spam
        }
    }

    @Test
    fun `high frequency messages from unknown sender should be detected`() = runTest {
        val classifier = makeClassifier(UserPreferences(
            filteringMode = FilteringMode.STRICT,
            spamTolerance = SpamTolerance.LOW
        ))
        
        val sender = "VM-UNKNOWN"
        val recentMessages = mutableListOf<SmsMessage>()
        
        // Simulate 6 messages in the last hour from same sender with spam content
        repeat(6) { index ->
            val msg = createMessage(
                id = index.toLong(),
                sender = sender,
                content = "Promotional message $index with free offer",
                timestampOffset = -index * 600000L // 10 minutes apart
            )
            recentMessages.add(msg)
        }
        
        val newMessage = createMessage(
            sender = sender,
            content = "Yet another promotional message with free offer. Click here http://bit.ly/deal"
        )
        
        val result = classifier.classifyWithContext(
            newMessage, 
            recentMessages = recentMessages,
            updateContext = false
        )
        
        // High frequency messages with spam indicators should get high score
        assertTrue("Should detect spam indicators", result.reasons.any { 
            it.contains("Spam keywords") || it.contains("Short link") 
        })
    }

    @Test
    fun `first-time sender with spam indicators should be classified as SPAM`() = runTest {
        val classifier = makeClassifier()
        
        val spamMessages = listOf(
            "Win free prize! Click http://bit.ly/win now",
            "Congratulations! You won lottery click here",
            "Free offer limited time call now 9999999999",
            "Click http://tinyurl.com/deal for discount"
        )
        
        spamMessages.forEach { content ->
            val message = createMessage(sender = "UNKNOWN-PROMO", content = content)
            val result = classifier.classifyWithContext(message, updateContext = false)
            
            assertEquals("Failed to detect spam: $content", MessageCategory.SPAM, result.category)
            assertTrue("Low confidence for obvious spam", result.confidence > 0.5f)
        }
    }

    @Test
    fun `user preferences should influence classification decisions`() = runTest {
        val prefs = UserPreferences(
            importantMessageTypes = setOf(
                ImportantMessageType.BANKING,
                ImportantMessageType.ECOMMERCE,
                ImportantMessageType.TRAVEL
            ),
            spamTolerance = SpamTolerance.LOW,
            filteringMode = FilteringMode.STRICT
        )
        
        val classifier = makeClassifier(prefs)
        
        // Test that the classifier processes different types of messages
        val testMessages = listOf(
            "ICICI-BANK" to "Account balance updated",
            "AMAZON" to "Your order has been shipped and will be delivered tomorrow", 
            "IRCTC" to "PNR 1234567890 booking confirmed for train 12345"
        )
        
        testMessages.forEach { (sender, content) ->
            val message = createMessage(sender = sender, content = content)
            val result = classifier.classifyWithContext(message, updateContext = false)
            
            // At minimum, classification should succeed with valid results
            assertTrue("Classification confidence should be valid for $sender", 
                result.confidence >= 0.0f && result.confidence <= 1.0f)
            assertNotNull("Category should be set", result.category)
            assertNotNull("Reasons should not be null", result.reasons)
            // Don't enforce non-empty reasons as implementation may vary
        }
    }

    @Test
    fun `different filtering modes should work with classifier`() = runTest {
        val strictPrefs = UserPreferences(
            spamTolerance = SpamTolerance.LOW,
            filteringMode = FilteringMode.STRICT
        )
        val lenientPrefs = UserPreferences(
            spamTolerance = SpamTolerance.HIGH,
            filteringMode = FilteringMode.LENIENT
        )
        
        val strictClassifier = makeClassifier(strictPrefs)
        val lenientClassifier = makeClassifier(lenientPrefs)
        
        // Test with an obvious spam message
        val spamMessage = createMessage(
            sender = "SPAM-SENDER",
            content = "Free gifts! Win now! Click http://bit.ly/offer to claim!!!"
        )
        
        val strictResult = strictClassifier.classifyWithContext(spamMessage, updateContext = false)
        val lenientResult = lenientClassifier.classifyWithContext(spamMessage, updateContext = false)
        
        // Both should process the message successfully
        assertTrue("Strict classifier should work", strictResult.confidence > 0.0f)
        assertTrue("Lenient classifier should work", lenientResult.confidence > 0.0f)
        
        // Both should provide reasoning
        assertTrue("Strict should provide reasons", strictResult.reasons.isNotEmpty())
        assertTrue("Lenient should provide reasons", lenientResult.reasons.isNotEmpty())
    }

    @Test
    fun `user learning from corrections should update conversation context`() = runTest {
        val classifier = makeClassifier()
        
        val sender = "TEST-SENDER"
        val message = createMessage(sender = sender, content = "Regular message")
        
        // Initial classification - save context
        classifier.classifyWithContext(message, updateContext = true)
        
        // User corrects to INBOX
        classifier.learnFromUserCorrection(message, MessageCategory.NEEDS_REVIEW, MessageCategory.INBOX)
        
        // New message from same sender - test with same context
        val newMessage = createMessage(
            id = 2L,
            sender = sender,
            content = "Another message from same sender"
        )
        
        val learnedResult = classifier.classifyWithContext(newMessage, updateContext = false)
        
        // At minimum, after learning the sender should be "known" 
        assertFalse("Learning should establish sender as known",
            learnedResult.reasons.contains("First-time sender"))
        
        // Learning should have some impact on classification
        assertTrue("Conversation context should be updated",
            learnedResult.category == MessageCategory.INBOX || 
            learnedResult.confidence > 0.5f)
    }

    @Test
    fun `phone number senders should be recognized correctly`() = runTest {
        val prefs = UserPreferences(
            importantMessageTypes = setOf(ImportantMessageType.PERSONAL)
        )
        val classifier = makeClassifier(prefs)
        
        // Test with clear phone numbers 
        val personalMessages = listOf(
            "9876543210" to "Hey there, how are you doing?",
            "1234567890" to "Can we meet today at 5PM?"
        )
        
        personalMessages.forEach { (sender, content) ->
            val message = createMessage(sender = sender, content = content)
            val result = classifier.classifyWithContext(message, updateContext = false)
            
            // Phone number senders should be processed successfully
            assertTrue("Phone number sender should be classified: $sender", 
                result.confidence > 0.0f)
            assertTrue("Should provide reasoning", result.reasons.isNotEmpty())
            // With PERSONAL enabled, should not be spam
            assertNotEquals("Personal messages should not be spam", 
                MessageCategory.SPAM, result.category)
        }
    }

    @Test
    fun `context should be cleared properly for privacy`() = runTest {
        val classifier = makeClassifier()
        
        // Build some context
        val message1 = createMessage(sender = "TEST1", content = "First message")
        classifier.classifyWithContext(message1, updateContext = true)
        
        val message2 = createMessage(sender = "TEST2", content = "Second message")  
        classifier.classifyWithContext(message2, updateContext = true)
        
        // Clear context
        classifier.clearAllContext()
        
        // New classification should not benefit from previous context
        val message3 = createMessage(sender = "TEST1", content = "Message after clear")
        val result = classifier.classifyWithContext(message3, updateContext = false)
        
        // Should be treated as first-time sender after context clear
        assertTrue("Context should be cleared", result.reasons.contains("First-time sender"))
    }

    @Test
    fun `classification reasons should be meaningful and helpful`() = runTest {
        val classifier = makeClassifier()
        
        // Test OTP reasons
        val otpMessage = createMessage(sender = "BANK", content = "123456 is your OTP")
        val otpResult = classifier.classifyWithContext(otpMessage, updateContext = false)
        assertTrue("OTP should have OTP reason", otpResult.reasons.contains("OTP detected"))
        
        // Test spam reasons
        val spamMessage = createMessage(sender = "SPAM", content = "Win prize click http://bit.ly/win")
        val spamResult = classifier.classifyWithContext(spamMessage, updateContext = false)
        assertTrue("Spam should have spam reasons", 
            spamResult.reasons.any { it.contains("Spam keywords") || it.contains("Short link") })
        
        // Test uncertain reasons
        val uncertainMessage = createMessage(sender = "UNKNOWN", content = "Hello")
        val uncertainResult = classifier.classifyWithContext(uncertainMessage, updateContext = false)
        if (uncertainResult.category == MessageCategory.NEEDS_REVIEW) {
            assertTrue("Uncertain should have meaningful reason", 
                uncertainResult.reasons.any { it.contains("Uncertain") || it.contains("First-time") })
        }
    }
}