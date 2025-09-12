// This file is for extensions only - enum is defined in SmsMessage.kt
package com.smartsmsfilter.domain.model

// Extension functions for display names
fun MessageCategory.displayName(): String = when (this) {
    MessageCategory.INBOX -> "Inbox"
    MessageCategory.SPAM -> "Spam"
    MessageCategory.NEEDS_REVIEW -> "Review"
}
