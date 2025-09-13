package com.smartsmsfilter.utils

import android.util.Log

/**
 * Extension functions for safe null handling throughout the app
 */

/**
 * Safe let with logging for null cases
 */
inline fun <T> T?.safeLetWithLog(tag: String, message: String, block: (T) -> Unit) {
    if (this != null) {
        block(this)
    } else {
        Log.w(tag, "Null value encountered: $message")
    }
}

/**
 * Safe execution with default value
 */
inline fun <T, R> T?.safelyExecute(default: R, block: (T) -> R): R {
    return if (this != null) {
        try {
            block(this)
        } catch (e: Exception) {
            Log.e("SafetyExtensions", "Error in safe execution", e)
            default
        }
    } else {
        default
    }
}

/**
 * Require not null with descriptive error
 */
fun <T> T?.requireNotNullWithMessage(lazyMessage: () -> String): T {
    return this ?: throw IllegalStateException(lazyMessage())
}

/**
 * Safe cast with logging
 */
inline fun <reified T> Any?.safeCastWithLog(tag: String): T? {
    return (this as? T).also { result ->
        if (result == null && this != null) {
            Log.w(tag, "Failed to cast ${this::class.simpleName} to ${T::class.simpleName}")
        }
    }
}

/**
 * Execute if not null with else branch
 */
inline fun <T> T?.ifNotNullOrElse(ifNotNull: (T) -> Unit, orElse: () -> Unit) {
    if (this != null) {
        ifNotNull(this)
    } else {
        orElse()
    }
}

/**
 * Safe string conversion
 */
fun Any?.toSafeString(): String {
    return this?.toString() ?: ""
}

/**
 * Safe list access
 */
fun <T> List<T>?.safeGet(index: Int): T? {
    return this?.getOrNull(index)
}

/**
 * Safe map access with default
 */
fun <K, V> Map<K, V>?.safeGet(key: K, default: V): V {
    return this?.get(key) ?: default
}