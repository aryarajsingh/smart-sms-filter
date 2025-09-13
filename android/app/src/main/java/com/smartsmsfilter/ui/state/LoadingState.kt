package com.smartsmsfilter.ui.state

/**
 * Standardized loading states for all operations across the app
 */
sealed class LoadingState {
    /**
     * No operation in progress
     */
    object Idle : LoadingState()
    
    /**
     * Operation starting
     */
    object Starting : LoadingState()
    
    /**
     * Operation in progress
     * @param progress Optional progress from 0.0 to 1.0, null for indeterminate
     * @param message Optional status message
     * @param canCancel Whether the operation can be cancelled
     */
    data class Loading(
        val progress: Float? = null,
        val message: String? = null,
        val canCancel: Boolean = false
    ) : LoadingState()
    
    /**
     * Operation completed successfully
     * @param message Optional success message
     */
    data class Success(
        val message: String? = null
    ) : LoadingState()
    
    /**
     * Operation failed with error
     * @param error The error that occurred
     * @param canRetry Whether the operation can be retried
     */
    data class Error(
        val error: Throwable,
        val canRetry: Boolean = true
    ) : LoadingState()
}

/**
 * Extension functions for convenient state checking
 */
val LoadingState.isLoading: Boolean
    get() = this is LoadingState.Loading || this is LoadingState.Starting

val LoadingState.isIdle: Boolean
    get() = this is LoadingState.Idle

val LoadingState.isSuccess: Boolean
    get() = this is LoadingState.Success

val LoadingState.isError: Boolean
    get() = this is LoadingState.Error

val LoadingState.canCancel: Boolean
    get() = (this as? LoadingState.Loading)?.canCancel == true

val LoadingState.progress: Float?
    get() = (this as? LoadingState.Loading)?.progress

val LoadingState.message: String?
    get() = when (this) {
        is LoadingState.Loading -> message
        is LoadingState.Success -> message
        is LoadingState.Error -> error.message
        else -> null
    }

/**
 * Common loading operations for standardized messaging
 */
enum class LoadingOperation(val defaultMessage: String) {
    LOADING_MESSAGES("Loading messages..."),
    SENDING_MESSAGE("Sending message..."),
    CLASSIFYING_MESSAGE("Analyzing message..."),
    STARRING_MESSAGE("Updating starred status..."),
    MOVING_MESSAGE("Moving message..."),
    DELETING_MESSAGE("Deleting message..."),
    ARCHIVING_MESSAGE("Archiving message..."),
    BULK_OPERATION("Processing messages..."),
    REFRESHING("Refreshing..."),
    LOADING_CONTACTS("Loading contacts..."),
    SYNCING_DATA("Syncing data..."),
    SAVING_PREFERENCES("Saving preferences...")
}