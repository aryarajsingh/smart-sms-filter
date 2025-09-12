package com.smartsmsfilter.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.smartsmsfilter.MainActivity
import com.smartsmsfilter.data.preferences.PreferencesManager
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        // Notification Channels
        const val CHANNEL_IMPORTANT = "important_messages"
        const val CHANNEL_NORMAL = "normal_messages"
        const val CHANNEL_SILENT = "silent_messages"
        const val CHANNEL_NEEDS_REVIEW = "needs_review_messages"
        
        // Notification IDs
        private var notificationId = 1000
        
        // Channel priorities
        const val PRIORITY_IMPORTANT = NotificationCompat.PRIORITY_HIGH
        const val PRIORITY_NORMAL = NotificationCompat.PRIORITY_DEFAULT
        const val PRIORITY_SILENT = NotificationCompat.PRIORITY_LOW
    }
    
    init {
        createNotificationChannels()
    }
    
    /**
     * Create notification channels for different message categories
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Important Messages Channel (OTPs, Banking, etc.)
            val importantChannel = NotificationChannel(
                CHANNEL_IMPORTANT,
                "Important Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical messages like OTPs, banking alerts, and important notifications"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            
            // Normal Messages Channel
            val normalChannel = NotificationChannel(
                CHANNEL_NORMAL,
                "Normal Messages",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Regular messages and notifications"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            
            // Silent/Spam Channel
            val silentChannel = NotificationChannel(
                CHANNEL_SILENT,
                "Promotional & Spam",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Promotional messages and spam - delivered silently"
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }
            
            // Needs Review Channel
            val reviewChannel = NotificationChannel(
                CHANNEL_NEEDS_REVIEW,
                "Messages Needing Review",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Messages that need manual review for categorization"
                enableLights(true)
                enableVibration(false)
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannels(
                listOf(importantChannel, normalChannel, silentChannel, reviewChannel)
            )
        }
    }
    
    /**
     * Show smart notification based on message category and user preferences
     */
    fun showSmartNotification(message: SmsMessage) {
        android.util.Log.d("SmartNotificationManager", "showSmartNotification called for message: ${message.sender}")
        
        val preferences = runBlocking { preferencesManager.userPreferences.first() }
        android.util.Log.d("SmartNotificationManager", "Smart notifications enabled: ${preferences.enableSmartNotifications}")
        
        // Check if smart notifications are enabled
        if (!preferences.enableSmartNotifications) {
            android.util.Log.d("SmartNotificationManager", "Smart notifications disabled, falling back to basic")
            // Fall back to normal notification
            showBasicNotification(message)
            return
        }
        
        // Determine notification channel and priority based on category
        val (channelId, priority, shouldVibrate, shouldSound) = when (message.category) {
            MessageCategory.INBOX -> {
                // Important messages get high priority; OTPs are never silent
                if (isImportantMessage(message)) {
                    NotificationParams(CHANNEL_IMPORTANT, PRIORITY_IMPORTANT, true, true)
                } else {
                    NotificationParams(CHANNEL_NORMAL, PRIORITY_NORMAL, true, true)
                }
            }
            MessageCategory.SPAM -> {
                // Spam gets silent treatment
                NotificationParams(CHANNEL_SILENT, PRIORITY_SILENT, false, false)
            }
            MessageCategory.NEEDS_REVIEW -> {
                // Needs review gets low priority with visual indication
                NotificationParams(CHANNEL_NEEDS_REVIEW, PRIORITY_SILENT, false, false)
            }
        }
        
        // Build and show notification
        showNotification(
            message = message,
            channelId = channelId,
            priority = priority,
            vibrate = shouldVibrate,
            sound = shouldSound
        )
    }
    
    /**
     * Check if a message is considered important (OTP, Banking, etc.)
     */
    private fun isImportantMessage(message: SmsMessage): Boolean {
        val content = message.content.lowercase()
        val sender = message.sender.uppercase()
        
        // Check for OTP patterns
        if (content.contains("otp") || content.contains("verification") || 
            content.contains("code") || content.matches(Regex(".*\\b\\d{4,8}\\b.*"))) {
            return true
        }
        
        // Check for banking patterns
        if (sender.contains("BANK") || sender.contains("INB") || 
            content.contains("credited") || content.contains("debited") ||
            content.contains("transaction")) {
            return true
        }
        
        // Check if marked as important
        return message.isImportant
    }
    
    /**
     * Show a basic notification (fallback when smart notifications are disabled)
     */
    private fun showBasicNotification(message: SmsMessage) {
        showNotification(
            message = message,
            channelId = CHANNEL_NORMAL,
            priority = PRIORITY_NORMAL,
            vibrate = true,
            sound = true
        )
    }
    
    /**
     * Actually display the notification
     */
    private fun showNotification(
        message: SmsMessage,
        channelId: String,
        priority: Int,
        vibrate: Boolean,
        sound: Boolean
    ) {
        // Create intent to open app when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("message_id", message.id)
            putExtra("from_notification", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            message.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Determine notification title and content
        val (title, content) = getNotificationContent(message)
        
        // Build notification
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(getNotificationIcon(message.category))
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.content))
            .setPriority(priority)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setGroup(message.category.name)
            .setWhen(message.timestamp.time)
        
        // Add category-specific styling
        when (message.category) {
            MessageCategory.INBOX -> {
                if (isImportantMessage(message)) {
                    builder.setColor(context.getColor(android.R.color.holo_red_dark))
                    builder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
                }
            }
            MessageCategory.SPAM -> {
                builder.setColor(context.getColor(android.R.color.darker_gray))
                builder.setCategory(NotificationCompat.CATEGORY_PROMO)
            }
            MessageCategory.NEEDS_REVIEW -> {
                builder.setColor(context.getColor(android.R.color.holo_orange_dark))
                builder.setCategory(NotificationCompat.CATEGORY_STATUS)
                builder.addAction(
                    android.R.drawable.ic_menu_view,
                    "Review",
                    pendingIntent
                )
            }
        }
        
        // Control vibration and sound
        if (!vibrate) {
            builder.setVibrate(longArrayOf(0))
        }
        if (!sound) {
            builder.setSilent(true)
        }
        
        // Check if notifications are enabled
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            android.util.Log.w("SmartNotificationManager", "Notifications are disabled for this app")
            return
        }
        
        // Show the notification
        android.util.Log.d("SmartNotificationManager", "Attempting to show notification for ${message.category} message")
        with(NotificationManagerCompat.from(context)) {
            try {
                val notification = builder.build()
                android.util.Log.d("SmartNotificationManager", "Notification built successfully, showing with ID: $notificationId")
                notify(notificationId++, notification)
                android.util.Log.d("SmartNotificationManager", "Notification shown successfully")
            } catch (e: SecurityException) {
                android.util.Log.e("SmartNotificationManager", "SecurityException showing notification", e)
                // Handle missing notification permission
                e.printStackTrace()
            } catch (e: Exception) {
                android.util.Log.e("SmartNotificationManager", "Exception showing notification", e)
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Get appropriate notification content based on message category
     */
    private fun getNotificationContent(message: SmsMessage): Pair<String, String> {
        return when (message.category) {
            MessageCategory.INBOX -> {
                if (isImportantMessage(message)) {
                    Pair("🔴 Important: ${message.sender}", getTruncatedContent(message.content, 100))
                } else {
                    Pair(message.sender, getTruncatedContent(message.content, 100))
                }
            }
            MessageCategory.SPAM -> {
                Pair("📧 Promotional: ${message.sender}", getTruncatedContent(message.content, 80))
            }
            MessageCategory.NEEDS_REVIEW -> {
                Pair("⚠️ Review Required: ${message.sender}", "Tap to categorize this message")
            }
        }
    }
    
    /**
     * Get appropriate icon for notification based on category
     */
    private fun getNotificationIcon(category: MessageCategory): Int {
        return when (category) {
            MessageCategory.INBOX -> android.R.drawable.ic_dialog_email
            MessageCategory.SPAM -> android.R.drawable.ic_dialog_info
            MessageCategory.NEEDS_REVIEW -> android.R.drawable.ic_dialog_alert
        }
    }
    
    /**
     * Truncate content for notification preview
     */
    private fun getTruncatedContent(content: String, maxLength: Int): String {
        return if (content.length > maxLength) {
            "${content.take(maxLength)}..."
        } else {
            content
        }
    }
    
    /**
     * Clear all notifications from a specific category
     */
    fun clearCategoryNotifications(category: MessageCategory) {
        val notificationManager = NotificationManagerCompat.from(context)
        // In a real implementation, we'd track notification IDs by category
        // For now, this is a placeholder
    }
    
    /**
     * Update notification settings when preferences change
     */
    fun updateNotificationSettings() {
        // Re-create channels with updated settings if needed
        createNotificationChannels()
    }
    
    // Helper data class for notification parameters
    private data class NotificationParams(
        val channelId: String,
        val priority: Int, 
        val vibrate: Boolean,
        val sound: Boolean
    )
}
