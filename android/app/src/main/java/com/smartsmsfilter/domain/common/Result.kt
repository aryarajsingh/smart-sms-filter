package com.smartsmsfilter.domain.common

/**
 * A generic wrapper for handling operations that can succeed or fail.
 * Provides consistent error handling across the entire application.
 */
sealed class Result<out T> {
    
    /**
     * Represents a successful operation with data
     */
    data class Success<T>(val data: T) : Result<T>()
    
    /**
     * Represents a failed operation with error information
     */
    data class Error(val exception: AppException) : Result<Nothing>()
    
    /**
     * Represents an operation in progress
     */
    object Loading : Result<Nothing>()
    
    /**
     * Returns true if the result is a success
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Returns true if the result is an error
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Returns true if the result is loading
     */
    val isLoading: Boolean
        get() = this is Loading
    
    /**
     * Returns the data if success, null otherwise
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }
    
    /**
     * Returns the data if success, or throws the exception if error
     */
    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw exception
        is Loading -> throw IllegalStateException("Cannot get data from loading state")
    }
    
    /**
     * Transforms the data if success, preserves error/loading states
     */
    inline fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> this
        is Loading -> this
    }
    
    /**
     * Flat map for chaining operations
     */
    inline fun <R> flatMap(transform: (T) -> Result<R>): Result<R> = when (this) {
        is Success -> transform(data)
        is Error -> this
        is Loading -> this
    }
    
    /**
     * Handle success and error cases
     */
    inline fun fold(
        onSuccess: (T) -> Unit = {},
        onError: (AppException) -> Unit = {},
        onLoading: () -> Unit = {}
    ) {
        when (this) {
            is Success -> onSuccess(data)
            is Error -> onError(exception)
            is Loading -> onLoading()
        }
    }
}

/**
 * Extension functions for easier Result creation
 */

/**
 * Wraps a value in a Success result
 */
fun <T> T.asSuccess(): Result<T> = Result.Success(this)

/**
 * Creates an Error result from an exception
 */
fun AppException.asError(): Result<Nothing> = Result.Error(this)

/**
 * Creates an Error result from a throwable
 */
fun Throwable.asError(): Result<Nothing> = Result.Error(AppException.from(this))

/**
 * Safely executes a block and returns a Result
 */
inline fun <T> resultOf(block: () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: AppException) {
    Result.Error(e)
} catch (e: Exception) {
    Result.Error(AppException.from(e))
}

/**
 * Safely executes a suspend block and returns a Result
 */
suspend inline fun <T> suspendResultOf(crossinline block: suspend () -> T): Result<T> = try {
    Result.Success(block())
} catch (e: AppException) {
    Result.Error(e)
} catch (e: Exception) {
    Result.Error(AppException.from(e))
}
