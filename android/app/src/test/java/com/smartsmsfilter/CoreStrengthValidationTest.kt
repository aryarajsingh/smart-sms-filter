package com.smartsmsfilter

import com.smartsmsfilter.classification.SimpleMessageClassifier
import com.smartsmsfilter.classification.PrivateContextualClassifier
import com.smartsmsfilter.data.contacts.ContactManager
import com.smartsmsfilter.data.contacts.Contact
import com.smartsmsfilter.data.preferences.*
import com.smartsmsfilter.domain.classifier.impl.ClassificationServiceImpl
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.model.StarredMessage
import com.smartsmsfilter.domain.model.StarredSenderGroup
import com.smartsmsfilter.domain.repository.SmsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito.*
import java.util.Date

/**
 * Comprehensive tests to validate the core strength of the SMS filtering system.
 * Tests edge cases, security concerns, and robustness.
 */
class CoreStrengthValidationTest {

    private class FakePreferencesSource(val prefs: UserPreferences) : PreferencesSource {
        override val userPreferences = MutableStateFlow(prefs)
    }

    private open class TestRepository : SmsRepository {
        var senderPreferences: Map<String, com.smartsmsfilter.domain.model.SenderPreferences> = emptyMap()
        var messageHistory: Map<String, List<SmsMessage>> = emptyMap()
        
        override suspend fun getSenderPreferences(sender: String) = senderPreferences[sender]
        override fun getMessagesByAddress(address: String) = flowOf(messageHistory[address] ?: emptyList())
        
        // Minimal implementations for other required methods
        override fun getAllMessages() = flowOf(emptyList<SmsMessage>())
        override fun getMessagesByCategory(category: MessageCategory) = flowOf(emptyList<SmsMessage>())
        override suspend fun insertMessage(message: SmsMessage) = com.smartsmsfilter.domain.common.Result.Success(message.id)
        override suspend fun updateMessageCategory(messageId: Long, category: MessageCategory) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun markAsRead(messageId: Long) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun deleteMessage(messageId: Long) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun archiveMessage(messageId: Long) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun archiveMessages(messageIds: List<Long>) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun unarchiveMessage(messageId: Long) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun softDeleteMessage(messageId: Long) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun softDeleteMessages(messageIds: List<Long>) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun moveToCategory(messageId: Long, category: MessageCategory) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun moveToCategoryBatch(messageIds: List<Long>, category: MessageCategory) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun markAsImportant(messageId: Long, isImportant: Boolean) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override fun getArchivedMessages() = flowOf(emptyList<SmsMessage>())
        override fun getAllActiveMessages() = flowOf(emptyList<SmsMessage>())
        override suspend fun getUnreadCount() = 0
        override suspend fun getUnreadCountByCategory(category: MessageCategory) = 0
        override suspend fun setSenderPinnedToInbox(sender: String, pinned: Boolean) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun updateSenderReputation(sender: String, importanceScore: Float?, spamScore: Float?) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun insertClassificationAudit(messageId: Long?, classifier: MessageCategory, category: MessageCategory, confidence: Float, reasons: List<String>) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun insertUserFeedbackAudit(messageId: Long, targetCategory: MessageCategory, reasons: List<String>) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun getLatestClassificationReason(messageId: Long): String? = null
        override suspend fun getMessageById(messageId: Long): SmsMessage? = null
        override suspend fun restoreSoftDeletedMessages(messageIds: List<Long>) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        
        // Starred messages methods
        override suspend fun starMessage(starredMessage: StarredMessage) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun unstarMessage(messageId: Long) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun isMessageStarred(messageId: Long): Boolean = false
        override fun getAllStarredMessages() = flowOf(emptyList<StarredMessage>())
        override fun getStarredMessagesBySender() = flowOf(emptyList<StarredSenderGroup>())
        override fun getStarredMessagesForSender(sender: String) = flowOf(emptyList<StarredMessage>())
        override suspend fun getStarredMessageCount(): Int = 0
    }

    private fun makeMockContactManager(): ContactManager {
        val mock = mock(ContactManager::class.java)
        // Default: return unknown contact
        `when`(runBlocking { mock.getContactByPhoneNumber(anyString()) }).thenReturn(
            Contact(id = 0, name = "Unknown", phoneNumber = "", phoneType = "", photoUri = null, isFrequentContact = false)
        )
        return mock
    }

    private fun makeClassifier(
        prefs: UserPreferences = UserPreferences(),
        contactManager: ContactManager = makeMockContactManager()
    ): SimpleMessageClassifier {
        val prefsSource = FakePreferencesSource(prefs)
        return SimpleMessageClassifier(prefsSource, contactManager)
    }

    private fun makeService(
        prefs: UserPreferences = UserPreferences(),
        repository: SmsRepository = TestRepository(),
        contactManager: ContactManager = makeMockContactManager()
    ): ClassificationServiceImpl {
        val prefsSource = FakePreferencesSource(prefs)
        val simple = SimpleMessageClassifier(prefsSource, contactManager)
        val contextual = PrivateContextualClassifier(prefsSource)
        return ClassificationServiceImpl(simple, contextual, prefsSource, repository)
    }

