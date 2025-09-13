package com.smartsmsfilter.classification

import com.smartsmsfilter.data.preferences.*
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.Date

class ClassifierDebugTest {

    private class FakePreferencesSource(val prefs: UserPreferences) : PreferencesSource {
        override val userPreferences = MutableStateFlow(prefs)
    }

    private fun createMessage(sender: String, content: String): SmsMessage {
        return SmsMessage(
            id = 1L,
            sender = sender,
            content = content,
            timestamp = Date(),
            category = MessageCategory.NEEDS_REVIEW
        )
    }

    @Test
    fun `debug classifier behavior`() = runTest {
        val classifier = PrivateContextualClassifier(FakePreferencesSource(UserPreferences()))
        
        val message = createMessage("HDFC-BANK", "Your account balance is Rs 25000")
        val result = classifier.classifyWithContext(message, updateContext = false)
        
        println("=== DEBUG CLASSIFIER ===")
        println("Category: ${result.category}")
        println("Confidence: ${result.confidence}")
        println("Reasons: ${result.reasons}")
        println("========================")
        
        // Just verify it doesn't crash
        assert(result.confidence >= 0.0f)
        assert(result.reasons.isNotEmpty())
    }
}