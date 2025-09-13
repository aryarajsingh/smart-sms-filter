package com.smartsmsfilter.service

import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import com.smartsmsfilter.domain.classifier.ClassificationService
import com.smartsmsfilter.domain.common.CoroutineScopeManager
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.MessageClassification
import com.smartsmsfilter.domain.model.SmsMessage as DomainSmsMessage
import com.smartsmsfilter.domain.repository.SmsRepository
import com.smartsmsfilter.services.SmartNotificationManager
import com.smartsmsfilter.utils.DefaultSmsAppHelper
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals

class SmsReceiverTest {

    private lateinit var smsReceiver: SmsReceiver
    private lateinit var context: Context
    private lateinit var intent: Intent
    private lateinit var smsRepository: SmsRepository
    private lateinit var scopeManager: CoroutineScopeManager
    private lateinit var classificationService: ClassificationService
    private lateinit var notificationManager: SmartNotificationManager

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        context = mockk(relaxed = true)
        intent = mockk(relaxed = true)
        smsRepository = mockk(relaxed = true)
        scopeManager = mockk(relaxed = true)
        classificationService = mockk(relaxed = true)
        notificationManager = mockk(relaxed = true)
        
        smsReceiver = SmsReceiver()
        smsReceiver.smsRepository = smsRepository
        smsReceiver.scopeManager = scopeManager
        smsReceiver.classificationService = classificationService
        smsReceiver.notificationManager = notificationManager
        
        mockkStatic(DefaultSmsAppHelper::class)
        mockkStatic(Telephony.Sms.Intents::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test incoming SMS is properly classified and stored`() = runTest {
        // Given
        val testSender = "+1234567890"
        val testContent = "Test message content"
        val testTimestamp = System.currentTimeMillis()
        
        val smsMessage = mockk<SmsMessage> {
            every { originatingAddress } returns testSender
            every { messageBody } returns testContent
            every { timestampMillis } returns testTimestamp
        }
        
        every { DefaultSmsAppHelper.isDefaultSmsApp(context) } returns true
        every { intent.action } returns Telephony.Sms.Intents.SMS_DELIVER_ACTION
        every { Telephony.Sms.Intents.getMessagesFromIntent(intent) } returns arrayOf(smsMessage)
        
        val expectedClassification = MessageClassification(
            category = MessageCategory.INBOX,
            confidence = 0.85f,
            reasons = listOf("ML classified as important"),
            messageId = 123L
        )
        
        coEvery { 
            classificationService.classifyAndStore(any()) 
        } returns expectedClassification
        
        // When
        smsReceiver.onReceive(context, intent)
        
        // Allow coroutine to complete
        Thread.sleep(100)
        
        // Then
        coVerify(exactly = 1) { 
            classificationService.classifyAndStore(
                match { message ->
                    message.sender.contains("1234567890") &&
                    message.content == testContent &&
                    message.category == MessageCategory.NEEDS_REVIEW &&
                    !message.isOutgoing
                }
            )
        }
        
        coVerify(exactly = 1) {
            notificationManager.showSmartNotification(
                match { message ->
                    message.category == MessageCategory.INBOX &&
                    message.id == 123L
                }
            )
        }
    }

    @Test
    fun `test SMS is not processed when not default app and wrong action`() = runTest {
        // Given
        every { DefaultSmsAppHelper.isDefaultSmsApp(context) } returns false
        every { intent.action } returns Telephony.Sms.Intents.SMS_DELIVER_ACTION
        
        // When
        smsReceiver.onReceive(context, intent)
        
        // Then
        coVerify(exactly = 0) { 
            classificationService.classifyAndStore(any())
        }
        coVerify(exactly = 0) {
            notificationManager.showSmartNotification(any())
        }
    }

    @Test
    fun `test handles null or empty messages gracefully`() = runTest {
        // Given
        every { DefaultSmsAppHelper.isDefaultSmsApp(context) } returns true
        every { intent.action } returns Telephony.Sms.Intents.SMS_DELIVER_ACTION
        every { Telephony.Sms.Intents.getMessagesFromIntent(intent) } returns null
        
        // When
        smsReceiver.onReceive(context, intent)
        
        // Then
        coVerify(exactly = 0) { 
            classificationService.classifyAndStore(any())
        }
    }

    @Test
    fun `test normalizes phone numbers correctly`() = runTest {
        // Given
        val messyNumber = "+1 (234) 567-8900"
        val smsMessage = mockk<SmsMessage> {
            every { originatingAddress } returns messyNumber
            every { messageBody } returns "Test"
            every { timestampMillis } returns System.currentTimeMillis()
        }
        
        every { DefaultSmsAppHelper.isDefaultSmsApp(context) } returns true
        every { intent.action } returns Telephony.Sms.Intents.SMS_DELIVER_ACTION
        every { Telephony.Sms.Intents.getMessagesFromIntent(intent) } returns arrayOf(smsMessage)
        
        coEvery { 
            classificationService.classifyAndStore(any()) 
        } returns MessageClassification(
            category = MessageCategory.SPAM,
            confidence = 0.9f,
            reasons = listOf("Promotional content"),
            messageId = 456L
        )
        
        // When
        smsReceiver.onReceive(context, intent)
        Thread.sleep(100)
        
        // Then
        coVerify(exactly = 1) { 
            classificationService.classifyAndStore(
                match { message ->
                    // Should be normalized
                    message.sender == "+12345678900"
                }
            )
        }
    }

    @Test
    fun `test ML classification returns correct category`() = runTest {
        // Given - spam message
        val spamMessage = mockk<SmsMessage> {
            every { originatingAddress } returns "PROMO"
            every { messageBody } returns "Congratulations! You've won $1000000!"
            every { timestampMillis } returns System.currentTimeMillis()
        }
        
        every { DefaultSmsAppHelper.isDefaultSmsApp(context) } returns true
        every { intent.action } returns Telephony.Sms.Intents.SMS_DELIVER_ACTION
        every { Telephony.Sms.Intents.getMessagesFromIntent(intent) } returns arrayOf(spamMessage)
        
        coEvery { 
            classificationService.classifyAndStore(any()) 
        } returns MessageClassification(
            category = MessageCategory.SPAM,
            confidence = 0.95f,
            reasons = listOf("ML model classified as: Spam/promotional content"),
            messageId = 789L
        )
        
        // When
        smsReceiver.onReceive(context, intent)
        Thread.sleep(100)
        
        // Then
        coVerify(exactly = 1) {
            notificationManager.showSmartNotification(
                match { message ->
                    message.category == MessageCategory.SPAM
                }
            )
        }
    }
}