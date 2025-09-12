package com.smartsmsfilter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.smartsmsfilter.domain.model.SmsMessage as DomainSmsMessage
import com.smartsmsfilter.domain.repository.SmsRepository
import com.smartsmsfilter.domain.common.CoroutineScopeManager
import com.smartsmsfilter.domain.common.AppException
import com.smartsmsfilter.domain.validation.validatePhoneNumber
import com.smartsmsfilter.domain.validation.validateMessage
import com.smartsmsfilter.domain.classifier.ClassificationService
import com.smartsmsfilter.services.SmartNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Date
import com.smartsmsfilter.utils.DefaultSmsAppHelper
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var smsRepository: SmsRepository
    
    @Inject 
    lateinit var scopeManager: CoroutineScopeManager
    
    @Inject
    lateinit var classificationService: ClassificationService
    
    @Inject
    lateinit var notificationManager: SmartNotificationManager
    
    // Note: Don't create a global scope - use scopeManager instead
    
    companion object {
        private const val TAG = "SmsReceiver"
    }
    /**
     * This BroadcastReceiver is responsible for intercepting incoming SMS messages.
     * It listens for two intents: `SMS_DELIVER_ACTION` and `SMS_RECEIVED_ACTION`.
     *
     * To avoid duplicate notifications, it uses the `DefaultSmsAppHelper` to check
     * if the app is the default SMS app. If it is, it only processes the
     * `SMS_DELIVER_ACTION`. If it's not the default app, it processes the
     * `SMS_RECEIVED_ACTION` and aborts the broadcast to prevent the system's
     * default SMS app from also processing the message.
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.w(TAG, "Received null context or intent")
            return
        }

        val isDefaultSmsApp = DefaultSmsAppHelper.isDefaultSmsApp(context)
        val action = intent.action

        val shouldProcess = when {
            isDefaultSmsApp && action == Telephony.Sms.Intents.SMS_DELIVER_ACTION -> {
                Log.d(TAG, "Processing as default SMS app")
                true
            }
            !isDefaultSmsApp && action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION -> {
                Log.d(TAG, "Processing as non-default SMS app")
                true
            }
            else -> {
                Log.d(TAG, "Skipping SMS broadcast. isDefault: $isDefaultSmsApp, action: $action")
                false
            }
        }

        if (shouldProcess) {
            try {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

                if (messages.isNullOrEmpty()) {
                    Log.w(TAG, "No SMS messages found in intent")
                    return
                }

                // Process each message with proper error handling
                messages.forEach { smsMessage ->
                    processSmsMessageSafely(smsMessage)
                }

                if (!isDefaultSmsApp) {
                    abortBroadcast()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS intent", e)
            }
        }
    }
    
    private fun processSmsMessageSafely(smsMessage: SmsMessage) {
        // Use a controlled scope that will be cancelled properly
        scopeManager.launchSafely {
            try {
                val sender = smsMessage.originatingAddress ?: "Unknown"
                val content = smsMessage.messageBody ?: ""
                val timestamp = Date(smsMessage.timestampMillis)
                
                Log.d(TAG, "Processing SMS from $sender")
                
                // Validate input data
                val senderValidation = sender.validatePhoneNumber()
                val contentValidation = content.validateMessage()
                
                if (senderValidation.isError) {
                    Log.w(TAG, "Invalid sender phone number: $sender")
                    // Still process with original sender for logging
                }
                
                if (contentValidation.isError) {
                    Log.w(TAG, "Invalid message content")
                    return@launchSafely // Skip empty messages
                }
                
                // Prepare domain message (category will be finalized by service)
                val finalSender = senderValidation.getOrNull() ?: sender
                val finalContent = contentValidation.getOrNull() ?: content
                val normalizedSender = com.smartsmsfilter.ui.utils.normalizePhoneNumber(finalSender)
                val provisionalMessage = DomainSmsMessage(
                    sender = normalizedSender,
                    content = finalContent,
                    timestamp = timestamp,
                    category = com.smartsmsfilter.domain.model.MessageCategory.NEEDS_REVIEW,
                    isRead = false,
                    isOutgoing = false
                )

                // Classify and store via service (handles DB write)
                val classification = classificationService.classifyAndStore(provisionalMessage)
                Log.d(TAG, "Message classified as: ${classification.category} (conf=${classification.confidence})")

                // Notify user based on final category
                notificationManager.showSmartNotification(
                    provisionalMessage.copy(category = classification.category)
                )
                Log.d(TAG, "Notification sent for category: ${classification.category}")
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error processing SMS", e)
            }
        }
    }
}
