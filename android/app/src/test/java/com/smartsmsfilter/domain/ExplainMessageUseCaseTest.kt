package com.smartsmsfilter.domain

import com.smartsmsfilter.classification.PrivateContextualClassifier
import com.smartsmsfilter.data.preferences.PreferencesSource
import com.smartsmsfilter.data.preferences.UserPreferences
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.repository.SmsRepository
import com.smartsmsfilter.domain.usecase.ExplainMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class ExplainMessageUseCaseTest {

    private class FakePrefs(private val prefs: UserPreferences) : PreferencesSource {
        override val userPreferences = MutableStateFlow(prefs)
    }

    private open class FakeRepo : SmsRepository {
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
        override suspend fun getSenderPreferences(sender: String): com.smartsmsfilter.domain.model.SenderPreferences? = null
        override suspend fun setSenderPinnedToInbox(sender: String, pinned: Boolean) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun updateSenderReputation(sender: String, importanceScore: Float?, spamScore: Float?) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun insertClassificationAudit(messageId: Long?, classifier: MessageCategory, category: MessageCategory, confidence: Float, reasons: List<String>) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun insertUserFeedbackAudit(messageId: Long, targetCategory: MessageCategory, reasons: List<String>) = com.smartsmsfilter.domain.common.Result.Success(Unit)
        override suspend fun getLatestClassificationReason(messageId: Long): String? = null
        override suspend fun getMessageById(messageId: Long): SmsMessage? = null
        override suspend fun restoreSoftDeletedMessages(messageIds: List<Long>) = com.smartsmsfilter.domain.common.Result.Success(Unit)
    }

    @Test
    fun fallbackIncludesPinnedSender() {
        val prefsSource = FakePrefs(UserPreferences())
        val contextual = PrivateContextualClassifier(prefsSource)
        val repo = object : FakeRepo() {
            override suspend fun getSenderPreferences(sender: String): com.smartsmsfilter.domain.model.SenderPreferences? {
                return com.smartsmsfilter.domain.model.SenderPreferences(sender = sender, pinnedToInbox = true)
            }
        }
        val useCase = ExplainMessageUseCase(contextual, repo)
        val msg = SmsMessage(id = 1, sender = "AMZN", content = "Your order shipped", timestamp = Date(), category = MessageCategory.NEEDS_REVIEW)
        val reasons = kotlinx.coroutines.runBlocking { useCase(msg) }
        assertTrue(reasons.any { it.contains("Pinned sender") })
    }

    @Test
    fun fallbackIncludesOtp() {
        val prefsSource = FakePrefs(UserPreferences())
        val contextual = PrivateContextualClassifier(prefsSource)
        val repo = FakeRepo()
        val useCase = ExplainMessageUseCase(contextual, repo)
        val msg = SmsMessage(id = 2, sender = "VM-OTP", content = "Use 123456 to verify your login", timestamp = Date(), category = MessageCategory.INBOX)
        val reasons = kotlinx.coroutines.runBlocking { useCase(msg) }
        assertTrue(reasons.any { it.contains("OTP detected") })
    }

    @Test
    fun fallbackIncludesAutoSpam() {
        val prefsSource = FakePrefs(UserPreferences())
        val contextual = PrivateContextualClassifier(prefsSource)
        val repo = object : FakeRepo() {
            override suspend fun getSenderPreferences(sender: String): com.smartsmsfilter.domain.model.SenderPreferences? {
                return com.smartsmsfilter.domain.model.SenderPreferences(sender = sender, autoSpam = true)
            }
        }
        val useCase = ExplainMessageUseCase(contextual, repo)
        val msg = SmsMessage(id = 3, sender = "VM-PROMO", content = "Exclusive offer just for you", timestamp = Date(), category = MessageCategory.SPAM)
        val reasons = kotlinx.coroutines.runBlocking { useCase(msg) }
        assertTrue(reasons.any { it.contains("Sender marked auto-spam") })
    }
}
