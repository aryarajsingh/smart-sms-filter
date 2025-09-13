package com.smartsmsfilter.di

import com.smartsmsfilter.classification.PrivateContextualClassifier
import com.smartsmsfilter.classification.SimpleMessageClassifier
import com.smartsmsfilter.data.preferences.PreferencesSource
import com.smartsmsfilter.data.preferences.UserPreferences
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.MessageClassification
import com.smartsmsfilter.domain.model.SmsMessage
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.Mockito.*
import java.util.*

/**
 * Unit tests for RuleBasedSmsClassifierWrapper
 * Tests integration of existing rule-based classifiers with new interface
 */
@RunWith(MockitoJUnitRunner::class)
class RuleBasedSmsClassifierWrapperTest {

    @Mock
    private lateinit var mockSimpleClassifier: SimpleMessageClassifier

    @Mock
    private lateinit var mockContextualClassifier: PrivateContextualClassifier

    @Mock
    private lateinit var mockPreferencesSource: PreferencesSource

    private lateinit var wrapper: RuleBasedSmsClassifierWrapper

    // Test data
    private val defaultUserPreferences = UserPreferences()
    
    private val testMessage = SmsMessage(
        id = 1L,
        sender = "TEST-SENDER",
        content = "Test message content",
        timestamp = Date(),
        category = MessageCategory.NEEDS_REVIEW
    )

    @Before
    fun setup() {
        // Setup mock preferences
        `when`(mockPreferencesSource.userPreferences).thenReturn(flowOf(defaultUserPreferences))

        wrapper = RuleBasedSmsClassifierWrapper(
            simpleClassifier = mockSimpleClassifier,
            contextualClassifier = mockContextualClassifier,
            preferencesSource = mockPreferencesSource
        )
    }

    @Test
    fun `test classifier interface implementation`() {
        // Test that wrapper implements SmsClassifier interface correctly
        assert(wrapper is com.smartsmsfilter.domain.classifier.SmsClassifier)
        assert(wrapper.getConfidenceThreshold() > 0f)
        assert(wrapper.getConfidenceThreshold() <= 1f)
    }

    @Test
    fun `test OTP detection takes priority`() = runBlocking {
        val otpMessage = testMessage.copy(content = "Your OTP is 123456")
        
        // Mock simple classifier to return SPAM (should be overridden)
        `when`(mockSimpleClassifier.classifyMessage(any(), any())).thenReturn(MessageCategory.SPAM)

        val result = wrapper.classifyMessage(otpMessage)

        // OTP should always go to INBOX regardless of simple classifier result
        assert(result.category == MessageCategory.INBOX)
        assert(result.confidence > 0.9f) // High confidence for OTP
        assert(result.reasons.any { it.contains("OTP") })
    }

    @Test
    fun `test simple classifier integration`() = runBlocking {
        val normalMessage = testMessage.copy(content = "Normal message")
        
        // Mock simple classifier to return INBOX
        `when`(mockSimpleClassifier.classifyMessage(any(), any())).thenReturn(MessageCategory.INBOX)

        val result = wrapper.classifyMessage(normalMessage)

        // Should use simple classifier result
        assert(result.category == MessageCategory.INBOX)
        assert(result.confidence > 0f)
        assert(result.reasons.isNotEmpty())
    }

    @Test
    fun `test contextual classifier integration`() = runBlocking {
        val normalMessage = testMessage.copy(content = "Contextual test message")
        
        // Mock simple classifier
        `when`(mockSimpleClassifier.classifyMessage(any(), any())).thenReturn(MessageCategory.NEEDS_REVIEW)
        
        // Mock contextual classifier with high confidence
        val contextualResult = MessageClassification(
            category = MessageCategory.SPAM,
            confidence = 0.8f,
            reasons = listOf("Contextual analysis indicates spam")
        )
        `when`(mockContextualClassifier.classifyWithContext(any(), any())).thenReturn(contextualResult)

        val result = wrapper.classifyMessage(normalMessage)

        // Should get result from contextual classifier due to high confidence
        assert(result.reasons.any { it.contains("Contextual analysis") })
    }

    @Test
    fun `test batch classification`() = runBlocking {
        val messages = listOf(
            testMessage.copy(id = 1L),
            testMessage.copy(id = 2L),
            testMessage.copy(id = 3L)
        )

        // Mock simple classifier for all messages
        `when`(mockSimpleClassifier.classifyMessage(any(), any())).thenReturn(MessageCategory.INBOX)

        val results = wrapper.classifyBatch(messages)

        // Should classify all messages
        assert(results.size == messages.size)
        messages.forEach { message ->
            val result = results[message.id]
            assert(result != null)
            assert(result!!.category != null)
            assert(result.confidence > 0f)
        }
    }

    @Test
    fun `test learning from correction`() = runBlocking {
        val correction = MessageClassification(
            category = MessageCategory.SPAM,
            confidence = 1.0f,
            reasons = listOf("User correction")
        )

        // Should delegate to contextual classifier
        wrapper.learnFromCorrection(testMessage, correction)
        
        // Verify contextual classifier was called
        verify(mockContextualClassifier).learnFromUserCorrection(
            eq(testMessage),
            eq(testMessage.category),
            eq(correction.category)
        )
    }

    @Test
    fun `test error handling in classification`() = runBlocking {
        // Mock simple classifier to throw exception
        `when`(mockSimpleClassifier.classifyMessage(any(), any())).thenThrow(RuntimeException("Test exception"))

        val result = wrapper.classifyMessage(testMessage)

        // Should gracefully handle errors
        assert(result.category == MessageCategory.NEEDS_REVIEW)
        assert(result.confidence < 0.5f)
        assert(result.reasons.any { it.contains("failed") })
    }

    @Test
    fun `test confidence threshold`() {
        val threshold = wrapper.getConfidenceThreshold()
        
        // Should have reasonable threshold
        assert(threshold >= 0.5f)
        assert(threshold <= 1.0f)
    }
}