package com.smartsmsfilter.domain.usecase

import com.smartsmsfilter.domain.model.StarredMessage
import com.smartsmsfilter.domain.model.StarredSenderGroup
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.repository.SmsRepository
import com.smartsmsfilter.data.contacts.ContactManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToggleMessageStarUseCase @Inject constructor(
    private val repository: SmsRepository,
    private val contactManager: ContactManager
) {
    
    /**
     * Star or unstar a specific message
     */
    suspend operator fun invoke(message: SmsMessage): Result<Boolean> {
        return try {
            val isCurrentlyStarred = repository.isMessageStarred(message.id)
            
            if (isCurrentlyStarred) {
                // Unstar the message
                repository.unstarMessage(message.id)
                Result.success(false)
            } else {
                // Star the message
                val contact = contactManager.getContactByPhoneNumber(message.sender)
                val senderName = contact?.name ?: message.sender
                
                val starredMessage = StarredMessage(
                    messageId = message.id,
                    sender = message.sender,
                    senderName = senderName,
                    messagePreview = message.content.take(100),
                    starredAt = java.util.Date(),
                    originalTimestamp = message.timestamp
                )
                
                repository.starMessage(starredMessage)
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Singleton
class GetStarredMessagesUseCase @Inject constructor(
    private val repository: SmsRepository
) {
    
    /**
     * Get all starred messages grouped by sender
     */
    fun getStarredMessagesBySender(): Flow<List<StarredSenderGroup>> {
        return repository.getStarredMessagesBySender()
    }
    
    /**
     * Get starred messages from a specific sender
     */
    fun getStarredMessagesForSender(sender: String): Flow<List<StarredMessage>> {
        return repository.getStarredMessagesForSender(sender)
    }
    
    /**
     * Get all starred messages (flat list)
     */
    fun getAllStarredMessages(): Flow<List<StarredMessage>> {
        return repository.getAllStarredMessages()
    }
    
    /**
     * Check if a message is starred
     */
    suspend fun isMessageStarred(messageId: Long): Boolean {
        return repository.isMessageStarred(messageId)
    }
}

@Singleton  
class DeleteMessageUseCase @Inject constructor(
    private val repository: SmsRepository
) {
    
    /**
     * Delete a specific message (also removes star if it was starred)
     */
    suspend operator fun invoke(messageId: Long): Result<Unit> {
        return try {
            // Remove star first if it exists
            repository.unstarMessage(messageId)
            // Then delete the message
            repository.deleteMessage(messageId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}