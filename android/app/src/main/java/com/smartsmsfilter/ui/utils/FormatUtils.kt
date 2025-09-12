package com.smartsmsfilter.ui.utils

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
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

/** Normalizes a phone number for lookup/storage by stripping spaces, dashes, parentheses; keeps leading +. */
fun normalizePhoneNumber(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    // Keep leading + and digits only
    val cleaned = raw.trim().replace(Regex("[^+0-9]"), "")
    // Avoid leading zeros like 0091... (keep as-is if user has it); callers can further standardize if needed
    return cleaned
}

/**
 * Gets a display name for a sender (extracts contact name or formats phone number)
 */
fun getSenderDisplayName(sender: String): String {
    // Pure formatting fallback for non-contact contexts
    val digits = sender.filter { it.isDigit() || it == '+' }
    return if (digits.isNotBlank()) PhoneNumberUtils.formatNumber(digits, Locale.getDefault().country) ?: sender else sender
}

/**
 * Resolve display name via contacts when possible. Falls back to number formatting.
 * Tries multiple lookup strategies to better match historical messages with varying formats.
 */
fun resolveDisplayName(context: Context, address: String?): String {
    val addr = address?.trim().orEmpty()
    if (addr.isBlank()) return "Unknown"

    // In-memory cache to avoid repeated lookups
    val cached = NameCache.get(addr)
    if (cached != null) return cached

    fun queryPhoneLookup(value: String): String? {
        return try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(value))
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (_: Exception) { null }
    }

    fun queryByLastDigits(lastDigits: String): String? {
        return try {
            val sel = "${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
            val args = arrayOf("%$lastDigits")
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                sel,
                args,
                null
            )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        } catch (_: Exception) { null }
    }

    val normalized = normalizePhoneNumber(addr)
    val digitsOnly = normalized.filter { it.isDigit() }
    val last10 = if (digitsOnly.length >= 10) digitsOnly.takeLast(10) else null

    val candidates = listOf(addr, normalized) + listOfNotNull(last10)

    for (candidate in candidates) {
        val name = queryPhoneLookup(candidate)
        if (!name.isNullOrBlank()) {
            NameCache.put(addr, name)
            return name
        }
    }

    if (last10 != null) {
        val name = queryByLastDigits(last10)
        if (!name.isNullOrBlank()) {
            NameCache.put(addr, name)
            return name
        }
    }

    val fallback = getSenderDisplayName(addr)
    NameCache.put(addr, fallback)
    return fallback
}

/** Simple name cache */
private object NameCache {
    private const val MAX = 256
    private val map = object : LinkedHashMap<String, String>(MAX, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > MAX
    }
    fun get(key: String): String? = synchronized(map) { map[key] }
    fun put(key: String, value: String) { synchronized(map) { map[key] = value } }
}

/**
 * Formats a Date object into a relative time string
 */
fun formatRelativeTime(date: Date): String = formatRelativeTime(date.time)
