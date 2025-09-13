package com.smartsmsfilter.ml

import android.content.Context
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.util.*
import org.mockito.Mockito.*

/**
 * Unit tests for TensorFlowLiteSmsClassifier
 * Tests basic functionality, error handling, and model integration
 */
@RunWith(MockitoJUnitRunner::class)
class TensorFlowLiteSmsClassifierTest {

    @Mock
    private lateinit var mockContext: Context

    private lateinit var classifier: TensorFlowLiteSmsClassifier

    // Test SMS messages
    private val otpMessage = SmsMessage(
        id = 1L,
        sender = "SBIINB",
        content = "Your OTP is 123456. Valid for 10 minutes. Do not share with anyone.",
        timestamp = Date(),
        category = MessageCategory.NEEDS_REVIEW
    )

    private val spamMessage = SmsMessage(
        id = 2L,
        sender = "VM-PROMO",
        content = "Congratulations! You have won 1 Crore! Call now to claim your prize!",
        timestamp = Date(),
        category = MessageCategory.NEEDS_REVIEW
    )

    private val bankingMessage = SmsMessage(
        id = 3L,
        sender = "HDFCBANK",
        content = "Rs 5000 debited from A/c XX1234 on 13-SEP-25. Avl Bal Rs 15000",
        timestamp = Date(),
        category = MessageCategory.NEEDS_REVIEW
    )

    @Before
    fun setup() {
        classifier = TensorFlowLiteSmsClassifier(mockContext)
    }

    @Test
    fun `test classifier interface implementation`() {
        // Test that classifier implements SmsClassifier interface correctly
        assert(classifier is com.smartsmsfilter.domain.classifier.SmsClassifier)
        assert(classifier.getConfidenceThreshold() > 0f)
        assert(classifier.getConfidenceThreshold() <= 1f)
    }

    @Test
    fun `test classification without model loading should fallback gracefully`() = runBlocking {
        // Mock context to simulate missing assets
        `when`(mockContext.assets).thenReturn(null)

        val result = classifier.classifyMessage(otpMessage)

        // Should fallback to NEEDS_REVIEW when model fails to load
        assert(result.category == MessageCategory.NEEDS_REVIEW)
        assert(result.confidence < 0.5f)
        assert(result.reasons.any { it.contains("failed") })
    }

    @Test
    fun `test batch classification consistency`() = runBlocking {
        val messages = listOf(otpMessage, spamMessage, bankingMessage)
        
        val batchResults = classifier.classifyBatch(messages)
        
        // Check that all messages get classified
        assert(batchResults.size == messages.size)
        
        // Each message should have a classification
        messages.forEach { message ->
            val result = batchResults[message.id]
            assert(result != null)
            assert(result!!.category in MessageCategory.values())
            assert(result.confidence >= 0f && result.confidence <= 1f)
            assert(result.reasons.isNotEmpty())
        }
    }

    @Test
    fun `test learning from correction doesn't crash`() = runBlocking {
        val correction = com.smartsmsfilter.domain.model.MessageClassification(
            category = MessageCategory.SPAM,
            confidence = 1.0f,
            reasons = listOf("User correction")
        )

        // Should not throw exception (TFLite models don't learn online)
        classifier.learnFromCorrection(spamMessage, correction)
    }

    @Test
    fun `test confidence threshold is reasonable`() {
        val threshold = classifier.getConfidenceThreshold()
        
        // Confidence threshold should be reasonable (between 0.5 and 0.8)
        assert(threshold >= 0.5f)
        assert(threshold <= 0.8f)
    }

    @Test
    fun `test empty message handling`() = runBlocking {
        val emptyMessage = SmsMessage(
            id = 99L,
            sender = "",
            content = "",
            timestamp = Date(),
            category = MessageCategory.NEEDS_REVIEW
        )

        val result = classifier.classifyMessage(emptyMessage)
        
        // Should handle empty content gracefully
        assert(result.category != null)
        assert(result.confidence >= 0f)
    }

    @Test
    fun `test very long message handling`() = runBlocking {
        val longContent = "A".repeat(1000) // Very long message
        val longMessage = SmsMessage(
            id = 100L,
            sender = "SENDER",
            content = longContent,
            timestamp = Date(),
            category = MessageCategory.NEEDS_REVIEW
        )

        val result = classifier.classifyMessage(longMessage)
        
        // Should handle long content without crashing
        assert(result.category != null)
        assert(result.confidence >= 0f)
        assert(result.reasons.isNotEmpty())
    }
}