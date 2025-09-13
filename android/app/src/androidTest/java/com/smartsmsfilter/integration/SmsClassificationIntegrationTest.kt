package com.smartsmsfilter.integration

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.smartsmsfilter.data.database.SmsDatabase
import com.smartsmsfilter.domain.classifier.ClassificationService
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.repository.SmsRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SmsClassificationIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var classificationService: ClassificationService

    @Inject
    lateinit var smsRepository: SmsRepository

    @Inject
    lateinit var database: SmsDatabase

    @Before
    fun setup() {
        hiltRule.inject()
        // Clear database before each test
        runBlocking {
            database.clearAllTables()
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testOtpMessageClassification() = runBlocking {
        // Given
        val otpMessage = SmsMessage(
            id = 1L,
            sender = "HDFC-BANK",
            content = "Your OTP is 123456 for transaction of Rs. 5000",
            timestamp = Date(),
            isRead = false
        )

        // When
        val result = classificationService.classifyAndStore(otpMessage)

        // Then
        assertEquals(MessageCategory.INBOX, result.category)
        assertTrue(result.confidence > 0.7f)
        assertTrue(result.reasons.any { it.contains("OTP", ignoreCase = true) })
    }

    @Test
    fun testSpamMessageClassification() = runBlocking {
        // Given
        val spamMessage = SmsMessage(
            id = 2L,
            sender = "VM-PROMO",
            content = "Congratulations! You've won 10 lakhs! Click http://bit.ly/win now",
            timestamp = Date(),
            isRead = false
        )

        // When
        val result = classificationService.classifyAndStore(spamMessage)

        // Then
        assertEquals(MessageCategory.SPAM, result.category)
        assertTrue(result.confidence > 0.6f)
    }

    @Test
    fun testBankingMessageClassification() = runBlocking {
        // Given
        val bankMessage = SmsMessage(
            id = 3L,
            sender = "SBIINB",
            content = "Rs 2500.00 debited from A/c XX1234 on 01-01-24. Balance: Rs 10000.00",
            timestamp = Date(),
            isRead = false
        )

        // When
        val result = classificationService.classifyAndStore(bankMessage)

        // Then
        assertEquals(MessageCategory.INBOX, result.category)
        assertTrue(result.reasons.any { it.contains("bank", ignoreCase = true) || it.contains("financial", ignoreCase = true) })
    }

    @Test
    fun testMessagePersistence() = runBlocking {
        // Given
        val message = SmsMessage(
            id = 4L,
            sender = "TEST-SENDER",
            content = "Test message content",
            timestamp = Date(),
            isRead = false
        )

        // When
        classificationService.classifyAndStore(message)

        // Then
        val savedMessage = smsRepository.getMessageById(4L)
        assertNotNull(savedMessage)
        assertEquals("TEST-SENDER", savedMessage.sender)
        assertEquals("Test message content", savedMessage.content)
    }

    @Test
    fun testBatchClassification() = runBlocking {
        // Given
        val messages = listOf(
            SmsMessage(5L, "BANK", "Your balance is Rs 5000", Date(), false),
            SmsMessage(6L, "PROMO", "Sale! 50% off on all items", Date(), false),
            SmsMessage(7L, "OTP", "Your verification code is 987654", Date(), false)
        )

        // When
        val results = messages.map { message ->
            classificationService.classifyAndStore(message)
        }

        // Then
        assertEquals(3, results.size)
        assertTrue(results.all { it.confidence > 0f })
        assertTrue(results.all { it.reasons.isNotEmpty() })
    }

    @Test
    fun testUserCorrectionLearning() = runBlocking {
        // Given
        val message = SmsMessage(
            id = 8L,
            sender = "MYNTRA",
            content = "Your order has been delivered",
            timestamp = Date(),
            isRead = false
        )

        // When - Initial classification
        val initialResult = classificationService.classifyAndStore(message)
        
        // User corrects the classification
        val correctedCategory = if (initialResult.category == MessageCategory.SPAM) {
            MessageCategory.INBOX
        } else {
            MessageCategory.SPAM
        }
        
        smsRepository.updateMessageCategory(8L, correctedCategory)
        
        // Future messages from same sender should consider this preference
        val newMessage = SmsMessage(
            id = 9L,
            sender = "MYNTRA",
            content = "New collection available",
            timestamp = Date(),
            isRead = false
        )
        
        val newResult = classificationService.classifyAndStore(newMessage)

        // Then
        assertNotNull(newResult)
        // The service should have learned from the correction
        // (actual behavior depends on implementation)
    }

    @Test
    fun testEdgeCases() = runBlocking {
        // Test empty message
        val emptyMessage = SmsMessage(10L, "SENDER", "", Date(), false)
        val emptyResult = classificationService.classifyAndStore(emptyMessage)
        assertNotNull(emptyResult)

        // Test very long message
        val longContent = "Lorem ipsum ".repeat(100)
        val longMessage = SmsMessage(11L, "SENDER", longContent, Date(), false)
        val longResult = classificationService.classifyAndStore(longMessage)
        assertNotNull(longResult)

        // Test special characters
        val specialMessage = SmsMessage(12L, "SENDER", "₹💰🎉 Special offer!", Date(), false)
        val specialResult = classificationService.classifyAndStore(specialMessage)
        assertNotNull(specialResult)
    }

    @Test
    fun testPerformance() = runBlocking {
        // Measure classification time
        val startTime = System.currentTimeMillis()
        
        val messages = (1..100).map { id ->
            SmsMessage(
                id = id.toLong(),
                sender = "SENDER$id",
                content = "Message content $id",
                timestamp = Date(),
                isRead = false
            )
        }

        messages.forEach { message ->
            classificationService.classifyAndStore(message)
        }

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        // Should process 100 messages in under 10 seconds
        assertTrue(totalTime < 10000, "Classification took too long: ${totalTime}ms")
        
        // Average time per message should be under 100ms
        val avgTime = totalTime / messages.size
        assertTrue(avgTime < 100, "Average classification time too high: ${avgTime}ms")
    }
}