package com.smartsmsfilter.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "starred_messages",
    indices = [
        Index(value = ["sender"]),
        Index(value = ["starredAt"]),
        Index(value = ["messageId"], unique = true)
    ]
)
data class StarredMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageId: Long, // Reference to the original SMS message
    val sender: String, // Phone number or sender ID
    val senderName: String? = null, // Contact name if available
    val messagePreview: String, // First 100 chars of message content
    val starredAt: Long = System.currentTimeMillis(),
    val originalTimestamp: Long // Original message timestamp
)

@Dao
interface StarredMessageDao {
    
    @Query("SELECT * FROM starred_messages ORDER BY starredAt DESC")
    fun getAllStarredMessages(): Flow<List<StarredMessageEntity>>
    
    @Query("SELECT * FROM starred_messages WHERE sender = :sender ORDER BY originalTimestamp ASC")
    fun getStarredMessagesForSender(sender: String): Flow<List<StarredMessageEntity>>
    
    @Query("SELECT DISTINCT sender, senderName, COUNT(*) as messageCount, MAX(starredAt) as lastStarredAt FROM starred_messages GROUP BY sender ORDER BY lastStarredAt DESC")
    fun getStarredMessagesBySender(): Flow<List<StarredSenderSummary>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStarredMessage(starredMessage: StarredMessageEntity)
    
    @Query("DELETE FROM starred_messages WHERE messageId = :messageId")
    suspend fun unstarMessage(messageId: Long)
    
    @Query("DELETE FROM starred_messages WHERE sender = :sender")
    suspend fun unstarAllFromSender(sender: String)
    
    @Query("SELECT EXISTS(SELECT 1 FROM starred_messages WHERE messageId = :messageId)")
    suspend fun isMessageStarred(messageId: Long): Boolean
    
    @Query("SELECT COUNT(*) FROM starred_messages")
    suspend fun getStarredMessageCount(): Int
}

data class StarredSenderSummary(
    val sender: String,
    val senderName: String?,
    val messageCount: Int,
    val lastStarredAt: Long
)