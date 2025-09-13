package com.smartsmsfilter.domain.model

import java.util.Date

data class SmsMessage(
    val id: Long = 0,
    val sender: String,
    val content: String,
    val timestamp: Date,
    val category: MessageCategory = MessageCategory.NEEDS_REVIEW,
    val isRead: Boolean = false,
    val threadId: String? = null,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val manualCategoryOverride: MessageCategory? = null,
    val isImportant: Boolean = false,
    // True if this message was sent by the user (outgoing). Used for UI alignment/colors.
    val isOutgoing: Boolean = false
)

enum class MessageCategory {
    INBOX,          // Important messages (OTP, bank, personal)
    SPAM,           // Promotional/unwanted messages  
    NEEDS_REVIEW    // Uncertain classification requiring user review
}

data class MessageClassification(
    val category: MessageCategory,
    val confidence: Float,
    val reasons: List<String> = emptyList(),
    val messageId: Long? = null
)
