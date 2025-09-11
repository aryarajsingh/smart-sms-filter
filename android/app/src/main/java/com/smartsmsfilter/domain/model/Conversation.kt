package com.smartsmsfilter.domain.model

import com.smartsmsfilter.data.contacts.Contact
import java.util.Date

/**
 * Represents a conversation thread between the user and a contact/phone number
 */
data class Conversation(
    val id: String, // Usually the phone number or contact ID
    val contact: Contact,
    val messages: List<SmsMessage> = emptyList(),
    val lastMessage: SmsMessage? = messages.maxByOrNull { it.timestamp },
    val unreadCount: Int = messages.count { !it.isRead },
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val notificationEnabled: Boolean = true,
    val conversationColor: ConversationColor = ConversationColor.DEFAULT
)

/**
 * Conversation summary for list display
 */
data class ConversationSummary(
    val id: String,
    val contactName: String,
    val contactPhoneNumber: String,
    val contactPhotoUri: String?,
    val lastMessageText: String,
    val lastMessageTimestamp: Date,
    val unreadCount: Int,
    val isPinned: Boolean,
    val lastMessageCategory: MessageCategory,
    val conversationColor: ConversationColor = ConversationColor.DEFAULT
)

/**
 * Colors that can be assigned to conversations for organization
 */
enum class ConversationColor(val colorName: String) {
    DEFAULT("Default"),
    BLUE("Blue"),
    GREEN("Green"),
    ORANGE("Orange"),
    PURPLE("Purple"),
    RED("Red"),
    PINK("Pink"),
    TEAL("Teal")
}

/**
 * Conversation filters for organizing message view
 */
enum class ConversationFilter {
    ALL,
    UNREAD,
    PINNED,
    ARCHIVED,
    IMPORTANT,
    SPAM_FILTERED
}
