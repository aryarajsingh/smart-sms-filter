package com.smartsmsfilter.domain.repository

import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import kotlinx.coroutines.flow.Flow

interface SmsRepository {
    
    fun getAllMessages(): Flow<List<SmsMessage>>
    
    fun getMessagesByCategory(category: MessageCategory): Flow<List<SmsMessage>>
    
    suspend fun insertMessage(message: SmsMessage): Long
    
    suspend fun updateMessageCategory(messageId: Long, category: MessageCategory)
    
    suspend fun markAsRead(messageId: Long)
    
    suspend fun deleteMessage(messageId: Long)
    
    suspend fun getUnreadCount(): Int
    
    suspend fun getUnreadCountByCategory(category: MessageCategory): Int
    
    fun getMessagesByAddress(address: String): Flow<List<SmsMessage>>
}
