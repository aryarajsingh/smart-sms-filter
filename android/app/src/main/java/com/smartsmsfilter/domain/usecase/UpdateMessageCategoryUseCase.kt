package com.smartsmsfilter.domain.usecase

import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.repository.SmsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateMessageCategoryWithLearningUseCase @Inject constructor(
    private val repository: SmsRepository
) {
    
    /**
     * Update message category with proper UX logic and learning
     * 
     * UX Rules:
     * 1. When moving to INBOX: Learn that sender is good
     * 2. When moving to SPAM: Learn that sender is bad  
     * 3. When marking as IMPORTANT: Also move to INBOX + pin sender
     * 4. All changes trigger learning for future messages
     */
    suspend operator fun invoke(
        messageId: Long, 
        newCategory: MessageCategory,
        markAsImportant: Boolean = false
    ): Result<SmsMessage> {
        return try {
            // Get the current message
            val message = repository.getMessageById(messageId)
                ?: return Result.failure(IllegalArgumentException("Message not found"))
            
            // Update the category
            repository.moveToCategory(messageId, newCategory)
            
            // Update importance flag if requested
            if (markAsImportant) {
                repository.markAsImportant(messageId, true)
                // If marking as important, also learn from this behavior
                repository.setSenderPinnedToInbox(message.sender, true)
                repository.updateSenderReputation(message.sender, 0.8f, null)
            } else {
                // Learn from category change
                when (newCategory) {
                    MessageCategory.SPAM -> {
                        repository.setSenderPinnedToInbox(message.sender, false)
                        repository.updateSenderReputation(message.sender, null, 0.8f)
                    }
                    MessageCategory.INBOX -> {
                        repository.updateSenderReputation(message.sender, 0.7f, null)
                    }
                    MessageCategory.NEEDS_REVIEW -> { /* No learning needed */ }
                }
            }
            
            // Return updated message
            val updatedMessage = message.copy(
                category = newCategory,
                isImportant = if (markAsImportant) true else message.isImportant
            )
            
            Result.success(updatedMessage)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Bulk update multiple messages to a category
     */
    suspend fun updateMultiple(
        messageIds: List<Long>,
        newCategory: MessageCategory
    ): Result<List<SmsMessage>> {
        return try {
            val updatedMessages = mutableListOf<SmsMessage>()
            
            messageIds.forEach { messageId ->
                val result = invoke(messageId, newCategory)
                if (result.isSuccess) {
                    result.getOrNull()?.let { updatedMessages.add(it) }
                }
            }
            
            Result.success(updatedMessages)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}