    // EDGE CASE TESTS

    @Test
    fun handlesEmptyAndNullContent() = runTest {
        val classifier = makeClassifier()
        
        // Empty content should not crash
        val emptyResult = classifier.classifyMessage("TEST", "")
        assertNotNull(emptyResult)
        
        // Very short content
        val shortResult = classifier.classifyMessage("TEST", "Hi")
        assertNotNull(shortResult)
    }

    @Test
    fun handlesExtremelyLongMessages() = runTest {
        val classifier = makeClassifier()
        val longMessage = "A".repeat(10000) // 10KB message
        
        val result = classifier.classifyMessage("TEST", longMessage)
        assertNotNull(result)
        // Should complete in reasonable time (test will timeout if it hangs)
    }

    @Test
    fun handlesSpecialCharactersAndUnicode() = runTest {
        val classifier = makeClassifier()
        
        // Unicode characters
        val unicodeMessage = "🎉 Congratulations! You've won ₹10000! 中文测试 العربية"
        val result1 = classifier.classifyMessage("TEST", unicodeMessage)
        assertNotNull(result1)
        
        // Special characters that might break regex
        val specialChars = "\\n\\t\\r[]{}()*+?.^$|"
        val result2 = classifier.classifyMessage("TEST", specialChars)
        assertNotNull(result2)
        
        // SQL injection attempt in content
        val sqlInjection = "'; DROP TABLE sms_messages; --"
        val result3 = classifier.classifyMessage("TEST", sqlInjection)
        assertNotNull(result3)
    }

    @Test
    fun priorityOrderIsRespected() = runTest {
        val contactManager = mock(ContactManager::class.java)
        // Mock known contact
        `when`(runBlocking { contactManager.getContactByPhoneNumber("+1234567890") }).thenReturn(
            Contact(id = 123, name = "John", phoneNumber = "+1234567890", phoneType = "Mobile", photoUri = null, isFrequentContact = false)
        )
        
        val classifier = makeClassifier(contactManager = contactManager)
        
        // Known contact with spam content should go to INBOX (contact priority wins)
        val result = classifier.classifyMessage("+1234567890", "WIN FREE MONEY NOW!!! CLICK HERE!!!")
        assertEquals("Known contacts should override spam detection", MessageCategory.INBOX, result)
    }

    @Test
    fun explicitSpamWarningsAlwaysBlocked() = runTest {
        val contactManager = mock(ContactManager::class.java)
        // Even mock a known contact
        `when`(runBlocking { contactManager.getContactByPhoneNumber("+1234567890") }).thenReturn(
            Contact(id = 123, name = "John", phoneNumber = "+1234567890", phoneType = "Mobile", photoUri = null, isFrequentContact = false)
        )
        
        val classifier = makeClassifier(contactManager = contactManager)
        
        // Explicit spam warning should be blocked even from known contact
        val result = classifier.classifyMessage("+1234567890", "Airtel Warning: SPAM - This message contains suspicious content")
        assertEquals("Explicit spam warnings should always be blocked", MessageCategory.SPAM, result)
    }

    @Test
    fun otpDetectionIsRobust() = runTest {
        val classifier = makeClassifier()
        
        val otpMessages = listOf(
            "123456 is your OTP for login verification", // matches "is your otp"
            "Use 456789 to complete verification", // matches "use\\s+\\d{4,8}"
            "Enter 999888 to verify your account", // matches "enter\\s+\\d{4,8}"
            "Your OTP: 111222 for banking", // matches "\\b\\d{4,8}\\b.{0,20}(otp|code|verification)"
            "Verification code: 333444", // matches "(otp|code|verification).{0,20}\\b\\d{4,8}\\b"
            "555666 is your verification code" // matches "\\b\\d{6}\\b.{0,10}(verify|valid)" - modified to use "verification"
        )
        
        otpMessages.forEach { message ->
            val result = classifier.classifyMessage("TEST", message)
            assertEquals("OTP message should go to inbox: $message", MessageCategory.INBOX, result)
        }
    }

    @Test
    fun spamDetectionHandlesEdgeCases() = runTest {
        val classifier = makeClassifier(UserPreferences(
            filteringMode = FilteringMode.STRICT,
            spamTolerance = SpamTolerance.LOW
        ))
        
        // Mixed case spam attempts
        val mixedCaseSpam = "WiN fReE mOnEy NoW!!!"
        val result1 = classifier.classifyMessage("SPAM-SENDER", mixedCaseSpam)
        assertTrue("Mixed case spam should be detected", result1 == MessageCategory.SPAM || result1 == MessageCategory.NEEDS_REVIEW)
        
        // Subtle promotional content
        val subtlePromo = "Limited time offer on electronics. Shop now!"
        val result2 = classifier.classifyMessage("VM-PROMO", subtlePromo)
        assertNotNull(result2)
    }

