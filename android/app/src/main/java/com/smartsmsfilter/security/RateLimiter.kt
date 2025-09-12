package com.smartsmsfilter.security

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private val Context.rateLimitDataStore by preferencesDataStore(name = "rate_limits")

/**
 * Manages rate limiting for various operations to prevent abuse
 * Implements token bucket algorithm for flexible rate limiting
 */
@Singleton
class RateLimiter @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "RateLimiter"
        
        // Rate limit configurations
        private const val SMS_HOURLY_LIMIT = 30
        private const val SMS_DAILY_LIMIT = 100
        private const val SMS_PER_NUMBER_HOURLY_LIMIT = 5
        
        // Time windows in milliseconds
        private const val HOUR_IN_MILLIS = 3600000L
        private const val DAY_IN_MILLIS = 86400000L
        
        // DataStore keys
        private val SMS_HOURLY_COUNT = intPreferencesKey("sms_hourly_count")
        private val SMS_HOURLY_RESET = longPreferencesKey("sms_hourly_reset")
        private val SMS_DAILY_COUNT = intPreferencesKey("sms_daily_count")
        private val SMS_DAILY_RESET = longPreferencesKey("sms_daily_reset")
    }
    
    // In-memory cache for per-number limits
    private val perNumberLimits = ConcurrentHashMap<String, TokenBucket>()
    
    /**
     * Token bucket implementation for rate limiting
     */
    private class TokenBucket(
        private val capacity: Int,
        private val refillPeriodMillis: Long
    ) {
        private var tokens: Int = capacity
        private var lastRefillTime: Long = System.currentTimeMillis()
        
        @Synchronized
        fun tryConsume(): Boolean {
            refill()
            return if (tokens > 0) {
                tokens--
                true
            } else {
                false
            }
        }
        
        private fun refill() {
            val now = System.currentTimeMillis()
            val timePassed = now - lastRefillTime
            
            if (timePassed >= refillPeriodMillis) {
                tokens = capacity
                lastRefillTime = now
            }
        }
        
        @Synchronized
        fun getAvailableTokens(): Int {
            refill()
            return tokens
        }
    }
    
    /**
     * Checks if an SMS can be sent based on rate limits
     * @param phoneNumber The recipient phone number
     * @return Pair of (canSend, errorMessage)
     */
    suspend fun canSendSms(phoneNumber: String): Pair<Boolean, String?> {
        // Check hourly limit
        val hourlyResult = checkHourlyLimit()
        if (!hourlyResult.first) {
            return hourlyResult
        }
        
        // Check daily limit
        val dailyResult = checkDailyLimit()
        if (!dailyResult.first) {
            return dailyResult
        }
        
        // Check per-number limit
        val perNumberResult = checkPerNumberLimit(phoneNumber)
        if (!perNumberResult.first) {
            return perNumberResult
        }
        
        return Pair(true, null)
    }
    
    /**
     * Records that an SMS was sent
     * @param phoneNumber The recipient phone number
     */
    suspend fun recordSmsSent(phoneNumber: String) {
        // Update hourly count
        context.rateLimitDataStore.edit { preferences ->
            val currentTime = System.currentTimeMillis()
            val hourlyReset = preferences[SMS_HOURLY_RESET] ?: 0L
            
            if (currentTime - hourlyReset >= HOUR_IN_MILLIS) {
                preferences[SMS_HOURLY_COUNT] = 1
                preferences[SMS_HOURLY_RESET] = currentTime
            } else {
                val currentCount = preferences[SMS_HOURLY_COUNT] ?: 0
                preferences[SMS_HOURLY_COUNT] = currentCount + 1
            }
        }
        
        // Update daily count
        context.rateLimitDataStore.edit { preferences ->
            val currentTime = System.currentTimeMillis()
            val dailyReset = preferences[SMS_DAILY_RESET] ?: 0L
            
            if (currentTime - dailyReset >= DAY_IN_MILLIS) {
                preferences[SMS_DAILY_COUNT] = 1
                preferences[SMS_DAILY_RESET] = currentTime
            } else {
                val currentCount = preferences[SMS_DAILY_COUNT] ?: 0
                preferences[SMS_DAILY_COUNT] = currentCount + 1
            }
        }
        
        // Consume token for per-number limit
        val bucket = perNumberLimits.getOrPut(phoneNumber) {
            TokenBucket(SMS_PER_NUMBER_HOURLY_LIMIT, HOUR_IN_MILLIS)
        }
        bucket.tryConsume()
        
        Log.d(TAG, "SMS sent recorded for $phoneNumber")
    }
    
    /**
     * Gets the current rate limit status
     */
    suspend fun getRateLimitStatus(): RateLimitStatus {
        val hourlyCount = context.rateLimitDataStore.data.map { 
            it[SMS_HOURLY_COUNT] ?: 0 
        }.first()
        
        val dailyCount = context.rateLimitDataStore.data.map { 
            it[SMS_DAILY_COUNT] ?: 0 
        }.first()
        
        return RateLimitStatus(
            hourlyUsed = hourlyCount,
            hourlyLimit = SMS_HOURLY_LIMIT,
            dailyUsed = dailyCount,
            dailyLimit = SMS_DAILY_LIMIT
        )
    }
    
    /**
     * Resets all rate limits (for testing or administrative purposes)
     */
    suspend fun resetLimits() {
        context.rateLimitDataStore.edit { preferences ->
            preferences.clear()
        }
        perNumberLimits.clear()
        Log.d(TAG, "All rate limits reset")
    }
    
    private suspend fun checkHourlyLimit(): Pair<Boolean, String?> {
        val currentTime = System.currentTimeMillis()
        val data = context.rateLimitDataStore.data.first()
        val hourlyReset = data[SMS_HOURLY_RESET] ?: 0L
        val hourlyCount = if (currentTime - hourlyReset >= HOUR_IN_MILLIS) {
            0
        } else {
            data[SMS_HOURLY_COUNT] ?: 0
        }
        
        return if (hourlyCount >= SMS_HOURLY_LIMIT) {
            Pair(false, "Hourly SMS limit reached. Please try again later.")
        } else {
            Pair(true, null)
        }
    }
    
    private suspend fun checkDailyLimit(): Pair<Boolean, String?> {
        val currentTime = System.currentTimeMillis()
        val data = context.rateLimitDataStore.data.first()
        val dailyReset = data[SMS_DAILY_RESET] ?: 0L
        val dailyCount = if (currentTime - dailyReset >= DAY_IN_MILLIS) {
            0
        } else {
            data[SMS_DAILY_COUNT] ?: 0
        }
        
        return if (dailyCount >= SMS_DAILY_LIMIT) {
            Pair(false, "Daily SMS limit reached. Please try again tomorrow.")
        } else {
            Pair(true, null)
        }
    }
    
    private fun checkPerNumberLimit(phoneNumber: String): Pair<Boolean, String?> {
        val bucket = perNumberLimits.getOrPut(phoneNumber) {
            TokenBucket(SMS_PER_NUMBER_HOURLY_LIMIT, HOUR_IN_MILLIS)
        }
        
        return if (bucket.getAvailableTokens() <= 0) {
            Pair(false, "Too many messages to this number. Please wait before sending more.")
        } else {
            Pair(true, null)
        }
    }
}

/**
 * Data class representing current rate limit status
 */
data class RateLimitStatus(
    val hourlyUsed: Int,
    val hourlyLimit: Int,
    val dailyUsed: Int,
    val dailyLimit: Int
) {
    val hourlyRemaining: Int get() = (hourlyLimit - hourlyUsed).coerceAtLeast(0)
    val dailyRemaining: Int get() = (dailyLimit - dailyUsed).coerceAtLeast(0)
    val canSend: Boolean get() = hourlyRemaining > 0 && dailyRemaining > 0
}
