package com.smartsmsfilter.ui.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized loading state manager for ViewModels
 * Provides consistent loading state management across all operations
 */
class LoadingStateManager {
    
    // Map of operation keys to loading states
    private val _loadingStates = MutableStateFlow<Map<String, LoadingState>>(emptyMap())
    val loadingStates: StateFlow<Map<String, LoadingState>> = _loadingStates.asStateFlow()
    
    // Global loading state - true if any operation is loading
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Main operation loading state (for primary operations)
    private val _mainLoadingState = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val mainLoadingState: StateFlow<LoadingState> = _mainLoadingState.asStateFlow()
    
    /**
     * Set loading state for a specific operation
     */
    fun setLoadingState(key: String, state: LoadingState) {
        val currentStates = _loadingStates.value.toMutableMap()
        
        if (state is LoadingState.Idle) {
            currentStates.remove(key)
        } else {
            currentStates[key] = state
        }
        
        _loadingStates.value = currentStates
        updateGlobalState()
    }
    
    /**
     * Set the main loading state (for primary operations like loading messages)
     */
    fun setMainLoadingState(state: LoadingState) {
        _mainLoadingState.value = state
        updateGlobalState()
    }
    
    /**
     * Get loading state for a specific operation
     */
    fun getLoadingState(key: String): LoadingState {
        return _loadingStates.value[key] ?: LoadingState.Idle
    }
    
    /**
     * Check if a specific operation is loading
     */
    fun isOperationLoading(key: String): Boolean {
        return getLoadingState(key).isLoading
    }
    
    /**
     * Clear all loading states
     */
    fun clearAll() {
        _loadingStates.value = emptyMap()
        _mainLoadingState.value = LoadingState.Idle
        _isLoading.value = false
    }
    
    /**
     * Clear loading state for a specific operation
     */
    fun clearOperation(key: String) {
        setLoadingState(key, LoadingState.Idle)
    }
    
    /**
     * Execute an operation with automatic loading state management
     */
    suspend inline fun <T> withLoadingState(
        key: String,
        operation: LoadingOperation,
        canCancel: Boolean = false,
        crossinline action: suspend () -> T
    ): Result<T> {
        return try {
            setLoadingState(key, LoadingState.Loading(
                message = operation.defaultMessage,
                canCancel = canCancel
            ))
            
            val result = action()
            setLoadingState(key, LoadingState.Success())
            Result.success(result)
        } catch (e: Exception) {
            setLoadingState(key, LoadingState.Error(e, canRetry = true))
            Result.failure(e)
        }
    }
    
    /**
     * Execute an operation with progress tracking
     */
    suspend inline fun <T> withProgressiveLoadingState(
        key: String,
        operation: LoadingOperation,
        canCancel: Boolean = false,
        crossinline action: suspend (updateProgress: (Float) -> Unit) -> T
    ): Result<T> {
        return try {
            setLoadingState(key, LoadingState.Loading(
                progress = 0f,
                message = operation.defaultMessage,
                canCancel = canCancel
            ))
            
            val updateProgress = { progress: Float ->
                setLoadingState(key, LoadingState.Loading(
                    progress = progress,
                    message = operation.defaultMessage,
                    canCancel = canCancel
                ))
            }
            
            val result = action(updateProgress)
            setLoadingState(key, LoadingState.Success())
            Result.success(result)
        } catch (e: Exception) {
            setLoadingState(key, LoadingState.Error(e, canRetry = true))
            Result.failure(e)
        }
    }
    
    /**
     * Execute a main operation (updates main loading state)
     */
    suspend inline fun <T> withMainLoadingState(
        operation: LoadingOperation,
        canCancel: Boolean = false,
        crossinline action: suspend () -> T
    ): Result<T> {
        return try {
            setMainLoadingState(LoadingState.Loading(
                message = operation.defaultMessage,
                canCancel = canCancel
            ))
            
            val result = action()
            setMainLoadingState(LoadingState.Success())
            Result.success(result)
        } catch (e: Exception) {
            setMainLoadingState(LoadingState.Error(e, canRetry = true))
            Result.failure(e)
        }
    }
    
    private fun updateGlobalState() {
        val anyLoading = _mainLoadingState.value.isLoading || 
                         _loadingStates.value.values.any { it.isLoading }
        _isLoading.value = anyLoading
    }
    
    companion object {
        // Common operation keys
        const val LOAD_MESSAGES = "load_messages"
        const val SEND_MESSAGE = "send_message"
        const val STAR_MESSAGE = "star_message"
        const val MOVE_MESSAGE = "move_message"
        const val DELETE_MESSAGE = "delete_message"
        const val ARCHIVE_MESSAGE = "archive_message"
        const val BULK_OPERATION = "bulk_operation"
        const val REFRESH = "refresh"
        const val SYNC = "sync"
        const val LOAD_CONTACTS = "load_contacts"
        const val SAVE_PREFERENCES = "save_preferences"
    }
}

/**
 * Extension functions for common ViewModel patterns
 */

/**
 * Execute an operation with automatic loading state handling
 */
suspend inline fun <T> LoadingStateManager.executeOperation(
    key: String,
    operation: LoadingOperation,
    canCancel: Boolean = false,
    crossinline block: suspend () -> T
): T? {
    return withLoadingState(key, operation, canCancel) {
        block()
    }.getOrNull()
}

/**
 * Execute a main operation with automatic loading state handling
 */
suspend inline fun <T> LoadingStateManager.executeMainOperation(
    operation: LoadingOperation,
    canCancel: Boolean = false,
    crossinline block: suspend () -> T
): T? {
    return withMainLoadingState(operation, canCancel) {
        block()
    }.getOrNull()
}