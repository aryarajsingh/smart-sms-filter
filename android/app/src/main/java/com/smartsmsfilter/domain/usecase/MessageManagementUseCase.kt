package com.smartsmsfilter.domain.usecase

import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.repository.SmsRepository
import com.smartsmsfilter.domain.common.Result
import javax.inject.Inject

/**
 * Use cases for message management operations
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
     */
    suspend fun moveToCategory(messageId: Long, category: MessageCategory) {
        smsRepository.moveToCategory(messageId, category)
    }
    
    /**
     * Move multiple messages to a category
     */
    suspend fun moveToCategoryBatch(messageIds: List<Long>, category: MessageCategory): Result<Unit> {
        return smsRepository.moveToCategoryBatch(messageIds, category)
    }
    
    /**
     * Mark message as important for learning purposes
     */
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
