package com.smartsmsfilter.data.repository

import android.util.Log
import com.smartsmsfilter.data.database.SmsMessageDao
import com.smartsmsfilter.data.model.toDomain
import com.smartsmsfilter.data.model.toEntity
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.repository.SmsRepository
import com.smartsmsfilter.data.model.toDomain
import com.smartsmsfilter.domain.model.toDomain
import com.smartsmsfilter.domain.model.toEntity
import com.smartsmsfilter.domain.common.Result
import com.smartsmsfilter.domain.common.suspendResultOf
import com.smartsmsfilter.domain.common.AppException
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of the [SmsRepository].
 * This class is the single source of truth for all SMS message data, handling the complexities of
 * database operations, transaction management, and mapping between data and domain models.
 * @param dao The Data Access Object for SMS messages.
 * @param database The Room database instance.
 */
@Singleton
class SmsRepositoryImpl @Inject constructor(
    private val dao: SmsMessageDao,
    private val database: com.smartsmsfilter.data.database.SmsDatabase
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
    
    override suspend fun insertMessage(message: SmsMessage): Result<Long> = suspendResultOf {
        try {
            // Validate message data
            if (message.sender.isBlank() || message.content.isBlank()) {
                Log.w("SmsRepository", "Attempting to insert message with empty sender or content")
                return@suspendResultOf -2L // Invalid message
            }
            
            // Check for exact duplicates only (same sender, content, and exact timestamp)
            val isDuplicate = dao.isDuplicate(
                sender = message.sender,
                content = message.content,
                timestamp = message.timestamp.time
            ) > 0
            
            if (!isDuplicate) {
                dao.insertMessage(message.toEntity())
            } else {
                Log.d("SmsRepository", "Skipping duplicate message from ${message.sender}")
                -1L // Indicate duplicate
            }
        } catch (e: Exception) {
            throw AppException.DatabaseError("insertMessage", e)
        }
    }
    
    override suspend fun updateMessageCategory(messageId: Long, category: MessageCategory): Result<Unit> = suspendResultOf {
        try {
            dao.updateMessageCategory(messageId, category.name)
        } catch (e: Exception) {
            throw AppException.DatabaseError("updateMessageCategory", e)
        }
    }
    
    override suspend fun markAsRead(messageId: Long): Result<Unit> = suspendResultOf {
        try {
            dao.markAsRead(messageId)
        } catch (e: Exception) {
            throw AppException.DatabaseError("markAsRead", e)
        }
    }
    
    override suspend fun deleteMessage(messageId: Long): Result<Unit> = suspendResultOf {
        try {
            dao.deleteMessageById(messageId)
        } catch (e: Exception) {
            throw AppException.DatabaseError("deleteMessage", e)
        }
    }
    
    // Message management implementations
    override suspend fun archiveMessage(messageId: Long): Result<Unit> = suspendResultOf {
        try {
            dao.archiveMessage(messageId)
        } catch (e: Exception) {
            throw AppException.DatabaseError("archiveMessage", e)
        }
    }
    
    override suspend fun archiveMessages(messageIds: List<Long>): Result<Unit> = suspendResultOf {
        database.withTransaction {
            try {
                dao.archiveMessages(messageIds)
            } catch (e: Exception) {
                throw AppException.DatabaseError("archiveMessages", e)
            }
        }
    }
    
    override suspend fun unarchiveMessage(messageId: Long): Result<Unit> = suspendResultOf {
        try {
            dao.unarchiveMessage(messageId)
        } catch (e: Exception) {
            throw AppException.DatabaseError("unarchiveMessage", e)
        }
    }
    
    override suspend fun softDeleteMessage(messageId: Long): Result<Unit> = suspendResultOf {
        try {
            dao.softDeleteMessage(messageId)
        } catch (e: Exception) {
            throw AppException.DatabaseError("softDeleteMessage", e)
        }
    }
    
    override suspend fun softDeleteMessages(messageIds: List<Long>): Result<Unit> = suspendResultOf {
        database.withTransaction {
            try {
                dao.softDeleteMessages(messageIds)
            } catch (e: Exception) {
                throw AppException.DatabaseError("softDeleteMessages", e)
            }
        }
    }

    override suspend fun restoreSoftDeletedMessages(messageIds: List<Long>): Result<Unit> = suspendResultOf {
        database.withTransaction {
            try {
                dao.restoreSoftDeletedMessages(messageIds)
            } catch (e: Exception) {
                throw AppException.DatabaseError("restoreSoftDeletedMessages", e)
            }
        }
    }
    
    override suspend fun moveToCategory(messageId: Long, category: MessageCategory): Result<Unit> = suspendResultOf {
        try {
            dao.moveToCategory(messageId, category.name)
        } catch (e: Exception) {
            throw AppException.DatabaseError("moveToCategory", e)
        }
    }
    
    override suspend fun moveToCategoryBatch(messageIds: List<Long>, category: MessageCategory): Result<Unit> = suspendResultOf {
        android.util.Log.d("SmsRepositoryImpl", "Starting batch category move for ${messageIds.size} messages to $category")
        database.withTransaction {
            try {
                android.util.Log.d("SmsRepositoryImpl", "Executing DAO batch move with IDs: $messageIds")
                dao.moveToCategoryBatch(messageIds, category.name)
                android.util.Log.d("SmsRepositoryImpl", "Successfully completed batch move to category: $category")
            } catch (e: Exception) {
                android.util.Log.e("SmsRepositoryImpl", "Error in DAO batch move operation", e)
                throw AppException.DatabaseError("moveToCategoryBatch", e)
            }
        }
    }
    
    override suspend fun markAsImportant(messageId: Long, isImportant: Boolean): Result<Unit> = suspendResultOf {
        try {
            dao.markAsImportant(messageId, isImportant)
        } catch (e: Exception) {
            throw AppException.DatabaseError("markAsImportant", e)
        }
    }
    
    override fun getArchivedMessages(): Flow<List<SmsMessage>> {
        return dao.getArchivedMessages().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getAllActiveMessages(): Flow<List<SmsMessage>> {
        return dao.getAllActiveMessages().map { entities ->
            entities.map { it.toDomain() }
        }
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
    // Sender preferences mapping helpers
    private fun com.smartsmsfilter.data.database.SenderPreferencesEntity.toDomain() = com.smartsmsfilter.domain.model.SenderPreferences(
        sender = sender,
        pinnedToInbox = pinnedToInbox,
        autoSpam = autoSpam,
        importanceScore = importanceScore,
        spamScore = spamScore,
        userImportanceVotes = userImportanceVotes,
        userSpamVotes = userSpamVotes,
        lastCorrectedAt = lastCorrectedAt
    )

    override suspend fun getSenderPreferences(sender: String): com.smartsmsfilter.domain.model.SenderPreferences? {
        return database.senderPreferencesDao().get(sender)?.toDomain()
    }

    override suspend fun setSenderPinnedToInbox(sender: String, pinned: Boolean): com.smartsmsfilter.domain.common.Result<Unit> = com.smartsmsfilter.domain.common.suspendResultOf {
        try {
            database.senderPreferencesDao().setPinned(sender, pinned)
        } catch (e: Exception) {
            throw com.smartsmsfilter.domain.common.AppException.DatabaseError("setSenderPinnedToInbox", e)
        }
    }

    override suspend fun updateSenderReputation(sender: String, importanceScore: Float?, spamScore: Float?): com.smartsmsfilter.domain.common.Result<Unit> = com.smartsmsfilter.domain.common.suspendResultOf {
        try {
            val existing = database.senderPreferencesDao().get(sender)
            val updated = com.smartsmsfilter.data.database.SenderPreferencesEntity(
                sender = sender,
                pinnedToInbox = existing?.pinnedToInbox ?: false,
                autoSpam = existing?.autoSpam ?: false,
                importanceScore = importanceScore ?: existing?.importanceScore ?: 0f,
                spamScore = spamScore ?: existing?.spamScore ?: 0f,
                userImportanceVotes = existing?.userImportanceVotes ?: 0,
                userSpamVotes = existing?.userSpamVotes ?: 0,
                lastCorrectedAt = System.currentTimeMillis()
            )
            database.senderPreferencesDao().upsert(updated)
        } catch (e: Exception) {
            throw com.smartsmsfilter.domain.common.AppException.DatabaseError("updateSenderReputation", e)
        }
    }
    override suspend fun insertClassificationAudit(messageId: Long?, classifier: com.smartsmsfilter.domain.model.MessageCategory, category: com.smartsmsfilter.domain.model.MessageCategory, confidence: Float, reasons: List<String>): com.smartsmsfilter.domain.common.Result<Unit> = com.smartsmsfilter.domain.common.suspendResultOf {
        try {
            val joined = if (reasons.isNotEmpty()) reasons.joinToString("|") else null
            database.classificationAuditDao().insert(
                com.smartsmsfilter.data.database.ClassificationAuditEntity(
                    messageId = messageId,
                    classifier = "hybrid",
                    category = category.name,
                    confidence = confidence,
                    reasonsJson = joined
                )
            )
        } catch (e: Exception) {
            throw com.smartsmsfilter.domain.common.AppException.DatabaseError("insertClassificationAudit", e)
        }
    }

    override suspend fun insertUserFeedbackAudit(messageId: Long, targetCategory: com.smartsmsfilter.domain.model.MessageCategory, reasons: List<String>): com.smartsmsfilter.domain.common.Result<Unit> = com.smartsmsfilter.domain.common.suspendResultOf {
        try {
            val joined = if (reasons.isNotEmpty()) reasons.joinToString("|") else null
            database.classificationAuditDao().insert(
                com.smartsmsfilter.data.database.ClassificationAuditEntity(
                    messageId = messageId,
                    classifier = "user_feedback",
                    category = targetCategory.name,
                    confidence = 1.0f,
                    reasonsJson = joined
                )
            )
        } catch (e: Exception) {
            throw com.smartsmsfilter.domain.common.AppException.DatabaseError("insertUserFeedbackAudit", e)
        }
    }

    override suspend fun getLatestClassificationReason(messageId: Long): String? {
        return try {
            database.classificationAuditDao().getLatestReasonsForMessage(messageId)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getMessageById(messageId: Long): SmsMessage? {
        return try {
            dao.getMessageById(messageId)?.toDomain()
        } catch (e: Exception) {
            null
        }
    }
    
    // Starred messages implementation
    override suspend fun starMessage(starredMessage: com.smartsmsfilter.domain.model.StarredMessage): com.smartsmsfilter.domain.common.Result<Unit> = com.smartsmsfilter.domain.common.suspendResultOf {
        try {
            database.starredMessageDao().insertStarredMessage(starredMessage.toEntity())
        } catch (e: Exception) {
            throw com.smartsmsfilter.domain.common.AppException.DatabaseError("starMessage", e)
        }
    }
    
    override suspend fun unstarMessage(messageId: Long): com.smartsmsfilter.domain.common.Result<Unit> = com.smartsmsfilter.domain.common.suspendResultOf {
        try {
            database.starredMessageDao().unstarMessage(messageId)
        } catch (e: Exception) {
            throw com.smartsmsfilter.domain.common.AppException.DatabaseError("unstarMessage", e)
        }
    }
    
    override suspend fun isMessageStarred(messageId: Long): Boolean {
        return try {
            database.starredMessageDao().isMessageStarred(messageId)
        } catch (e: Exception) {
            false
        }
    }
    
    override fun getAllStarredMessages(): Flow<List<com.smartsmsfilter.domain.model.StarredMessage>> {
        return database.starredMessageDao().getAllStarredMessages().map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }
    
    override fun getStarredMessagesBySender(): Flow<List<com.smartsmsfilter.domain.model.StarredSenderGroup>> {
        return database.starredMessageDao().getStarredMessagesBySender().map { summaries ->
            summaries.map { summary ->
                com.smartsmsfilter.domain.model.StarredSenderGroup(
                    sender = summary.sender,
                    senderName = summary.senderName,
                    starredMessages = emptyList(), // Will be loaded separately when needed
                    messageCount = summary.messageCount,
                    lastStarredAt = java.util.Date(summary.lastStarredAt)
                )
            }
        }
    }
    
    override fun getStarredMessagesForSender(sender: String): Flow<List<com.smartsmsfilter.domain.model.StarredMessage>> {
        return database.starredMessageDao().getStarredMessagesForSender(sender).map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }
    
    override suspend fun getStarredMessageCount(): Int {
        return try {
            database.starredMessageDao().getStarredMessageCount()
        } catch (e: Exception) {
            0
        }
    }
}
