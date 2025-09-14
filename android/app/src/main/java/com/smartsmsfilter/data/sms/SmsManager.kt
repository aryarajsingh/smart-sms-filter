package com.smartsmsfilter.data.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class SmsSenderManager @Inject constructor(
    private val context: Context,
    private val rateLimiter: com.smartsmsfilter.security.RateLimiter
) {
    
    companion object {
        private const val TAG = "SmsSenderManager"
        private const val SENT_SMS_ACTION = "SMS_SENT"
        private const val DELIVERED_SMS_ACTION = "SMS_DELIVERED"
        private const val MAX_SMS_LENGTH = 160 // Standard SMS length
    }
    
    private val smsManager = SmsManager.getDefault()
    
    /**
     * Sends an SMS message to a recipient
     * Automatically handles multi-part messages for long content
     */
    suspend fun sendSms(
        phoneNumber: String, 
        message: String
    ): Result<SmsDeliveryStatus> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Sending SMS to $phoneNumber")
            
            // Validate inputs
            if (phoneNumber.isBlank() || message.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Phone number and message cannot be empty"))
            }
            
            // Check rate limits
            val (canSend, errorMessage) = rateLimiter.canSendSms(phoneNumber)
            if (!canSend) {
                return@withContext Result.failure(IllegalStateException(errorMessage ?: "Rate limit exceeded"))
            }
            
            // Create pending intents for delivery tracking
            val sentIntent = PendingIntent.getBroadcast(
                context, 
                0, 
                Intent(SENT_SMS_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val deliveredIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(DELIVERED_SMS_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Check if message needs to be split into multiple parts
            if (message.length <= MAX_SMS_LENGTH) {
                // Single SMS
                smsManager.sendTextMessage(
                    phoneNumber,
                    null,
                    message,
                    sentIntent,
                    deliveredIntent
                )
                Log.d(TAG, "Single SMS sent")
            } else {
                // Multi-part SMS
                val parts = smsManager.divideMessage(message)
                val sentIntents = arrayListOf<PendingIntent>()
                val deliveredIntents = arrayListOf<PendingIntent>()
                
                // Create intents for each part
                repeat(parts.size) {
                    sentIntents.add(sentIntent)
                    deliveredIntents.add(deliveredIntent)
                }
                
                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    parts,
                    sentIntents,
                    deliveredIntents
                )
                Log.d(TAG, "Multi-part SMS sent (${parts.size} parts)")
            }
            
            // Record the SMS for rate limiting
            rateLimiter.recordSmsSent(phoneNumber)
            
            // Return success - in a real app, you'd wait for the broadcast receiver
            Result.success(SmsDeliveryStatus.SENT)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            Result.failure(e)
        }
    }
    
    /**
     * Calculates SMS parts and character count for UI display
     */
    fun calculateSmsInfo(message: String): SmsInfo {
        val parts = if (message.length <= MAX_SMS_LENGTH) {
            1
        } else {
            smsManager.divideMessage(message).size
        }
        
        val remainingChars = if (message.length <= MAX_SMS_LENGTH) {
            MAX_SMS_LENGTH - message.length
        } else {
            val lastPartLength = message.length % MAX_SMS_LENGTH
            if (lastPartLength == 0) MAX_SMS_LENGTH else MAX_SMS_LENGTH - lastPartLength
        }
        
        return SmsInfo(
            messageLength = message.length,
            partCount = parts,
            remainingCharacters = remainingChars,
            willSendAsMultipart = parts > 1
        )
    }
    
    /**
     * Validates phone number format
     */
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        return phoneNumber.matches(Regex("^[+]?[0-9]{10,15}$"))
    }
}

/**
 * SMS delivery status tracking
 */
enum class SmsDeliveryStatus {
    SENDING,
    SENT,
    DELIVERED,
    FAILED
}

/**
 * Information about SMS message characteristics
 */
data class SmsInfo(
    val messageLength: Int,
    val partCount: Int,
    val remainingCharacters: Int,
    val willSendAsMultipart: Boolean
)
