package com.smartsmsfilter.domain.common

/**
 * Represents a user-facing error with actionable feedback
 */
data class UiError(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val canRetry: Boolean = false,
    val isRecoverable: Boolean = true
)

/**
 * Extension functions to convert AppException to UiError with better UX
 */
fun AppException.toUiError(
    retryAction: (() -> Unit)? = null
): UiError = when (this) {
    is AppException.SmsPermissionDenied -> UiError(
        message = userMessage,
        actionLabel = "Grant Permission",
        onAction = retryAction,
        canRetry = isRetryable,
        isRecoverable = true
    )
    
    is AppException.SmsSendFailed -> UiError(
        message = "Failed to send message to $phoneNumber. Please check the number and try again.",
        actionLabel = "Retry",
        onAction = retryAction,
        canRetry = isRetryable,
        isRecoverable = true
    )
    
    is AppException.SmsReadFailed -> UiError(
        message = "Unable to load messages. Please check your permissions and try again.",
        actionLabel = "Retry",
        onAction = retryAction,
        canRetry = isRetryable,
        isRecoverable = true
    )
    
    is AppException.ContactPermissionDenied -> UiError(
        message = userMessage,
        actionLabel = "Grant Permission",
        onAction = retryAction,
        canRetry = isRetryable,
        isRecoverable = true
    )
    
    is AppException.ContactLoadFailed -> UiError(
        message = "Unable to load contacts. Showing phone numbers instead.",
        actionLabel = "Retry",
        onAction = retryAction,
        canRetry = isRetryable,
        isRecoverable = true
    )
    
    is AppException.DatabaseError -> UiError(
        message = "A storage error occurred. Please try again or restart the app if the problem persists.",
        actionLabel = "Retry",
        onAction = retryAction,
        canRetry = isRetryable,
        isRecoverable = true
    )
    
    is AppException.DatabaseCorrupted -> UiError(
        message = userMessage,
        actionLabel = "Restart App",
        onAction = null,
        canRetry = false,
        isRecoverable = false
    )
    
    is AppException.NetworkUnavailable -> UiError(
        message = "No internet connection. Showing cached data where available.",
        actionLabel = "Retry",
        onAction = retryAction,
        canRetry = isRetryable,
        isRecoverable = true
    )
    
    is AppException.InvalidPhoneNumber -> UiError(
        message = "Please enter a valid phone number.",
        actionLabel = null,
        onAction = null,
        canRetry = false,
        isRecoverable = true
    )
    
    is AppException.MessageTooLong -> UiError(
        message = "Message is too long ($length/$maxLength characters). Please shorten it.",
        actionLabel = null,
        onAction = null,
        canRetry = false,
        isRecoverable = true
    )
    
    is AppException.EmptyMessage -> UiError(
        message = userMessage,
        actionLabel = null,
        onAction = null,
        canRetry = false,
        isRecoverable = true
    )
    
    is AppException.ClassificationFailed -> UiError(
        message = "Unable to categorize message automatically. It will be placed in Review.",
        actionLabel = "Continue",
        onAction = retryAction,
        canRetry = isRetryable,
        isRecoverable = true
    )
    
    is AppException.UnknownError -> UiError(
        message = "Something went wrong. Please try again.",
        actionLabel = "Retry",
        onAction = retryAction,
        canRetry = isRetryable,
        isRecoverable = true
    )
}

/**
 * Extension function for Throwable to create UiError
 */
fun Throwable.toUiError(retryAction: (() -> Unit)? = null): UiError {
    val appException = when (this) {
        is AppException -> this
        else -> AppException.from(this)
    }
    return appException.toUiError(retryAction)
}