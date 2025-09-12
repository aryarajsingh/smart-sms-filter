package com.smartsmsfilter.domain.model

data class SenderPreferences(
    val sender: String,
    val pinnedToInbox: Boolean = false,
    val autoSpam: Boolean = false,
    val importanceScore: Float = 0f,
    val spamScore: Float = 0f,
    val userImportanceVotes: Int = 0,
    val userSpamVotes: Int = 0,
    val lastCorrectedAt: Long? = null
)
