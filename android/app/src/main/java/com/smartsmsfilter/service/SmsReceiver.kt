package com.smartsmsfilter.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.smartsmsfilter.domain.model.SmsMessage as DomainSmsMessage
import com.smartsmsfilter.domain.repository.SmsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var smsRepository: SmsRepository
    
    private val scope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val TAG = "SmsReceiver"
    }
    
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "SMS received!")
        
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            messages?.forEach { smsMessage ->
                processSmsMessage(smsMessage)
            }
        }
    }
    
    private fun processSmsMessage(smsMessage: SmsMessage) {
        val sender = smsMessage.originatingAddress ?: "Unknown"
        val content = smsMessage.messageBody ?: ""
        val timestamp = Date(smsMessage.timestampMillis)
        
        Log.d(TAG, "Processing SMS from $sender: $content")
        
        // Create domain message
        val domainMessage = DomainSmsMessage(
            sender = sender,
            content = content,
            timestamp = timestamp,
            category = com.smartsmsfilter.domain.model.MessageCategory.NEEDS_REVIEW, // Will be classified later
            isRead = false
        )
        
        // Save to database
        scope.launch {
            try {
                val messageId = smsRepository.insertMessage(domainMessage)
                Log.d(TAG, "SMS saved with ID: $messageId")
                
                // TODO: Start classification service
                // ClassificationService.classifyMessage(messageId)
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save SMS", e)
            }
        }
    }
}
