package com.smartsmsfilter.domain.usecase

import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.repository.SmsRepository
import com.smartsmsfilter.domain.common.Result
import javax.inject.Inject

/**
 * Use cases for message management operations
 * Integrates with sender learning to improve future classifications
 */
class MessageManagementUseCase @Inject constructor(
    private val smsRepository: SmsRepository
) {
    
    /**
     * Archive a single message
     */
    suspend fun archiveMessage(messageId: Long) {
        smsRepository.archiveMessage(messageId)
    }
    
    /**
     * Archive multiple messages
     */
    suspend fun archiveMessages(messageIds: List<Long>): Result<Unit> {
        return smsRepository.archiveMessages(messageIds)
    }
    
    /**
     * Unarchive a message
     */
    suspend fun unarchiveMessage(messageId: Long) {
        smsRepository.unarchiveMessage(messageId)
    }
    
    /**
     * Soft delete a message (mark as deleted but keep in database)
     */
    suspend fun deleteMessage(messageId: Long) {
        smsRepository.softDeleteMessage(messageId)
    }
    
    /**
     * Soft delete multiple messages
     */
    suspend fun deleteMessages(messageIds: List<Long>): Result<Unit> {
        return smsRepository.softDeleteMessages(messageIds)
    }
    
    /**
     * Move message to a different category (user manual override)
     * Also triggers sender learning from user behavior
     */
    suspend fun moveToCategory(messageId: Long, category: MessageCategory) {
        val message = smsRepository.getMessageById(messageId)
        smsRepository.moveToCategory(messageId, category)
        
        // Learn from user's category choice
        if (message != null) {
            when (category) {
                MessageCategory.SPAM -> {
                    // Remove inbox pinning if they marked as spam
                    smsRepository.setSenderPinnedToInbox(message.sender, false)
                    // Increase spam score
                    smsRepository.updateSenderReputation(message.sender, null, 0.8f)
                }
                MessageCategory.INBOX -> {
                    // Give sender slight positive boost
                    smsRepository.updateSenderReputation(message.sender, 0.7f, null)
                }
                MessageCategory.NEEDS_REVIEW -> { /* No learning needed for review */ }
            }
        }
    }
    
    /**
     * Move multiple messages to a category
     */
    suspend fun moveToCategoryBatch(messageIds: List<Long>, category: MessageCategory): Result<Unit> {
        return smsRepository.moveToCategoryBatch(messageIds, category)
    }
    
    /**
     * Mark message as important for learning purposes
     * Now delegates to the proper ToggleImportanceUseCase
     */
    @Deprecated("Use ToggleImportanceUseCase instead for proper learning integration")
    suspend fun markAsImportant(messageId: Long, isImportant: Boolean): Result<Unit> {
        return smsRepository.markAsImportant(messageId, isImportant)
    }
    
    /**
     * Mark message as spam (move to SPAM category)
     */
    suspend fun markAsSpam(messageId: Long) {
        moveToCategory(messageId, MessageCategory.SPAM)
    }
    
    /**
     * Mark message as not spam (move to INBOX category)  
     */
    suspend fun markAsNotSpam(messageId: Long) {
        moveToCategory(messageId, MessageCategory.INBOX)
    }
    
    /**
     * Mark multiple messages as spam
     */
    suspend fun markAsSpamBatch(messageIds: List<Long>) {
        moveToCategoryBatch(messageIds, MessageCategory.SPAM)
    }
    
    /**
     * Mark multiple messages as not spam
     */
    suspend fun markAsNotSpamBatch(messageIds: List<Long>) {
        moveToCategoryBatch(messageIds, MessageCategory.INBOX)
    }
}

/**
 * Represents the result of a message management operation
 */
data class MessageManagementResult(
    val success: Boolean,
    val message: String? = null,
    val affectedMessageIds: List<Long> = emptyList()
)
