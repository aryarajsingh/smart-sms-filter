package com.smartsmsfilter.data.database

import androidx.room.*
import com.smartsmsfilter.data.model.SmsMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the `sms_messages` table.
 * Provides methods for querying and modifying SMS message data in the local database.
 */
@Dao
interface SmsMessageDao {
    
    /** Fetches all messages, sorted by timestamp descending. */
    @Query("SELECT * FROM sms_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<SmsMessageEntity>>
    
    /** Fetches all active (not deleted or archived) messages for a given category. */
    @Query("SELECT * FROM sms_messages WHERE category = :category AND isDeleted = 0 AND isArchived = 0 ORDER BY timestamp DESC")
    fun getMessagesByCategory(category: String): Flow<List<SmsMessageEntity>>
    
    /** Fetches all archived messages. */
    @Query("SELECT * FROM sms_messages WHERE isArchived = 1 AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getArchivedMessages(): Flow<List<SmsMessageEntity>>
    
    /** Fetches all messages that are not soft-deleted. */
    @Query("SELECT * FROM sms_messages WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllActiveMessages(): Flow<List<SmsMessageEntity>>
    
    @Query("SELECT * FROM sms_messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: Long): SmsMessageEntity?
    
    /** Inserts a single message. If the message already exists, it is replaced. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: SmsMessageEntity): Long
    
    @Update
    suspend fun updateMessage(message: SmsMessageEntity)
    
    @Query("UPDATE sms_messages SET category = :category WHERE id = :messageId")
    suspend fun updateMessageCategory(messageId: Long, category: String)
    
    @Query("UPDATE sms_messages SET isRead = 1 WHERE id = :messageId")
    suspend fun markAsRead(messageId: Long)
    
    @Query("UPDATE sms_messages SET isArchived = 1, lastModified = :timestamp WHERE id = :messageId")
    suspend fun archiveMessage(messageId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE sms_messages SET isArchived = 0, lastModified = :timestamp WHERE id = :messageId")
    suspend fun unarchiveMessage(messageId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE sms_messages SET isDeleted = 1, deletedAt = :timestamp, lastModified = :timestamp WHERE id = :messageId")
    suspend fun softDeleteMessage(messageId: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE sms_messages SET isImportant = :isImportant, lastModified = :timestamp WHERE id = :messageId")
    suspend fun markAsImportant(messageId: Long, isImportant: Boolean, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE sms_messages SET category = :category, manualCategoryOverride = :category, lastModified = :timestamp WHERE id = :messageId")
    suspend fun moveToCategory(messageId: Long, category: String, timestamp: Long = System.currentTimeMillis())
    
    // Batch operations
    @Query("UPDATE sms_messages SET isArchived = 1, lastModified = :timestamp WHERE id IN (:messageIds)")
    suspend fun archiveMessages(messageIds: List<Long>, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE sms_messages SET isDeleted = 1, deletedAt = :timestamp, lastModified = :timestamp WHERE id IN (:messageIds)")
    suspend fun softDeleteMessages(messageIds: List<Long>, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE sms_messages SET isDeleted = 0, deletedAt = NULL, lastModified = :timestamp WHERE id IN (:messageIds)")
    suspend fun restoreSoftDeletedMessages(messageIds: List<Long>, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE sms_messages SET category = :category, manualCategoryOverride = :category, lastModified = :timestamp WHERE id IN (:messageIds)")
    suspend fun moveToCategoryBatch(messageIds: List<Long>, category: String, timestamp: Long = System.currentTimeMillis())
    
    @Delete
    suspend fun deleteMessage(message: SmsMessageEntity)
    
    @Query("DELETE FROM sms_messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: Long)
    
    @Query("SELECT COUNT(*) FROM sms_messages WHERE isRead = 0")
    suspend fun getUnreadCount(): Int
    
    @Query("SELECT COUNT(*) FROM sms_messages WHERE isRead = 0 AND category = :category")
    suspend fun getUnreadCountByCategory(category: String): Int
    
    @Query("SELECT COUNT(*) FROM sms_messages WHERE sender = :sender AND content = :content AND timestamp = :timestamp")
    suspend fun isDuplicate(sender: String, content: String, timestamp: Long): Int

    @Query("SELECT COUNT(*) FROM sms_messages WHERE sender = :sender AND content = :content AND ABS(timestamp - :timestamp) <= :windowMs")
    suspend fun countNearDuplicate(sender: String, content: String, timestamp: Long, windowMs: Long = 60000): Int
    
    /**
     * Fetches all messages in a conversation thread, identified by the sender's normalized address.
     * This includes both incoming and outgoing messages.
     * @param address The normalized phone number of the conversation partner.
     */
    @Query("SELECT * FROM sms_messages WHERE (sender = :address OR threadId = :address) AND isDeleted = 0 ORDER BY timestamp ASC")
    fun getMessagesByAddress(address: String): Flow<List<SmsMessageEntity>>
}
