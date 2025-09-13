package com.smartsmsfilter.classification

import com.smartsmsfilter.data.preferences.*
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.data.contacts.ContactManager
import com.smartsmsfilter.data.contacts.Contact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.*

class SimpleMessageClassifierTest {

    private class FakePreferencesSource(val prefs: UserPreferences) : PreferencesSource {
        override val userPreferences = MutableStateFlow(prefs)
    }

    private fun makeClassifier(prefs: UserPreferences = UserPreferences()): SimpleMessageClassifier {
        val fake = FakePreferencesSource(prefs)
        val mockContactManager = mock(ContactManager::class.java)
        // Mock contact manager to return null (no contacts found) by default using runBlocking
        `when`(runBlocking { mockContactManager.getContactByPhoneNumber(anyString()) }).thenReturn(
            Contact(id = 0, name = "Unknown", phoneNumber = "", phoneType = "", photoUri = null, isFrequentContact = false)
        )
        return SimpleMessageClassifier(fake, mockContactManager)
    }

    @Test
    fun otpAlwaysInbox() = runTest {
        val classifier = makeClassifier()
        val cat = classifier.classifyMessage("VM-ABCD", "123456 is your OTP for login")
        assertEquals(MessageCategory.INBOX, cat)
    }

    @Test
    fun ecommerceRespectsOnboardingImportantTypes() = runTest {
        val prefs = UserPreferences(
            importantMessageTypes = setOf(ImportantMessageType.ECOMMERCE)
        )
        val classifier = makeClassifier(prefs)
        val cat = classifier.classifyMessage("AMZN", "Your order has been shipped. Track your delivery here")
        assertEquals(MessageCategory.INBOX, cat)
    }

    @Test
    fun promoWithShortLinkRaisesSpamScoreButNotOverAggressive() = runTest {
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
    fun travelImportantTypeInbox() = runTest {
        val prefs = UserPreferences(
            importantMessageTypes = setOf(ImportantMessageType.TRAVEL)
        )
        val classifier = makeClassifier(prefs)
        val cat = classifier.classifyMessage("IRCTC", "PNR 1234567890 ticket booking confirmed")
        assertEquals(MessageCategory.INBOX, cat)
    }

    @Test
    fun utilitiesImportantTypeInbox() = runTest {
        val prefs = UserPreferences(
            importantMessageTypes = setOf(ImportantMessageType.UTILITIES)
        )
        val classifier = makeClassifier(prefs)
        val cat = classifier.classifyMessage("BESCOM", "Electricity bill due on 12 Sep. Pay to avoid late fee")
        assertEquals(MessageCategory.INBOX, cat)
    }
    
    @Test
    fun knownContactsAlwaysGoToInbox() = runTest {
        val prefs = UserPreferences() // No special preferences
        val fake = FakePreferencesSource(prefs)
        val mockContactManager = mock(ContactManager::class.java)
        
        // Mock a known contact (real contact with ID > 0) using runBlocking
        `when`(runBlocking { mockContactManager.getContactByPhoneNumber("+1234567890") }).thenReturn(
            Contact(id = 123, name = "John Doe", phoneNumber = "+1234567890", phoneType = "Mobile", photoUri = null, isFrequentContact = false)
        )
        
        val classifier = SimpleMessageClassifier(fake, mockContactManager)
        val cat = classifier.classifyMessage("+1234567890", "Hey, how are you doing?")
        
        // Known contacts should always go to inbox
        assertEquals(MessageCategory.INBOX, cat)
    }
    
    @Test
    fun explicitSpamWarningsAlwaysBlocked() = runTest {
        val prefs = UserPreferences() 
        val fake = FakePreferencesSource(prefs)
        val mockContactManager = mock(ContactManager::class.java)
        
        // Even mock a known contact using runBlocking
        `when`(runBlocking { mockContactManager.getContactByPhoneNumber("+1234567890") }).thenReturn(
            Contact(id = 123, name = "John Doe", phoneNumber = "+1234567890", phoneType = "Mobile", photoUri = null, isFrequentContact = false)
        )
        
        val classifier = SimpleMessageClassifier(fake, mockContactManager)
        val cat = classifier.classifyMessage("+1234567890", "Airtel Warning: SPAM - This message contains spam content")
        
        // Explicit spam warnings should be blocked even from known contacts
        assertEquals(MessageCategory.SPAM, cat)
    }
}
