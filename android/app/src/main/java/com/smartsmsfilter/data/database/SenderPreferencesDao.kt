package com.smartsmsfilter.data.database

import androidx.room.*

@Entity(tableName = "sender_preferences")
data class SenderPreferencesEntity(
    @PrimaryKey val sender: String,
    val pinnedToInbox: Boolean = false,
    val autoSpam: Boolean = false,
    val importanceScore: Float = 0f,
    val spamScore: Float = 0f,
    val userImportanceVotes: Int = 0,
    val userSpamVotes: Int = 0,
    val lastCorrectedAt: Long? = null
)

@Dao
interface SenderPreferencesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pref: SenderPreferencesEntity)

    @Query("SELECT * FROM sender_preferences WHERE sender = :sender LIMIT 1")
    suspend fun get(sender: String): SenderPreferencesEntity?

    @Query("UPDATE sender_preferences SET pinnedToInbox = :pinned WHERE sender = :sender")
    suspend fun setPinned(sender: String, pinned: Boolean)
}
