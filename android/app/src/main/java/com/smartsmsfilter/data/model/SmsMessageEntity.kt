package com.smartsmsfilter.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartsmsfilter.domain.model.MessageCategory
import java.util.Date

@Entity(
    tableName = "sms_messages",
    indices = [
        androidx.room.Index(value = ["category"]),
        androidx.room.Index(value = ["sender"]),
        androidx.room.Index(value = ["timestamp"]),
        androidx.room.Index(value = ["isArchived", "isDeleted"]),
        androidx.room.Index(value = ["isImportant"])
    ]
)
data class SmsMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val content: String,
    val timestamp: Long,
    val category: String,
    val isRead: Boolean = false,
    val threadId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val manualCategoryOverride: String? = null, // User manually changed category
    val isImportant: Boolean = false, // User marked as important
    val lastModified: Long = System.currentTimeMillis(),
    val isOutgoing: Boolean = false // True if sent by user
)

// Extension functions for mapping between domain and data models
fun SmsMessageEntity.toDomain() = com.smartsmsfilter.domain.model.SmsMessage(
    id = id,
    sender = sender,
    content = content,
    timestamp = Date(timestamp),
    category = MessageCategory.valueOf(category),
    isRead = isRead,
    threadId = threadId,
    isArchived = isArchived,
    isDeleted = isDeleted,
    manualCategoryOverride = manualCategoryOverride?.let { MessageCategory.valueOf(it) },
    isImportant = isImportant,
    isOutgoing = isOutgoing
)

fun com.smartsmsfilter.domain.model.SmsMessage.toEntity() = SmsMessageEntity(
    id = if (id == 0L) 0 else id,
    sender = sender,
    content = content,
    timestamp = timestamp.time,
    category = category.name,
    isRead = isRead,
    threadId = threadId,
    isArchived = isArchived,
    isDeleted = isDeleted,
    manualCategoryOverride = manualCategoryOverride?.name,
    isImportant = isImportant,
    isOutgoing = isOutgoing
)
