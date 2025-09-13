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
     * Load all SMS messages from device for initial sync only.
     * This is used ONLY for the first-time load to populate the database.
     * After that, messages come through SmsReceiver which properly classifies them.
     */
    fun loadAllSmsMessages(): Flow<List<SmsMessage>> = flow {
        val messages = mutableListOf<SmsMessage>()
        
        try {
            // Query from multiple URIs to ensure we get ALL messages
            // 1. Get all messages from the main content URI
            Log.d(TAG, "Loading messages from main SMS URI for initial sync...")
            val mainMessages = loadMessagesFromUri(Telephony.Sms.CONTENT_URI, false)
            messages.addAll(mainMessages)
            Log.d(TAG, "Loaded ${mainMessages.size} messages from main URI")
            
            // 2. Also query inbox specifically to ensure we don't miss any
            Log.d(TAG, "Loading messages from inbox URI...")
            val inboxMessages = loadMessagesFromUri(Telephony.Sms.Inbox.CONTENT_URI, false)
            val existingIds = messages.map { it.id }.toSet()
            val newInboxMessages = inboxMessages.filter { it.id !in existingIds }
            messages.addAll(newInboxMessages)
            Log.d(TAG, "Added ${newInboxMessages.size} new messages from inbox")
            
            // 3. Query sent messages
            Log.d(TAG, "Loading sent messages...")
            val sentMessages = loadMessagesFromUri(Telephony.Sms.Sent.CONTENT_URI, true)
            val newSentMessages = sentMessages.filter { it.id !in messages.map { it.id }.toSet() }
            messages.addAll(newSentMessages)
            Log.d(TAG, "Added ${newSentMessages.size} sent messages")
            
            // Sort all messages by timestamp
            messages.sortByDescending { it.timestamp }
            
            Log.d(TAG, "Total loaded: ${messages.size} SMS messages from device for initial sync")
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
            // Load ALL messages then take recent ones
            val allMessages = loadMessagesFromUri(
                Telephony.Sms.CONTENT_URI,
                isOutgoing = false
            )
            
            messages.addAll(allMessages)
            
            // Sort by timestamp (newest first) and take requested limit
            messages.sortByDescending { it.timestamp }
            
            Log.d(TAG, "Loaded ${messages.take(limit).size} recent SMS messages from ${messages.size} total")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load recent SMS messages", e)
        }
        
        emit(messages.take(limit))
    }.flowOn(Dispatchers.IO)
    
    private suspend fun loadMessagesFromUri(
        uri: Uri, 
        isOutgoing: Boolean
    ): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        
        // Check for permission before accessing SMS
        if (context.checkSelfPermission(android.Manifest.permission.READ_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_SMS permission not granted")
            return messages
        }
        
        try {
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(
                    Telephony.Sms._ID,
                    Telephony.Sms.ADDRESS,
                    Telephony.Sms.BODY,
                    Telephony.Sms.DATE,
                    Telephony.Sms.READ,
                    Telephony.Sms.THREAD_ID,
                    Telephony.Sms.TYPE
                ),
                null,  // No selection - get ALL messages
                null,  // No selection args
                "${Telephony.Sms.DATE} DESC"  // Order by date descending
            )
            
            cursor?.use { c ->
                val idIndex = c.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressIndex = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
                val readIndex = c.getColumnIndexOrThrow(Telephony.Sms.READ)
                val threadIndex = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
                val typeIndex = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
                
                var count = 0
                Log.d(TAG, "Cursor has ${c.count} messages from $uri")
                
                while (c.moveToNext()) {
                    count++
                    if (count % 100 == 0) {
                        Log.d(TAG, "Processing message $count of ${c.count}")
                    }
                    val id = if (idIndex >= 0 && !c.isNull(idIndex)) c.getLong(idIndex) else 0L
                    val address = if (addressIndex >= 0 && !c.isNull(addressIndex)) c.getString(addressIndex) else "Unknown"
                    val body = if (bodyIndex >= 0 && !c.isNull(bodyIndex)) c.getString(bodyIndex) else ""
                    val date = if (dateIndex >= 0 && !c.isNull(dateIndex)) c.getLong(dateIndex) else System.currentTimeMillis()
                    val isRead = if (readIndex >= 0 && !c.isNull(readIndex)) c.getInt(readIndex) == 1 else false
                    val threadId = if (threadIndex >= 0 && !c.isNull(threadIndex)) c.getString(threadIndex) else null
                    val type = if (typeIndex >= 0 && !c.isNull(typeIndex)) c.getInt(typeIndex) else Telephony.Sms.MESSAGE_TYPE_INBOX
                    
                    // Normalize the address for consistency
                    val normalizedAddress = normalizePhoneNumber(address)
                    
                    // Determine if message is outgoing based on TYPE column
                    val actualIsOutgoing = type == Telephony.Sms.MESSAGE_TYPE_SENT || 
                                          type == Telephony.Sms.MESSAGE_TYPE_OUTBOX ||
                                          type == Telephony.Sms.MESSAGE_TYPE_QUEUED
                    
                    // For initial sync, use NEEDS_REVIEW category
                    // Real classification happens through SmsReceiver and ClassificationService
                    // This prevents overriding already classified messages
                    val category = if (actualIsOutgoing) {
                        MessageCategory.INBOX // Sent messages go to inbox
                    } else {
                        // Don't classify here - let ClassificationService handle it
                        // Use NEEDS_REVIEW as default for initial load
                        MessageCategory.NEEDS_REVIEW
                    }
                    
                    messages.add(
                        SmsMessage(
                            id = id,
                            sender = normalizedAddress,
                            content = body,
                            timestamp = Date(date),
                            category = category,
                            isRead = isRead,
                            threadId = normalizedAddress, // Use normalized address as threadId for consistency
                            isOutgoing = actualIsOutgoing
                        )
                    )
                }
                Log.d(TAG, "Processed $count messages from $uri")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load messages from $uri", e)
        }
        
        return messages
    }
    
    /**
     * Normalizes phone number for consistent comparison across the whole app.
     * Delegates to the shared UI utils normalizer so DB, grouping and receiver all agree.
     */
    private fun normalizePhoneNumber(phoneNumber: String): String {
        return com.smartsmsfilter.ui.utils.normalizePhoneNumber(phoneNumber)
    }
}
