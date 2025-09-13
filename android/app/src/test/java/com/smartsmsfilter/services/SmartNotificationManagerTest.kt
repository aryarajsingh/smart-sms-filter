package com.smartsmsfilter.services

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.smartsmsfilter.data.contacts.Contact
import com.smartsmsfilter.data.contacts.ContactManager
import com.smartsmsfilter.data.preferences.PreferencesManager
import com.smartsmsfilter.data.preferences.UserPreferences
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date
import kotlin.test.assertTrue

class SmartNotificationManagerTest {

    private lateinit var notificationManager: SmartNotificationManager
    private lateinit var context: Context
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var contactManager: ContactManager
    private lateinit var androidNotificationManager: NotificationManager
    private lateinit var notificationManagerCompat: NotificationManagerCompat

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        
        context = mockk(relaxed = true)
        preferencesManager = mockk(relaxed = true)
        contactManager = mockk(relaxed = true)
        androidNotificationManager = mockk(relaxed = true)
        notificationManagerCompat = mockk(relaxed = true)
        
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns androidNotificationManager
        
        mockkStatic(NotificationManagerCompat::class)
        every { NotificationManagerCompat.from(context) } returns notificationManagerCompat
        every { notificationManagerCompat.areNotificationsEnabled() } returns true
        
        notificationManager = SmartNotificationManager(context, preferencesManager, contactManager)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test notification shows contact name when available`() = runTest {
        // Given
        val phoneNumber = "+1234567890"
        val contactName = "John Doe"
        val messageContent = "Hello, this is a test message"
        
        val contact = Contact(
            id = 1,
            name = contactName,
            phoneNumber = phoneNumber,
            phoneType = "Mobile",
            photoUri = null,
            isFrequentContact = false
        )
        
        coEvery { contactManager.getContactByPhoneNumber(phoneNumber) } returns contact
        
        every { preferencesManager.userPreferences } returns flowOf(
            UserPreferences(enableSmartNotifications = true)
        )
        
        val message = SmsMessage(
            id = 123,
            sender = phoneNumber,
            content = messageContent,
            timestamp = Date(),
            category = MessageCategory.INBOX,
            isRead = false,
            threadId = phoneNumber,
            isOutgoing = false
        )
        
        // When
        notificationManager.showSmartNotification(message)
        
        // Then
        coVerify(exactly = 1) {
            contactManager.getContactByPhoneNumber(phoneNumber)
        }
        
        verify(exactly = 1) {
            notificationManagerCompat.notify(any(), match { notification ->
                // Verify the notification was created with contact name
                notification.extras.getString("android.title")?.contains(contactName) == true ||
                notification.extras.getString("android.text")?.contains(contactName) == true
            })
        }
    }

    @Test
    fun `test notification falls back to phone number when contact not found`() = runTest {
        // Given
        val phoneNumber = "+9876543210"
        val messageContent = "Test message without contact"
        
        coEvery { contactManager.getContactByPhoneNumber(phoneNumber) } returns null
        
        every { preferencesManager.userPreferences } returns flowOf(
            UserPreferences(enableSmartNotifications = true)
        )
        
        val message = SmsMessage(
            id = 456,
            sender = phoneNumber,
            content = messageContent,
            timestamp = Date(),
            category = MessageCategory.SPAM,
            isRead = false,
            threadId = phoneNumber,
            isOutgoing = false
        )
        
        // When
        notificationManager.showSmartNotification(message)
        
        // Then
        coVerify(exactly = 1) {
            contactManager.getContactByPhoneNumber(phoneNumber)
        }
        
        verify(exactly = 1) {
            notificationManagerCompat.notify(any(), match { notification ->
                // Should show phone number when contact not found
                notification.extras.getString("android.title")?.contains(phoneNumber) == true ||
                notification.extras.getString("android.text")?.contains(phoneNumber) == true
            })
        }
    }

    @Test
    fun `test notification handles contact lookup errors gracefully`() = runTest {
        // Given
        val phoneNumber = "+5551234567"
        
        coEvery { 
            contactManager.getContactByPhoneNumber(phoneNumber) 
        } throws Exception("Database error")
        
        every { preferencesManager.userPreferences } returns flowOf(
            UserPreferences(enableSmartNotifications = true)
        )
        
        val message = SmsMessage(
            id = 789,
            sender = phoneNumber,
            content = "Test error handling",
            timestamp = Date(),
            category = MessageCategory.NEEDS_REVIEW,
            isRead = false,
            threadId = phoneNumber,
            isOutgoing = false
        )
        
        // When
        notificationManager.showSmartNotification(message)
        
        // Then - should not crash and should show phone number
        verify(exactly = 1) {
            notificationManagerCompat.notify(any(), match { notification ->
                notification.extras.getString("android.title")?.contains(phoneNumber) == true ||
                notification.extras.getString("android.text")?.contains(phoneNumber) == true
            })
        }
    }

    @Test
    fun `test spam messages get silent notification`() = runTest {
        // Given
        val spamMessage = SmsMessage(
            id = 999,
            sender = "PROMO",
            content = "Buy now! 50% off!",
            timestamp = Date(),
            category = MessageCategory.SPAM,
            isRead = false,
            threadId = "PROMO",
            isOutgoing = false
        )
        
        every { preferencesManager.userPreferences } returns flowOf(
            UserPreferences(enableSmartNotifications = true)
        )
        
        coEvery { contactManager.getContactByPhoneNumber(any()) } returns null
        
        // When
        notificationManager.showSmartNotification(spamMessage)
        
        // Then
        verify(exactly = 1) {
            notificationManagerCompat.notify(any(), match { notification ->
                // Spam should use silent channel
                notification.channelId == SmartNotificationManager.CHANNEL_SILENT
            })
        }
    }

    @Test
    fun `test important messages get high priority notification`() = runTest {
        // Given - OTP message
        val otpMessage = SmsMessage(
            id = 111,
            sender = "BANK",
            content = "Your OTP is 123456",
            timestamp = Date(),
            category = MessageCategory.INBOX,
            isRead = false,
            threadId = "BANK",
            isOutgoing = false,
            isImportant = true
        )
        
        every { preferencesManager.userPreferences } returns flowOf(
            UserPreferences(enableSmartNotifications = true)
        )
        
        coEvery { contactManager.getContactByPhoneNumber(any()) } returns null
        
        // When
        notificationManager.showSmartNotification(otpMessage)
        
        // Then
        verify(exactly = 1) {
            notificationManagerCompat.notify(any(), match { notification ->
                // Important messages should use important channel
                notification.channelId == SmartNotificationManager.CHANNEL_IMPORTANT
            })
        }
    }

    @Test
    fun `test notification respects user preferences`() = runTest {
        // Given - smart notifications disabled
        every { preferencesManager.userPreferences } returns flowOf(
            UserPreferences(enableSmartNotifications = false)
        )
        
        val message = SmsMessage(
            id = 222,
            sender = "+1234567890",
            content = "Test",
            timestamp = Date(),
            category = MessageCategory.INBOX,
            isRead = false,
            threadId = "+1234567890",
            isOutgoing = false
        )
        
        // When
        notificationManager.showSmartNotification(message)
        
        // Then - should fall back to basic notification
        verify(exactly = 1) {
            notificationManagerCompat.notify(any(), match { notification ->
                // Should use normal channel when smart notifications disabled
                notification.channelId == SmartNotificationManager.CHANNEL_NORMAL
            })
        }
    }
}