package com.smartsmsfilter.data.database

import androidx.room.*

@Entity(tableName = "classification_audit")
data class ClassificationAuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val messageId: Long?,
    val classifier: String,
    val category: String,
    val confidence: Float,
    val reasonsJson: String?,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ClassificationAuditDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(audit: ClassificationAuditEntity)

    @Query("DELETE FROM classification_audit WHERE timestamp < :cutoff")
    suspend fun pruneOlderThan(cutoff: Long)

    @Query("SELECT reasonsJson FROM classification_audit WHERE messageId = :messageId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestReasonsForMessage(messageId: Long): String?
}
