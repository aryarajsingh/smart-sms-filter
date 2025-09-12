package com.smartsmsfilter.domain.repository

import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.model.SenderPreferences
import com.smartsmsfilter.domain.common.Result
import kotlinx.coroutines.flow.Flow

interface SmsRepository {
    
    fun getAllMessages(): Flow<List<SmsMessage>>
    
    fun getMessagesByCategory(category: MessageCategory): Flow<List<SmsMessage>>
    
    suspend fun insertMessage(message: SmsMessage): Result<Long>
    
    suspend fun updateMessageCategory(messageId: Long, category: MessageCategory): Result<Unit>
    
    suspend fun markAsRead(messageId: Long): Result<Unit>
    
    suspend fun deleteMessage(messageId: Long): Result<Unit>
    
    // New message management methods
    suspend fun archiveMessage(messageId: Long): Result<Unit>
    
    suspend fun archiveMessages(messageIds: List<Long>): Result<Unit>
    
    suspend fun unarchiveMessage(messageId: Long): Result<Unit>
    
    suspend fun softDeleteMessage(messageId: Long): Result<Unit>
    
    suspend fun softDeleteMessages(messageIds: List<Long>): Result<Unit>
    
    suspend fun moveToCategory(messageId: Long, category: MessageCategory): Result<Unit>
    
    suspend fun moveToCategoryBatch(messageIds: List<Long>, category: MessageCategory): Result<Unit>
    
    suspend fun markAsImportant(messageId: Long, isImportant: Boolean): Result<Unit>
    
    fun getArchivedMessages(): Flow<List<SmsMessage>>
    
    fun getAllActiveMessages(): Flow<List<SmsMessage>>
    
    suspend fun getUnreadCount(): Int
    
    suspend fun getUnreadCountByCategory(category: MessageCategory): Int
    
    fun getMessagesByAddress(address: String): Flow<List<SmsMessage>>

    // Sender preferences (for learning and trust)
    suspend fun getSenderPreferences(sender: String): SenderPreferences?
    suspend fun setSenderPinnedToInbox(sender: String, pinned: Boolean): Result<Unit>
    suspend fun updateSenderReputation(sender: String, importanceScore: Float? = null, spamScore: Float? = null): Result<Unit>

    // Classification audit
    suspend fun insertClassificationAudit(messageId: Long?, classifier: MessageCategory, category: MessageCategory, confidence: Float, reasons: List<String>): Result<Unit>
    suspend fun insertUserFeedbackAudit(messageId: Long, targetCategory: MessageCategory, reasons: List<String>): Result<Unit>
    suspend fun getLatestClassificationReason(messageId: Long): String?

    // Message lookups
    suspend fun getMessageById(messageId: Long): SmsMessage?

    // Undo support
    suspend fun restoreSoftDeletedMessages(messageIds: List<Long>): Result<Unit>
}
