package com.smartsmsfilter.domain.common

/**
 * Base class for all application exceptions with user-friendly messages
 */
sealed class AppException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    
    /**
     * User-friendly message that can be displayed to the user
     */
    abstract val userMessage: String
    
    /**
     * Error code for logging and debugging
     */
    abstract val errorCode: String
    
    /**
     * Whether this error can be retried
     */
    abstract val isRetryable: Boolean
    
    /**
     * Whether this error should be reported to analytics
     */
    abstract val shouldReport: Boolean

    // SMS-related errors
    data class SmsPermissionDenied(
        override val cause: Throwable? = null
    ) : AppException("SMS permission denied", cause) {
        override val userMessage = "SMS permission is required to send messages. Please grant permission in settings."
        override val errorCode = "SMS_PERMISSION_DENIED"
        override val isRetryable = true
        override val shouldReport = false
    }
    
    data class SmsSendFailed(
        val phoneNumber: String,
        override val cause: Throwable? = null
    ) : AppException("Failed to send SMS to $phoneNumber", cause) {
        override val userMessage = "Failed to send message. Please check the phone number and try again."
        override val errorCode = "SMS_SEND_FAILED"
        override val isRetryable = true
        override val shouldReport = true
    }
    
    data class SmsReadFailed(
        override val cause: Throwable? = null
    ) : AppException("Failed to read SMS messages", cause) {
        override val userMessage = "Unable to load messages. Please try again."
        override val errorCode = "SMS_READ_FAILED"
        override val isRetryable = true
        override val shouldReport = true
    }

    // Contact-related errors
    data class ContactPermissionDenied(
        override val cause: Throwable? = null
    ) : AppException("Contact permission denied", cause) {
        override val userMessage = "Contact permission is required to load contacts. Please grant permission in settings."
        override val errorCode = "CONTACT_PERMISSION_DENIED"
        override val isRetryable = true
        override val shouldReport = false
    }
    
    data class ContactLoadFailed(
        override val cause: Throwable? = null
    ) : AppException("Failed to load contacts", cause) {
        override val userMessage = "Unable to load contacts. Please try again."
        override val errorCode = "CONTACT_LOAD_FAILED"
        override val isRetryable = true
        override val shouldReport = true
    }

    // Database-related errors
    data class DatabaseError(
        val operation: String,
        override val cause: Throwable? = null
    ) : AppException("Database operation '$operation' failed", cause) {
        override val userMessage = "A storage error occurred. Please try again."
        override val errorCode = "DATABASE_ERROR"
        override val isRetryable = true
        override val shouldReport = true
    }
    
    data class DatabaseCorrupted(
        override val cause: Throwable? = null
    ) : AppException("Database is corrupted", cause) {
        override val userMessage = "App data is corrupted. Please restart the app or reinstall if the problem persists."
        override val errorCode = "DATABASE_CORRUPTED"
        override val isRetryable = false
        override val shouldReport = true
    }

    // Validation errors
    data class InvalidPhoneNumber(
        val phoneNumber: String
    ) : AppException("Invalid phone number: $phoneNumber") {
        override val userMessage = "Please enter a valid phone number."
        override val errorCode = "INVALID_PHONE_NUMBER"
        override val isRetryable = false
        override val shouldReport = false
    }
    
    data class MessageTooLong(
        val length: Int,
        val maxLength: Int
    ) : AppException("Message too long: $length characters (max: $maxLength)") {
        override val userMessage = "Message is too long. Please shorten it and try again."
        override val errorCode = "MESSAGE_TOO_LONG"
        override val isRetryable = false
        override val shouldReport = false
    }
    
    data class EmptyMessage(
        val field: String = "message"
    ) : AppException("$field cannot be empty") {
        override val userMessage = when (field) {
            "message" -> "Please enter a message."
            "phoneNumber" -> "Please enter a phone number."
            else -> "Required field is empty."
        }
        override val errorCode = "EMPTY_FIELD"
        override val isRetryable = false
        override val shouldReport = false
    }

    // Classification errors
    data class ClassificationFailed(
        val messageContent: String,
        override val cause: Throwable? = null
    ) : AppException("Failed to classify message", cause) {
        override val userMessage = "Unable to categorize message automatically."
        override val errorCode = "CLASSIFICATION_FAILED"
        override val isRetryable = true
        override val shouldReport = true
    }

    // Network errors (for future use)
    data class NetworkUnavailable(
        override val cause: Throwable? = null
    ) : AppException("Network unavailable", cause) {
        override val userMessage = "Please check your internet connection and try again."
        override val errorCode = "NETWORK_UNAVAILABLE"
        override val isRetryable = true
        override val shouldReport = false
    }

    // Unknown/Generic errors
    data class UnknownError(
        override val cause: Throwable? = null
    ) : AppException("An unknown error occurred", cause) {
        override val userMessage = "Something went wrong. Please try again."
        override val errorCode = "UNKNOWN_ERROR"
        override val isRetryable = true
        override val shouldReport = true
    }

    companion object {
        /**
         * Converts a generic throwable to an appropriate AppException
         */
        fun from(throwable: Throwable): AppException = when (throwable) {
            is AppException -> throwable
            is SecurityException -> {
                when {
                    throwable.message?.contains("SMS", ignoreCase = true) == true -> 
                        SmsPermissionDenied(throwable)
                    throwable.message?.contains("CONTACT", ignoreCase = true) == true -> 
                        ContactPermissionDenied(throwable)
                    else -> UnknownError(throwable)
                }
            }
            is IllegalArgumentException -> {
                when {
                    throwable.message?.contains("phone", ignoreCase = true) == true -> 
                        InvalidPhoneNumber(throwable.message ?: "")
                    throwable.message?.contains("empty", ignoreCase = true) == true -> 
                        EmptyMessage()
                    else -> UnknownError(throwable)
                }
            }
            is android.database.sqlite.SQLiteException -> DatabaseError("sqlite", throwable)
            is java.io.IOException -> NetworkUnavailable(throwable)
            else -> UnknownError(throwable)
        }
        
        /**
         * Creates validation error for empty fields
         */
        fun emptyField(fieldName: String) = EmptyMessage(fieldName)
        
        /**
         * Creates validation error for invalid phone numbers
         */
        fun invalidPhoneNumber(phoneNumber: String) = InvalidPhoneNumber(phoneNumber)
        
        /**
         * Creates error for messages that are too long
         */
        fun messageTooLong(length: Int, maxLength: Int) = MessageTooLong(length, maxLength)
    }
}

/**
 * Extension function to check if an exception is retryable
 */
val Throwable.isRetryable: Boolean
    get() = when (this) {
        is AppException -> this.isRetryable
        else -> true // Assume unknown errors are retryable
    }

/**
 * Extension function to get user-friendly message
 */
val Throwable.userMessage: String
    get() = when (this) {
        is AppException -> this.userMessage
        else -> "Something went wrong. Please try again."
    }
