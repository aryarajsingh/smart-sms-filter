package com.smartsmsfilter.domain.validation

import com.smartsmsfilter.domain.common.AppException
import com.smartsmsfilter.domain.common.Result
import com.smartsmsfilter.domain.common.asSuccess

/**
 * Validation utilities for domain entities and user inputs
 */
object ValidationUtils {
    
    // Phone number validation patterns
    private val PHONE_NUMBER_PATTERNS = listOf(
        Regex("^[+]?[1-9]\\d{1,14}$"), // E.164 format
        Regex("^[+]?[0-9]{10,15}$"),   // General international
        Regex("^[6-9]\\d{9}$"),        // Indian mobile format
        Regex("^[0]?[6-9]\\d{9}$")     // Indian mobile with optional 0
    )
    
    // Message constraints
    private const val MAX_MESSAGE_LENGTH = 1600 // Multiple SMS parts
    private const val MIN_MESSAGE_LENGTH = 1
    
    /**
     * Validates phone number format
     */
    fun validatePhoneNumber(phoneNumber: String?): Result<String> {
        if (phoneNumber.isNullOrBlank()) {
            return Result.Error(AppException.emptyField("phoneNumber"))
        }
        
        val cleanedNumber = phoneNumber.trim().replace("\\s+".toRegex(), "")
        
        val isValid = PHONE_NUMBER_PATTERNS.any { pattern ->
            pattern.matches(cleanedNumber)
        }
        
        return if (isValid) {
            cleanedNumber.asSuccess()
        } else {
            Result.Error(AppException.invalidPhoneNumber(phoneNumber))
        }
    }
    
    /**
     * Validates message content
     */
    fun validateMessageContent(message: String?): Result<String> {
        if (message.isNullOrBlank()) {
            return Result.Error(AppException.emptyField("message"))
        }
        
        val trimmedMessage = message.trim()
        
        return when {
            trimmedMessage.length < MIN_MESSAGE_LENGTH -> {
                Result.Error(AppException.emptyField("message"))
            }
            trimmedMessage.length > MAX_MESSAGE_LENGTH -> {
                Result.Error(AppException.messageTooLong(trimmedMessage.length, MAX_MESSAGE_LENGTH))
            }
            else -> trimmedMessage.asSuccess()
        }
    }
    
    /**
     * Validates contact name
     */
    fun validateContactName(name: String?): Result<String> {
        if (name.isNullOrBlank()) {
            return Result.Error(AppException.emptyField("name"))
        }
        
        val trimmedName = name.trim()
        
        return if (trimmedName.length in 1..100) {
            trimmedName.asSuccess()
        } else {
            Result.Error(AppException.UnknownError(IllegalArgumentException("Name must be between 1 and 100 characters")))
        }
    }
    
    /**
     * Validates message ID
     */
    fun validateMessageId(messageId: Long?): Result<Long> {
        return when {
            messageId == null -> Result.Error(AppException.UnknownError(IllegalArgumentException("Message ID cannot be null")))
            messageId <= 0 -> Result.Error(AppException.UnknownError(IllegalArgumentException("Message ID must be positive")))
            else -> messageId.asSuccess()
        }
    }
    
    /**
     * Validates a list of message IDs for batch operations
     */
    fun validateMessageIds(messageIds: List<Long>?): Result<List<Long>> {
        if (messageIds.isNullOrEmpty()) {
            return Result.Error(AppException.UnknownError(IllegalArgumentException("Message ID list cannot be empty")))
        }
        
        val invalidIds = messageIds.filter { it <= 0 }
        
        return if (invalidIds.isEmpty()) {
            messageIds.asSuccess()
        } else {
            Result.Error(AppException.UnknownError(IllegalArgumentException("Invalid message IDs found: $invalidIds")))
        }
    }
    
    /**
     * Sanitizes user input to prevent SQL injection and XSS
     */
    fun sanitizeUserInput(input: String?): String {
        if (input.isNullOrBlank()) return ""
        
        return input.trim()
            .replace("'", "''")  // Escape single quotes for SQL safety
            .replace("<", "&lt;") // Basic XSS prevention
            .replace(">", "&gt;")
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
    }
    
    /**
     * Validates search query
     */
    fun validateSearchQuery(query: String?): Result<String> {
        if (query.isNullOrBlank()) {
            return "".asSuccess() // Empty search is valid
        }
        
        val sanitizedQuery = sanitizeUserInput(query)
        
        return if (sanitizedQuery.length <= 500) {
            sanitizedQuery.asSuccess()
        } else {
            Result.Error(AppException.UnknownError(IllegalArgumentException("Search query too long")))
        }
    }
}

/**
 * Extension functions for easier validation
 */

/**
 * Validates phone number and returns the result
 */
fun String?.validatePhoneNumber(): Result<String> = ValidationUtils.validatePhoneNumber(this)

/**
 * Validates message content and returns the result
 */
fun String?.validateMessage(): Result<String> = ValidationUtils.validateMessageContent(this)

/**
 * Validates contact name and returns the result
 */
fun String?.validateContactName(): Result<String> = ValidationUtils.validateContactName(this)

/**
 * Validates message ID and returns the result
 */
fun Long?.validateMessageId(): Result<Long> = ValidationUtils.validateMessageId(this)

/**
 * Validates a list of message IDs
 */
fun List<Long>?.validateMessageIds(): Result<List<Long>> = ValidationUtils.validateMessageIds(this)

/**
 * Sanitizes user input
 */
fun String?.sanitize(): String = ValidationUtils.sanitizeUserInput(this)
