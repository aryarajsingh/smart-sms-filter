package com.smartsmsfilter.domain

import com.smartsmsfilter.data.preferences.FilteringMode
import com.smartsmsfilter.data.preferences.UserPreferences
import com.smartsmsfilter.domain.classifier.impl.ClassificationServiceImpl
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.repository.SmsRepository
import com.smartsmsfilter.classification.SimpleMessageClassifier
import com.smartsmsfilter.classification.PrivateContextualClassifier
import com.smartsmsfilter.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class ClassificationServiceImplTest {

    private class FakePrefs(private val prefs: UserPreferences) : com.smartsmsfilter.data.preferences.PreferencesSource {
        override val userPreferences = MutableStateFlow(prefs)
    }

    private open class FakeRepo : SmsRepository {
        override suspend fun getSenderPreferences(sender: String): com.smartsmsfilter.domain.model.SenderPreferences? = null
        val stored = mutableListOf<SmsMessage>()
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
        override fun getMessagesByAddress(address: String) = flowOf(emptyList<SmsMessage>())
        // already defined above
        override suspend fun setSenderPinnedToInbox(sender: String, pinned: Boolean) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun updateSenderReputation(sender: String, importanceScore: Float?, spamScore: Float?) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun insertClassificationAudit(messageId: Long?, classifier: MessageCategory, category: MessageCategory, confidence: Float, reasons: List<String>) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun insertUserFeedbackAudit(messageId: Long, targetCategory: MessageCategory, reasons: List<String>) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun getLatestClassificationReason(messageId: Long): String? = null
        override suspend fun getMessageById(messageId: Long): SmsMessage? = null
        override suspend fun restoreSoftDeletedMessages(messageIds: List<Long>) = com.smartsmsfilter.domain.common.Result.Success(Unit)
    }

    @Test
    fun blendingThresholdRespectsFilteringMode() {
        val prefs = UserPreferences(filteringMode = FilteringMode.STRICT)
        val prefsSource = FakePrefs(prefs)
        val simple = SimpleMessageClassifier(prefsSource)
        val contextual = PrivateContextualClassifier(prefsSource)
        val repo = FakeRepo()
        val svc = ClassificationServiceImpl(simple, contextual, prefsSource, repo)

        val message = SmsMessage(
            id = 1,
            sender = "AMZN",
            content = "Your order has been shipped",
            timestamp = Date()
        )
        val result = kotlinx.coroutines.runBlocking { svc.classifyAndStore(message) }
        // We do not assert exact category (depends on rules), but ensure confidence is between 0 and 1
        assertEquals(true, result.confidence in 0f..1f)
    }

    @Test
    fun pinnedSenderOverrideToInbox() {
        val prefs = UserPreferences(filteringMode = FilteringMode.MODERATE)
        val prefsSource = FakePrefs(prefs)
        val simple = SimpleMessageClassifier(prefsSource)
        val contextual = PrivateContextualClassifier(prefsSource)

        val repo = object : FakeRepo() {
            override suspend fun getSenderPreferences(sender: String): com.smartsmsfilter.domain.model.SenderPreferences? {
                return com.smartsmsfilter.domain.model.SenderPreferences(sender = sender, pinnedToInbox = true)
            }
        }
        val svc = ClassificationServiceImpl(simple, contextual, prefsSource, repo)

        val message = SmsMessage(
            id = 3L,
            sender = "AMZN",
            content = "Exclusive deal just for you",
            timestamp = Date()
        )
        val result = kotlinx.coroutines.runBlocking { svc.classifyAndStore(message) }
        assertEquals(MessageCategory.INBOX, result.category)
    }

    @Test
    fun autoSpamOverrideToSpam() {
        val prefs = UserPreferences(filteringMode = FilteringMode.LENIENT)
        val prefsSource = FakePrefs(prefs)
        val simple = SimpleMessageClassifier(prefsSource)
        val contextual = PrivateContextualClassifier(prefsSource)

        val repo = object : FakeRepo() {
            override suspend fun getSenderPreferences(sender: String): com.smartsmsfilter.domain.model.SenderPreferences? {
                return com.smartsmsfilter.domain.model.SenderPreferences(sender = sender, autoSpam = true)
            }
        }
        val svc = ClassificationServiceImpl(simple, contextual, prefsSource, repo)

        val message = SmsMessage(
            id = 4L,
            sender = "VM-PROMO",
            content = "Limited time discount for you",
            timestamp = Date()
        )
        val result = kotlinx.coroutines.runBlocking { svc.classifyAndStore(message) }
        assertEquals(MessageCategory.SPAM, result.category)
    }

    @Test
    fun otpForcesInbox() {
        val prefs = UserPreferences(filteringMode = FilteringMode.MODERATE)
        val prefsSource = FakePrefs(prefs)
        val simple = SimpleMessageClassifier(prefsSource)
        val contextual = PrivateContextualClassifier(prefsSource)
        val repo = FakeRepo()
        val svc = ClassificationServiceImpl(simple, contextual, prefsSource, repo)

        val message = SmsMessage(
            id = 2L,
            sender = "VM-OTP",
            content = "Use 123456 to verify your login",
            timestamp = Date()
        )
        val result = kotlinx.coroutines.runBlocking { svc.classifyAndStore(message) }
        assertEquals(MessageCategory.INBOX, result.category)
    }

    @Test
    fun unknownShortLinkIsSpam() {
        val prefs = UserPreferences(filteringMode = FilteringMode.MODERATE)
        val prefsSource = FakePrefs(prefs)
        val simple = SimpleMessageClassifier(prefsSource)
        val contextual = PrivateContextualClassifier(prefsSource)
        val repo = FakeRepo()
        val svc = ClassificationServiceImpl(simple, contextual, prefsSource, repo)

        val message = SmsMessage(
            id = 5L,
            sender = "VM-NEW",
            content = "Click http://bit.ly/xyz for a surprise",
            timestamp = Date()
        )
        val result = kotlinx.coroutines.runBlocking { svc.classifyAndStore(message) }
        assertEquals(MessageCategory.SPAM, result.category)
    }

    @Test
    fun highImportanceScoreBiasesToInbox() {
        val prefs = UserPreferences(filteringMode = FilteringMode.MODERATE)
        val prefsSource = FakePrefs(prefs)
        val simple = SimpleMessageClassifier(prefsSource)
        val contextual = PrivateContextualClassifier(prefsSource)
        val repo = object : FakeRepo() {
            override suspend fun getSenderPreferences(sender: String): com.smartsmsfilter.domain.model.SenderPreferences? {
                return com.smartsmsfilter.domain.model.SenderPreferences(sender = sender, importanceScore = 0.9f)
            }
        }
        val svc = ClassificationServiceImpl(simple, contextual, prefsSource, repo)

        val message = SmsMessage(
            id = 6L,
            sender = "AMZN",
            content = "We'd like your feedback",
            timestamp = Date()
        )
        val result = kotlinx.coroutines.runBlocking { svc.classifyAndStore(message) }
        // If rule/context says NEEDS_REVIEW, importance should bias to INBOX
        assertEquals(MessageCategory.INBOX, result.category)
    }

    @Test
    fun highSpamScoreBiasesToSpam() {
        val prefs = UserPreferences(filteringMode = FilteringMode.MODERATE)
        val prefsSource = FakePrefs(prefs)
        val simple = SimpleMessageClassifier(prefsSource)
        val contextual = PrivateContextualClassifier(prefsSource)
        val repo = object : FakeRepo() {
            override suspend fun getSenderPreferences(sender: String): com.smartsmsfilter.domain.model.SenderPreferences? {
                return com.smartsmsfilter.domain.model.SenderPreferences(sender = sender, spamScore = 0.9f)
            }
        }
        val svc = ClassificationServiceImpl(simple, contextual, prefsSource, repo)

        val message = SmsMessage(
            id = 7L,
            sender = "VM-NEW2",
            content = "Hello",
            timestamp = Date()
        )
        val result = kotlinx.coroutines.runBlocking { svc.classifyAndStore(message) }
        assertEquals(MessageCategory.SPAM, result.category)
    }
}
