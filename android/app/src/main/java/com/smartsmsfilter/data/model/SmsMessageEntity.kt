package com.smartsmsfilter.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartsmsfilter.domain.model.MessageCategory
import java.util.Date

@Entity(tableName = "sms_messages")
data class SmsMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sender: String,
    val content: String,
    val timestamp: Long,
    val category: String,
    val isRead: Boolean = false,
    val threadId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// Extension functions for mapping between domain and data models
fun SmsMessageEntity.toDomain() = com.smartsmsfilter.domain.model.SmsMessage(
    id = id,
    sender = sender,
    content = content,
    timestamp = Date(timestamp),
    category = MessageCategory.valueOf(category),
    isRead = isRead,
    threadId = threadId
)

fun com.smartsmsfilter.domain.model.SmsMessage.toEntity() = SmsMessageEntity(
    id = if (id == 0L) 0 else id,
    sender = sender,
    content = content,
    timestamp = timestamp.time,
    category = category.name,
    isRead = isRead,
    threadId = threadId
)
