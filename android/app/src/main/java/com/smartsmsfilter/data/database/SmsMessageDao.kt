package com.smartsmsfilter.data.database

import androidx.room.*
import com.smartsmsfilter.data.model.SmsMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsMessageDao {
    
    @Query("SELECT * FROM sms_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<SmsMessageEntity>>
    
    @Query("SELECT * FROM sms_messages WHERE category = :category ORDER BY timestamp DESC")
    fun getMessagesByCategory(category: String): Flow<List<SmsMessageEntity>>
    
    @Query("SELECT * FROM sms_messages WHERE id = :messageId")
    suspend fun getMessageById(messageId: Long): SmsMessageEntity?
    
    @Insert
    suspend fun insertMessage(message: SmsMessageEntity): Long
    
    @Update
    suspend fun updateMessage(message: SmsMessageEntity)
    
    @Query("UPDATE sms_messages SET category = :category WHERE id = :messageId")
    suspend fun updateMessageCategory(messageId: Long, category: String)
    
    @Query("UPDATE sms_messages SET isRead = 1 WHERE id = :messageId")
    suspend fun markAsRead(messageId: Long)
    
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
    
    @Query("SELECT * FROM sms_messages WHERE sender = :address ORDER BY timestamp DESC")
    fun getMessagesByAddress(address: String): Flow<List<SmsMessageEntity>>
}