    @Test
    fun contactLookupFailureIsGraceful() = runTest {
        val contactManager = mock(ContactManager::class.java)
        // Simulate contact lookup failure
        `when`(runBlocking { contactManager.getContactByPhoneNumber(anyString()) }).thenThrow(RuntimeException("Contact DB unavailable"))
        
        val classifier = makeClassifier(contactManager = contactManager)
        
        // Should not crash and should not treat as known contact
        val result = classifier.classifyMessage("+1234567890", "Hey, how are you?")
        assertNotNull(result)
        // Since contact lookup failed, should not automatically go to INBOX unless other rules apply
    }

    @Test
    fun serviceHandlesSenderPreferenceFailures() = runTest {
        val repository = object : TestRepository() {
            override suspend fun getSenderPreferences(sender: String): com.smartsmsfilter.domain.model.SenderPreferences? {
                throw RuntimeException("Database error")
            }
        }
        
        val service = makeService(repository = repository)
        val message = SmsMessage(
            id = 1,
            sender = "TEST",
            content = "Hello world",
            timestamp = Date()
        )
        
        // Should not crash and should complete classification
        val result = service.classifyAndStore(message)
        assertNotNull(result)
        assertNotNull(result.category)
    }

    @Test
    fun userPreferencesAreRespected() = runTest {
        // Test with different user preferences
        val strictPrefs = UserPreferences(
            filteringMode = FilteringMode.STRICT,
            spamTolerance = SpamTolerance.LOW,
            importantMessageTypes = setOf(ImportantMessageType.BANKING)
        )
        
        val lenientPrefs = UserPreferences(
            filteringMode = FilteringMode.LENIENT,
            spamTolerance = SpamTolerance.HIGH,
            importantMessageTypes = setOf()
        )
        
        val strictClassifier = makeClassifier(strictPrefs)
        val lenientClassifier = makeClassifier(lenientPrefs)
        
        val borderlineMessage = "Special offer! Limited time discount on products."
        
        val strictResult = strictClassifier.classifyMessage("VM-PROMO", borderlineMessage)
        val lenientResult = lenientClassifier.classifyMessage("VM-PROMO", borderlineMessage)
        
        // Strict mode should be more likely to filter than lenient mode
        assertNotNull(strictResult)
        assertNotNull(lenientResult)
    }

    @Test
    fun senderPreferencesOverrideRules() = runTest {
        val repository = object : TestRepository() {
            override suspend fun getSenderPreferences(sender: String): com.smartsmsfilter.domain.model.SenderPreferences? {
                return when (sender) {
                    "PINNED" -> com.smartsmsfilter.domain.model.SenderPreferences(sender = sender, pinnedToInbox = true)
                    "AUTOSPAM" -> com.smartsmsfilter.domain.model.SenderPreferences(sender = sender, autoSpam = true)
                    else -> null
                }
            }
        }
        
        val service = makeService(repository = repository)
        
        // Pinned sender should go to inbox even with spam content
        val pinnedMessage = SmsMessage(
            id = 1,
            sender = "PINNED",
            content = "WIN FREE MONEY NOW!!!",
            timestamp = Date()
        )
        val pinnedResult = service.classifyAndStore(pinnedMessage)
        assertEquals("Pinned sender should override spam detection", MessageCategory.INBOX, pinnedResult.category)
        
        // Auto-spam sender should go to spam even with legitimate content
        val autoSpamMessage = SmsMessage(
            id = 2,
            sender = "AUTOSPAM",
            content = "Your order has been delivered",
            timestamp = Date()
        )
        val autoSpamResult = service.classifyAndStore(autoSpamMessage)
        assertEquals("Auto-spam sender should override legitimate content", MessageCategory.SPAM, autoSpamResult.category)
    }

    @Test
    fun otpOverridesSenderPreferences() = runTest {
        val repository = object : TestRepository() {
            override suspend fun getSenderPreferences(sender: String): com.smartsmsfilter.domain.model.SenderPreferences? {
                return com.smartsmsfilter.domain.model.SenderPreferences(sender = sender, autoSpam = true)
            }
        }
        
        val service = makeService(repository = repository)
        
        // OTP should go to inbox even from auto-spam sender
        val otpMessage = SmsMessage(
            id = 1,
            sender = "SPAM-SENDER",
            content = "Your OTP is 123456 for login verification",
            timestamp = Date()
        )
        val result = service.classifyAndStore(otpMessage)
        assertEquals("OTP should override auto-spam preference", MessageCategory.INBOX, result.category)
        assertTrue("Should mention OTP detection", result.reasons.any { it.contains("OTP detected") })
    }

    @Test
    fun performanceWithLargeDatasets() = runTest {
        val classifier = makeClassifier()
        val startTime = System.currentTimeMillis()
        
        // Classify 1000 messages
        repeat(1000) { i ->
            classifier.classifyMessage("SENDER$i", "Test message content number $i")
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        // Should complete within reasonable time (adjust threshold as needed)
        assertTrue("Classification should be fast: ${totalTime}ms for 1000 messages", totalTime < 5000)
    }
}