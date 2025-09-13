package com.smartsmsfilter.domain.usecase

import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.repository.SmsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class SenderLearningUseCase @Inject constructor(
    private val repository: SmsRepository
) {
    
    /**
     * When user marks a message as important, learn that this sender should 
     * automatically go to inbox in the future
     */
    suspend fun learnFromImportanceMarking(message: SmsMessage) {
        try {
            val existingPrefs = repository.getSenderPreferences(message.sender)
            
            // Pin sender to inbox for future messages
            repository.setSenderPinnedToInbox(message.sender, true)
            
            // Boost sender importance score  
            val newImportanceScore = if (existingPrefs != null) {
                minOf(existingPrefs.importanceScore + 0.3f, 1.0f)
            } else {
                0.8f // High importance for newly marked important senders
            }
            
            repository.updateSenderReputation(
                sender = message.sender,
                importanceScore = newImportanceScore,
                spamScore = null // Don't change spam score
            )
            
            // Also move all existing messages from this sender to inbox if they're in review
            val existingMessages = repository.getMessagesByAddress(message.sender).first()
            existingMessages.forEach { msg ->
                if (msg.category == MessageCategory.NEEDS_REVIEW) {
                    repository.moveToCategory(msg.id, MessageCategory.INBOX)
                }
            }
            
        } catch (e: Exception) {
            // Best effort - don't fail the UI operation if learning fails
        }
    }
    
    /**
     * When user moves message to spam, learn that this sender should be auto-spam
     */
    suspend fun learnFromSpamMarking(message: SmsMessage) {
        try {
            val existingPrefs = repository.getSenderPreferences(message.sender)
            
            // Remove inbox pinning and mark as auto-spam
            repository.setSenderPinnedToInbox(message.sender, false)
            
            // Update reputation scores
            val newSpamScore = if (existingPrefs != null) {
                minOf(existingPrefs.spamScore + 0.5f, 1.0f)
            } else {
                0.9f
            }
            
            val newImportanceScore = if (existingPrefs != null) {
                maxOf(existingPrefs.importanceScore - 0.3f, 0.0f)
            } else {
                0.0f
            }
            
            repository.updateSenderReputation(
                sender = message.sender,
                importanceScore = newImportanceScore,
                spamScore = newSpamScore
            )
            
        } catch (e: Exception) {
            // Best effort - don't fail the UI operation if learning fails
        }
    }
    
    /**
     * When user moves message to inbox (but doesn't mark as important), 
     * give sender slight positive boost
     */
    suspend fun learnFromInboxMove(message: SmsMessage) {
        try {
            val existingPrefs = repository.getSenderPreferences(message.sender)
            
            // Give sender slight positive boost
            val newImportanceScore = if (existingPrefs != null) {
                minOf(existingPrefs.importanceScore + 0.1f, 1.0f)
            } else {
                0.1f
            }
            
            val newSpamScore = if (existingPrefs != null) {
                maxOf(existingPrefs.spamScore - 0.1f, 0.0f)
            } else {
                null // Don't set spam score for new senders
            }
            
            repository.updateSenderReputation(
                sender = message.sender,
                importanceScore = newImportanceScore,
                spamScore = newSpamScore
            )
            
        } catch (e: Exception) {
            // Best effort
        }
    }
}