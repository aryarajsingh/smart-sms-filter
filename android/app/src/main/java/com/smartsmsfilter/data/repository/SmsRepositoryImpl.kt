package com.smartsmsfilter.data.repository

import com.smartsmsfilter.data.database.SmsMessageDao
import com.smartsmsfilter.data.model.toDomain
import com.smartsmsfilter.data.model.toEntity
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.repository.SmsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsRepositoryImpl @Inject constructor(
    private val dao: SmsMessageDao
) : SmsRepository {
    
    override fun getAllMessages(): Flow<List<SmsMessage>> {
        return dao.getAllMessages().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getMessagesByCategory(category: MessageCategory): Flow<List<SmsMessage>> {
        return dao.getMessagesByCategory(category.name).map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun insertMessage(message: SmsMessage): Long {
        // Check for duplicates to avoid spam
        val isDuplicate = dao.isDuplicate(
            sender = message.sender,
            content = message.content,
            timestamp = message.timestamp.time
        ) > 0
        
        return if (!isDuplicate) {
            dao.insertMessage(message.toEntity())
        } else {
            -1L // Indicate duplicate
        }
    }
    
    override suspend fun updateMessageCategory(messageId: Long, category: MessageCategory) {
        dao.updateMessageCategory(messageId, category.name)
    }
    
    override suspend fun markAsRead(messageId: Long) {
        dao.markAsRead(messageId)
    }
    
    override suspend fun deleteMessage(messageId: Long) {
        dao.deleteMessageById(messageId)
    }
    
    override suspend fun getUnreadCount(): Int {
        return dao.getUnreadCount()
    }
    
    override suspend fun getUnreadCountByCategory(category: MessageCategory): Int {
        return dao.getUnreadCountByCategory(category.name)
    }
    
    override fun getMessagesByAddress(address: String): Flow<List<SmsMessage>> {
        return dao.getMessagesByAddress(address).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
