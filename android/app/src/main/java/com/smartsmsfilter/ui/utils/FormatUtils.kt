package com.smartsmsfilter.ui.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Formats a timestamp into a relative time string (e.g., "2m", "1h", "yesterday")
 */
fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    val minutes = diff / (1000 * 60)
    val hours = minutes / 60
    val days = hours / 24
    
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days == 1L -> "yesterday"
        days < 7 -> "${days}d"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}

/**
 * Gets a display name for a sender (extracts contact name or formats phone number)
 */
fun getSenderDisplayName(sender: String): String {
    // Remove country code if present
    val cleanedNumber = sender.removePrefix("+1").removePrefix("1")
    
    // Format as phone number if it's numeric
    return if (cleanedNumber.all { it.isDigit() } && cleanedNumber.length == 10) {
        "(${cleanedNumber.substring(0, 3)}) ${cleanedNumber.substring(3, 6)}-${cleanedNumber.substring(6)}"
    } else {
        sender // Return as-is for short codes or names
    }
}

/**
 * Formats a Date object into a relative time string
 */
fun formatRelativeTime(date: Date): String = formatRelativeTime(date.time)
