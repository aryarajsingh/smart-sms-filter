package com.smartsmsfilter.domain.model

import java.util.Date

data class StarredMessage(
    val id: Long = 0,
    val messageId: Long, // Reference to original SMS message
    val sender: String,
    val senderName: String? = null, // Contact name if available
    val messagePreview: String,
    val starredAt: Date,
    val originalTimestamp: Date
)

data class StarredSenderGroup(
    val sender: String,
    val senderName: String? = null,
    val starredMessages: List<StarredMessage>,
    val messageCount: Int,
    val lastStarredAt: Date
)

// Extension functions for mapping between domain and data models
fun com.smartsmsfilter.data.database.StarredMessageEntity.toDomain() = StarredMessage(
    id = id,
    messageId = messageId,
    sender = sender,
    senderName = senderName,
    messagePreview = messagePreview,
    starredAt = Date(starredAt),
    originalTimestamp = Date(originalTimestamp)
)

fun StarredMessage.toEntity() = com.smartsmsfilter.data.database.StarredMessageEntity(
    id = if (id == 0L) 0 else id,
    messageId = messageId,
    sender = sender,
    senderName = senderName,
    messagePreview = messagePreview,
    starredAt = starredAt.time,
    originalTimestamp = originalTimestamp.time
)