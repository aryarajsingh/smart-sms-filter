package com.smartsmsfilter.domain.common

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages coroutine scopes and prevents memory leaks
 */
@Singleton
class CoroutineScopeManager @Inject constructor() {
    
    /**
     * Background scope for long-running operations
     */
    val backgroundScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, exception ->
            // Handle exception but don't crash the app - silently continue
        }
    )
    
    /**
     * UI scope for UI-related operations (typically handled by ViewModels)
     */
    val uiScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main + CoroutineExceptionHandler { _, exception ->
            // Handle exception but don't crash the app - silently continue
        }
    )
    
    /**
     * Creates a safe flow that handles exceptions and runs on IO dispatcher
     */
    fun <T> createSafeFlow(
        flowBuilder: suspend FlowCollector<T>.() -> Unit
    ): Flow<T> = kotlinx.coroutines.flow.flow(flowBuilder)
        .catch { e ->
            emit(Result.Error(AppException.from(e)) as T)
        }
        .flowOn(Dispatchers.IO)
    
    /**
     * Executes a suspend function safely with proper error handling
     */
    suspend fun <T> executeSafely(
        operation: suspend () -> T
    ): Result<T> = try {
        Result.Success(operation())
    } catch (e: CancellationException) {
        // Re-throw cancellation exceptions to preserve coroutine cancellation
        throw e
    } catch (e: AppException) {
        Result.Error(e)
    } catch (e: Exception) {
        Result.Error(AppException.from(e))
    }
    
    /**
     * Launches a coroutine with proper error handling
     */
    fun launchSafely(
        scope: CoroutineScope = backgroundScope,
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        block: suspend CoroutineScope.() -> Unit
    ): Job = scope.launch(context, start) {
        try {
            block()
        } catch (e: CancellationException) {
            // Re-throw cancellation exceptions
            throw e
        } catch (e: Exception) {
            // Handle exception silently
        }
    }
    
    /**
     * Creates a timeout wrapper for operations that might hang
     */
    suspend fun <T> withTimeout(
        timeoutMillis: Long = 30_000, // 30 seconds default
        operation: suspend () -> T
    ): Result<T> = try {
        kotlinx.coroutines.withTimeout(timeoutMillis) {
            Result.Success(operation())
        }
    } catch (e: TimeoutCancellationException) {
        Result.Error(AppException.UnknownError(e))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(AppException.from(e))
    }
    
    /**
     * Cancels all scopes when app is destroyed
     */
    fun cancelAll() {
        backgroundScope.cancel("App destroyed")
        uiScope.cancel("App destroyed")
    }
    
    /**
     * Checks if scopes are active
     */
    fun isActive(): Boolean = backgroundScope.isActive && uiScope.isActive
}

/**
 * Extension functions for easier usage
 */

/**
 * Safely collect a flow with proper error handling
 */
suspend fun <T> Flow<T>.collectSafely(
    onError: (AppException) -> Unit = {},
    onSuccess: (T) -> Unit
) {
    this.catch { e ->
        val appException = if (e is AppException) e else AppException.from(e)
        onError(appException)
    }.collect { value ->
        onSuccess(value)
    }
}

/**
 * Transform a flow to handle Results properly
 */
fun <T> Flow<T>.asResultFlow(): Flow<Result<T>> = kotlinx.coroutines.flow.flow {
    emit(Result.Loading)
    this@asResultFlow.collect { value ->
        emit(Result.Success(value))
    }
}.catch { e ->
    val appException = if (e is AppException) e else AppException.from(e)
    emit(Result.Error(appException))
}
