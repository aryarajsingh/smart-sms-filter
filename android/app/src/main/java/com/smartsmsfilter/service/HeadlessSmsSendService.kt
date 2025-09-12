package com.smartsmsfilter.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.provider.Telephony
import android.telephony.SmsManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build

class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "HeadlessSmsSendService started with action: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_SEND_SMS -> {
                handleSendSms(intent)
            }
            else -> {
                Log.w(TAG, "Unknown action received: ${intent?.action}")
            }
        }
        
        // Stop the service after handling the request
        stopSelf(startId)
        return START_NOT_STICKY
    }
    
    private fun handleSendSms(intent: Intent) {
        try {
            val phoneNumber = intent.getStringExtra("phone_number")
            val message = intent.getStringExtra("message")
            
            if (phoneNumber == null || message == null) {
                Log.e(TAG, "Missing phone number or message")
                return
            }
            
            Log.d(TAG, "Sending SMS to $phoneNumber: $message")
            
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                this.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            
            // Create pending intents for delivery and sent status (optional)
            val sentPI = PendingIntent.getBroadcast(
                this, 0, Intent("SMS_SENT"), 
                PendingIntent.FLAG_UPDATE_CURRENT or 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )
            
            val deliverPI = PendingIntent.getBroadcast(
                this, 0, Intent("SMS_DELIVERED"), 
                PendingIntent.FLAG_UPDATE_CURRENT or 
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            )
            
            // Send the SMS
            smsManager.sendTextMessage(
                phoneNumber,
                null,
                message,
                sentPI,
                deliverPI
            )
            
            Log.d(TAG, "SMS sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending SMS", e)
        }
    }
    
    companion object {
        private const val TAG = "HeadlessSmsSendService"
        const val ACTION_SEND_SMS = "com.smartsmsfilter.SEND_SMS"
        
        fun createSendSmsIntent(context: Context, phoneNumber: String, message: String): Intent {
            return Intent(context, HeadlessSmsSendService::class.java).apply {
                action = ACTION_SEND_SMS
                putExtra("phone_number", phoneNumber)
                putExtra("message", message)
            }
        }
    }
}

