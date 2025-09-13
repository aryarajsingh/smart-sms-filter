package com.smartsmsfilter.services

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.smartsmsfilter.data.database.dao.SmsMessageDao
import com.smartsmsfilter.data.database.entities.SmsMessageEntity
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.services.ClassificationService
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SmsReaderTest {

    private lateinit var smsReader: SmsReader
    private lateinit var context: Context
    private lateinit var smsMessageDao: SmsMessageDao
    private lateinit var classificationService: ClassificationService
    private lateinit var contentResolver: ContentResolver
    private lateinit var cursor: Cursor

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        context = mockk(relaxed = true)
        smsMessageDao = mockk(relaxed = true)
        classificationService = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        cursor = mockk(relaxed = true)
        
        every { context.contentResolver } returns contentResolver
        
        smsReader = SmsReader(context, smsMessageDao, classificationService)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test readAllMessages preserves existing classifications`() = runTest {
        // Given - existing classified messages in database
        val existingMessages = listOf(
            SmsMessageEntity(
                id = 1001,
                sender = "+1234567890",
                content = "Already classified as spam",
                timestamp = Date(System.currentTimeMillis() - 86400000),
                category = MessageCategory.SPAM,
                isRead = true,
                threadId = "thread1",
                isOutgoing = false,
                mlConfidence = 0.95f,
                userOverride = false,
                classificationReason = "ML classified as spam"
            ),
            SmsMessageEntity(
                id = 1002,
                sender = "BANK",
                content = "Your OTP is 123456",
                timestamp = Date(System.currentTimeMillis() - 3600000),
                category = MessageCategory.INBOX,
                isRead = false,
                threadId = "thread2",
                isOutgoing = false,
                mlConfidence = 0.88f,
                userOverride = false,
                classificationReason = "OTP message"
            )
        )
        
        coEvery { smsMessageDao.getAllMessages() } returns flowOf(existingMessages)
        
        // Mock system SMS provider with same messages
        setupCursorForMessages(existingMessages)
        
        // When
        val messages = smsReader.readAllMessages()
        
        // Then - should not reclassify existing messages
        coVerify(exactly = 0) {
            classificationService.classifyAndStore(any())
        }
        
        // Should return existing classifications
        messages.collect { messageList ->
            assertEquals(2, messageList.size)
            
            val spamMessage = messageList.find { it.id == 1001L }
            assertNotNull(spamMessage)
            assertEquals(MessageCategory.SPAM, spamMessage.category)
            
            val otpMessage = messageList.find { it.id == 1002L }
            assertNotNull(otpMessage)
            assertEquals(MessageCategory.INBOX, otpMessage.category)
        }
    }

    @Test
    fun `test readAllMessages classifies new messages only`() = runTest {
        // Given - one existing message and one new message
        val existingMessage = SmsMessageEntity(
            id = 2001,
            sender = "+9876543210",
            content = "Existing message",
            timestamp = Date(System.currentTimeMillis() - 7200000),
            category = MessageCategory.INBOX,
            isRead = true,
            threadId = "thread1",
            isOutgoing = false,
            mlConfidence = 0.75f,
            userOverride = false,
            classificationReason = "Normal message"
        )
        
        coEvery { smsMessageDao.getAllMessages() } returns flowOf(listOf(existingMessage))
        coEvery { smsMessageDao.getMessageById(2001) } returns existingMessage
        coEvery { smsMessageDao.getMessageById(2002) } returns null
        
        // Mock system SMS with existing + new message
        val systemMessages = listOf(
            existingMessage,
            mockk<SmsMessageEntity>(relaxed = true) {
                every { id } returns 2002
                every { sender } returns "PROMO"
                every { content } returns "50% OFF Sale!"
                every { timestamp } returns Date()
                every { isRead } returns false
                every { threadId } returns "thread2"
                every { isOutgoing } returns false
            }
        )
        
        setupCursorForMessages(systemMessages)
        
        // When
        smsReader.readAllMessages()
        
        // Then - should only classify the new message
        coVerify(exactly = 1) {
            classificationService.classifyAndStore(match { message ->
                message.id == 2002L && message.sender == "PROMO"
            })
        }
        
        // Should not reclassify existing message
        coVerify(exactly = 0) {
            classificationService.classifyAndStore(match { message ->
                message.id == 2001L
            })
        }
    }

    @Test
    fun `test readAllMessages respects user overrides`() = runTest {
        // Given - message with user override
        val userOverrideMessage = SmsMessageEntity(
            id = 3001,
            sender = "MYSENDER",
            content = "User marked as important",
            timestamp = Date(),
            category = MessageCategory.INBOX,
            isRead = false,
            threadId = "thread1",
            isOutgoing = false,
            mlConfidence = 0.20f,
            userOverride = true,
            classificationReason = "User override: marked as important"
        )
        
        coEvery { smsMessageDao.getAllMessages() } returns flowOf(listOf(userOverrideMessage))
        coEvery { smsMessageDao.getMessageById(3001) } returns userOverrideMessage
        
        setupCursorForMessages(listOf(userOverrideMessage))
        
        // When
        val messages = smsReader.readAllMessages()
        
        // Then - should not reclassify user override
        coVerify(exactly = 0) {
            classificationService.classifyAndStore(any())
        }
        
        messages.collect { messageList ->
            val message = messageList.find { it.id == 3001L }
            assertNotNull(message)
            assertEquals(MessageCategory.INBOX, message.category)
            assertEquals(true, message.userOverride)
        }
    }

    @Test
    fun `test readUnreadMessages filters correctly`() = runTest {
        // Given - mix of read and unread messages
        val messages = listOf(
            SmsMessageEntity(
                id = 4001,
                sender = "+1111111111",
                content = "Unread message 1",
                timestamp = Date(),
                category = MessageCategory.INBOX,
                isRead = false,
                threadId = "thread1",
                isOutgoing = false,
                mlConfidence = 0.80f,
                userOverride = false,
                classificationReason = "Normal"
            ),
            SmsMessageEntity(
                id = 4002,
                sender = "+2222222222",
                content = "Read message",
                timestamp = Date(),
                category = MessageCategory.INBOX,
                isRead = true,
                threadId = "thread2",
                isOutgoing = false,
                mlConfidence = 0.75f,
                userOverride = false,
                classificationReason = "Normal"
            ),
            SmsMessageEntity(
                id = 4003,
                sender = "+3333333333",
                content = "Unread message 2",
                timestamp = Date(),
                category = MessageCategory.SPAM,
                isRead = false,
                threadId = "thread3",
                isOutgoing = false,
                mlConfidence = 0.95f,
                userOverride = false,
                classificationReason = "Spam"
            )
        )
        
        coEvery { smsMessageDao.getUnreadMessages() } returns flowOf(
            messages.filter { !it.isRead }
        )
        
        // When
        val unreadMessages = smsReader.readUnreadMessages()
        
        // Then
        unreadMessages.collect { messageList ->
            assertEquals(2, messageList.size)
            assert(messageList.all { !it.isRead })
            assert(messageList.any { it.id == 4001L })
            assert(messageList.any { it.id == 4003L })
            assert(messageList.none { it.id == 4002L })
        }
    }

    @Test
    fun `test syncWithSystemSms handles new and deleted messages`() = runTest {
        // Given - database has messages that system doesn't (deleted)
        val databaseMessages = listOf(
            SmsMessageEntity(
                id = 5001,
                sender = "+1234567890",
                content = "Still exists",
                timestamp = Date(),
                category = MessageCategory.INBOX,
                isRead = true,
                threadId = "thread1",
                isOutgoing = false,
                mlConfidence = 0.70f,
                userOverride = false,
                classificationReason = "Normal"
            ),
            SmsMessageEntity(
                id = 5002,
                sender = "+9876543210",
                content = "Deleted from system",
                timestamp = Date(),
                category = MessageCategory.SPAM,
                isRead = false,
                threadId = "thread2",
                isOutgoing = false,
                mlConfidence = 0.90f,
                userOverride = false,
                classificationReason = "Spam"
            )
        )
        
        // System only has one message (5001) plus a new one (5003)
        val systemMessages = listOf(
            databaseMessages[0], // Message 5001 still exists
            mockk<SmsMessageEntity>(relaxed = true) {
                every { id } returns 5003
                every { sender } returns "NEWSENDER"
                every { content } returns "New message"
                every { timestamp } returns Date()
                every { isRead } returns false
                every { threadId } returns "thread3"
                every { isOutgoing } returns false
            }
        )
        
        coEvery { smsMessageDao.getAllMessages() } returns flowOf(databaseMessages)
        coEvery { smsMessageDao.getMessageById(5001) } returns databaseMessages[0]
        coEvery { smsMessageDao.getMessageById(5002) } returns databaseMessages[1]
        coEvery { smsMessageDao.getMessageById(5003) } returns null
        
        setupCursorForMessages(systemMessages)
        
        // When
        smsReader.syncWithSystemSms()
        
        // Then
        // Should delete message 5002 (not in system)
        coVerify(exactly = 1) {
            smsMessageDao.deleteMessage(match { it.id == 5002L })
        }
        
        // Should classify new message 5003
        coVerify(exactly = 1) {
            classificationService.classifyAndStore(match { message ->
                message.id == 5003L && message.sender == "NEWSENDER"
            })
        }
        
        // Should not reclassify existing message 5001
        coVerify(exactly = 0) {
            classificationService.classifyAndStore(match { message ->
                message.id == 5001L
            })
        }
    }

    @Test
    fun `test markAsRead updates database`() = runTest {
        // Given
        val messageId = 6001L
        
        coEvery { smsMessageDao.markAsRead(messageId) } just Runs
        
        // When
        smsReader.markAsRead(messageId)
        
        // Then
        coVerify(exactly = 1) {
            smsMessageDao.markAsRead(messageId)
        }
    }

    @Test
    fun `test getMessagesByCategory filters correctly`() = runTest {
        // Given
        val spamMessages = listOf(
            SmsMessageEntity(
                id = 7001,
                sender = "SPAM1",
                content = "Win prizes!",
                timestamp = Date(),
                category = MessageCategory.SPAM,
                isRead = false,
                threadId = "thread1",
                isOutgoing = false,
                mlConfidence = 0.98f,
                userOverride = false,
                classificationReason = "Spam"
            ),
            SmsMessageEntity(
                id = 7002,
                sender = "SPAM2",
                content = "Claim your reward",
                timestamp = Date(),
                category = MessageCategory.SPAM,
                isRead = true,
                threadId = "thread2",
                isOutgoing = false,
                mlConfidence = 0.92f,
                userOverride = false,
                classificationReason = "Spam"
            )
        )
        
        coEvery { 
            smsMessageDao.getMessagesByCategory(MessageCategory.SPAM) 
        } returns flowOf(spamMessages)
        
        // When
        val messages = smsReader.getMessagesByCategory(MessageCategory.SPAM)
        
        // Then
        messages.collect { messageList ->
            assertEquals(2, messageList.size)
            assert(messageList.all { it.category == MessageCategory.SPAM })
        }
    }

    private fun setupCursorForMessages(messages: List<SmsMessageEntity>) {
        var currentIndex = -1
        
        every { cursor.moveToFirst() } answers {
            currentIndex = if (messages.isNotEmpty()) 0 else -1
            messages.isNotEmpty()
        }
        
        every { cursor.moveToNext() } answers {
            currentIndex++
            currentIndex < messages.size
        }
        
        every { cursor.getColumnIndexOrThrow(Telephony.Sms._ID) } returns 0
        every { cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS) } returns 1
        every { cursor.getColumnIndexOrThrow(Telephony.Sms.BODY) } returns 2
        every { cursor.getColumnIndexOrThrow(Telephony.Sms.DATE) } returns 3
        every { cursor.getColumnIndexOrThrow(Telephony.Sms.READ) } returns 4
        every { cursor.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID) } returns 5
        every { cursor.getColumnIndexOrThrow(Telephony.Sms.TYPE) } returns 6
        
        every { cursor.getLong(0) } answers {
            if (currentIndex in messages.indices) messages[currentIndex].id else 0
        }
        every { cursor.getString(1) } answers {
            if (currentIndex in messages.indices) messages[currentIndex].sender else ""
        }
        every { cursor.getString(2) } answers {
            if (currentIndex in messages.indices) messages[currentIndex].content else ""
        }
        every { cursor.getLong(3) } answers {
            if (currentIndex in messages.indices) messages[currentIndex].timestamp.time else 0
        }
        every { cursor.getInt(4) } answers {
            if (currentIndex in messages.indices) {
                if (messages[currentIndex].isRead) 1 else 0
            } else 0
        }
        every { cursor.getString(5) } answers {
            if (currentIndex in messages.indices) messages[currentIndex].threadId else ""
        }
        every { cursor.getInt(6) } answers {
            if (currentIndex in messages.indices) {
                if (messages[currentIndex].isOutgoing) Telephony.Sms.MESSAGE_TYPE_SENT 
                else Telephony.Sms.MESSAGE_TYPE_INBOX
            } else Telephony.Sms.MESSAGE_TYPE_INBOX
        }
        
        every { cursor.close() } just Runs
        
        every { 
            contentResolver.query(
                any<Uri>(),
                any(),
                any(),
                any(),
                any()
            )
        } returns cursor
    }
}