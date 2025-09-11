package com.smartsmsfilter.data.sms

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.classification.SimpleMessageClassifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsReader @Inject constructor(
    private val context: Context,
    private val classifier: SimpleMessageClassifier
) {
    
    companion object {
        private const val TAG = "SmsReader"
    }
    
    /**
     * Load all SMS messages from device
     */
    fun loadAllSmsMessages(): Flow<List<SmsMessage>> = flow {
        val messages = mutableListOf<SmsMessage>()
        
        try {
            // Query both inbox and sent messages
            val inboxMessages = loadMessagesFromUri(Telephony.Sms.Inbox.CONTENT_URI, false)
            val sentMessages = loadMessagesFromUri(Telephony.Sms.Sent.CONTENT_URI, true)
            
            messages.addAll(inboxMessages)
            messages.addAll(sentMessages)
            
            // Sort by timestamp (newest first)
            messages.sortByDescending { it.timestamp }
            
            Log.d(TAG, "Loaded ${messages.size} SMS messages from device")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load SMS messages", e)
        }
        
        emit(messages)
    }.flowOn(Dispatchers.IO)
    
    /**
     * Load recent SMS messages (last 100)
     */
    fun loadRecentSmsMessages(limit: Int = 100): Flow<List<SmsMessage>> = flow {
        val messages = mutableListOf<SmsMessage>()
        
        try {
            // Load recent inbox messages
            val inboxMessages = loadMessagesFromUri(
                Telephony.Sms.Inbox.CONTENT_URI, 
                isOutgoing = false, 
                limit = limit/2
            )
            
            // Load recent sent messages
            val sentMessages = loadMessagesFromUri(
                Telephony.Sms.Sent.CONTENT_URI, 
                isOutgoing = true, 
                limit = limit/2
            )
            
            messages.addAll(inboxMessages)
            messages.addAll(sentMessages)
            
            // Sort by timestamp and limit
            messages.sortByDescending { it.timestamp }
            
            Log.d(TAG, "Loaded ${messages.size} recent SMS messages")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load recent SMS messages", e)
        }
        
        emit(messages.take(limit))
    }.flowOn(Dispatchers.IO)
    
    private fun loadMessagesFromUri(
        uri: Uri, 
        isOutgoing: Boolean, 
        limit: Int = 1000
    ): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        
        try {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.READ,
                    Telephony.Sms.THREAD_ID
                ),
                null,
                null,
                "${Telephony.Sms.DATE} DESC LIMIT $limit"
            )
            
            cursor?.use { c ->
                val idIndex = c.getColumnIndex(Telephony.Sms._ID)
                val addressIndex = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val bodyIndex = c.getColumnIndex(Telephony.Sms.BODY)
                val dateIndex = c.getColumnIndex(Telephony.Sms.DATE)
                val readIndex = c.getColumnIndex(Telephony.Sms.READ)
                val threadIndex = c.getColumnIndex(Telephony.Sms.THREAD_ID)
                
                while (c.moveToNext()) {
                    val id = c.getLong(idIndex)
                    val address = c.getString(addressIndex) ?: "Unknown"
                    val body = c.getString(bodyIndex) ?: ""
                    val date = c.getLong(dateIndex)
                    val isRead = c.getInt(readIndex) == 1
                    val threadId = c.getString(threadIndex)
                    
                    val sender = if (isOutgoing) "You" else address
                    
                    // Classify the message using simple rules
                    val category = if (isOutgoing) {
                        MessageCategory.INBOX // Sent messages go to inbox
                    } else {
                        classifier.classifyMessage(sender, body)
                    }
                    
                    messages.add(
                        SmsMessage(
                            id = id,
                            sender = sender,
                            content = body,
                            timestamp = Date(date),
                            category = category,
                            isRead = isRead,
                            threadId = threadId
                        )
                    )
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load messages from $uri", e)
        }
        
        return messages
    }
}
