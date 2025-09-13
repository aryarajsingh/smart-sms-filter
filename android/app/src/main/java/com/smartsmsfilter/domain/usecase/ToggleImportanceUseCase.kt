package com.smartsmsfilter.domain.usecase

import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.repository.SmsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToggleImportanceUseCase @Inject constructor(
    private val repository: SmsRepository
) {
    
    /**
     * Toggle importance flag of a message and learn from user behavior
     * 
     * UX Logic:
     * - When marking as important: Move to inbox + pin sender for future
     * - When removing importance: Just remove flag, don't move message
     * - No message should be marked important by default
     */
    suspend fun invoke(messageId: Long, markAsImportant: Boolean): Result<SmsMessage> {
        return try {
            // Get the current message
            val currentMessage = repository.getMessageById(messageId)
                ?: return Result.failure(IllegalArgumentException("Message not found"))
            
            // Update the importance flag
            repository.markAsImportant(messageId, markAsImportant)
            
            // If marking as important, also move to inbox and learn from behavior
            if (markAsImportant) {
                // Move message to inbox if it's not already there
                if (currentMessage.category != MessageCategory.INBOX) {
                    repository.moveToCategory(messageId, MessageCategory.INBOX)
                }
                
                // Learn that this sender should be treated as important in future
                // Pin sender to inbox for future messages
                repository.setSenderPinnedToInbox(currentMessage.sender, true)
                // Boost sender importance score
                repository.updateSenderReputation(currentMessage.sender, 0.8f, null)
            }
            
            // Return the updated message with new category if changed
            val finalMessage = currentMessage.copy(
                isImportant = markAsImportant,
                category = if (markAsImportant && currentMessage.category != MessageCategory.INBOX) {
                    MessageCategory.INBOX
                } else {
                    currentMessage.category
                }
            )
            
            Result.success(finalMessage)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if a message is marked as important
     */
    suspend fun isMessageImportant(messageId: Long): Boolean {
        return try {
            val message = repository.getMessageById(messageId)
            message?.isImportant ?: false
        } catch (e: Exception) {
            false
        }
    }
}