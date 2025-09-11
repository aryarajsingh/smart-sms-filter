package com.smartsmsfilter.domain.model

import java.util.Date

data class SmsMessage(
    val id: Long = 0,
    val sender: String,
    val content: String,
    val timestamp: Date,
    val category: MessageCategory = MessageCategory.NEEDS_REVIEW,
    val isRead: Boolean = false,
    val threadId: String? = null
)

enum class MessageCategory {
    INBOX,      // Important messages (OTP, bank, personal)
    FILTERED,   // Spam/promotional messages  
    NEEDS_REVIEW // Uncertain classification
}

data class MessageClassification(
    val category: MessageCategory,
    val confidence: Float,
    val reasons: List<String> = emptyList()
)
