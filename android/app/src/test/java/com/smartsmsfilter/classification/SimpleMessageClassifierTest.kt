package com.smartsmsfilter.classification

import com.smartsmsfilter.data.preferences.*
import com.smartsmsfilter.domain.model.MessageCategory
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class SimpleMessageClassifierTest {

    private class FakePreferencesSource(val prefs: UserPreferences) : PreferencesSource {
        override val userPreferences = MutableStateFlow(prefs)
    }

    private fun makeClassifier(prefs: UserPreferences = UserPreferences()): SimpleMessageClassifier {
        val fake = FakePreferencesSource(prefs)
        return SimpleMessageClassifier(fake)
    }

    @Test
    fun otpAlwaysInbox() {
        val classifier = makeClassifier()
        val cat = classifier.classifyMessage("VM-ABCD", "123456 is your OTP for login")
        assertEquals(MessageCategory.INBOX, cat)
    }

    @Test
    fun ecommerceRespectsOnboardingImportantTypes() {
        val prefs = UserPreferences(
            importantMessageTypes = setOf(ImportantMessageType.ECOMMERCE)
        )
        val classifier = makeClassifier(prefs)
        val cat = classifier.classifyMessage("AMZN", "Your order has been shipped. Track your delivery here")
        assertEquals(MessageCategory.INBOX, cat)
    }

    @Test
    fun promoWithShortLinkRaisesSpamScoreButNotOverAggressive() {
        val classifier = makeClassifier(
            UserPreferences(
                filteringMode = FilteringMode.MODERATE,
                spamTolerance = SpamTolerance.MODERATE
            )
        )
        val cat = classifier.classifyMessage("VM-PROMO", "Limited time offer! Click http://bit.ly/deal to get discount!!!")
        // Could be SPAM or NEEDS_REVIEW based on scoring; at minimum not INBOX
        assertEquals(true, cat == MessageCategory.SPAM || cat == MessageCategory.NEEDS_REVIEW)
    }

    @Test
    fun travelImportantTypeInbox() {
        val prefs = UserPreferences(
            importantMessageTypes = setOf(ImportantMessageType.TRAVEL)
        )
        val classifier = makeClassifier(prefs)
        val cat = classifier.classifyMessage("IRCTC", "PNR 1234567890 ticket booking confirmed")
        assertEquals(MessageCategory.INBOX, cat)
    }

    @Test
    fun utilitiesImportantTypeInbox() {
        val prefs = UserPreferences(
            importantMessageTypes = setOf(ImportantMessageType.UTILITIES)
        )
        val classifier = makeClassifier(prefs)
        val cat = classifier.classifyMessage("BESCOM", "Electricity bill due on 12 Sep. Pay to avoid late fee")
        assertEquals(MessageCategory.INBOX, cat)
    }
}
