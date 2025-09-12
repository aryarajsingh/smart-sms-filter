package com.smartsmsfilter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.usecase.GetAllMessagesUseCase
import com.smartsmsfilter.domain.usecase.GetMessagesByCategoryUseCase
import com.smartsmsfilter.domain.usecase.UpdateMessageCategoryUseCase
import com.smartsmsfilter.domain.usecase.MarkMessageAsReadUseCase
import com.smartsmsfilter.domain.usecase.MessageManagementUseCase
import com.smartsmsfilter.domain.repository.SmsRepository
import com.smartsmsfilter.domain.common.Result
import com.smartsmsfilter.domain.common.userMessage
import com.smartsmsfilter.ui.state.MessageSelectionState
import com.smartsmsfilter.ui.state.MessageTab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SmsViewModel @Inject constructor(
    private val getAllMessagesUseCase: GetAllMessagesUseCase,
    private val getMessagesByCategoryUseCase: GetMessagesByCategoryUseCase,
    private val updateMessageCategoryUseCase: UpdateMessageCategoryUseCase,
    private val markMessageAsReadUseCase: MarkMessageAsReadUseCase,
    private val messageManagementUseCase: MessageManagementUseCase,
    private val smsReader: com.smartsmsfilter.data.sms.SmsReader,
    private val smsRepository: SmsRepository,
    private val explainMessageUseCase: com.smartsmsfilter.domain.usecase.ExplainMessageUseCase
) : ViewModel() {
    
    // Undo support
    private var lastOperation: LastOperation? = null
    
    // UI State
    private val _uiState = MutableStateFlow(SmsUiState())
    val uiState: StateFlow<SmsUiState> = _uiState.asStateFlow()
    
    // Selection State
    val selectionState = MessageSelectionState()
    
    // Messages by category
    val inboxMessages = getMessagesByCategoryUseCase(MessageCategory.INBOX)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val spamMessages = getMessagesByCategoryUseCase(MessageCategory.SPAM)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val reviewMessages = getMessagesByCategoryUseCase(MessageCategory.NEEDS_REVIEW)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    init {
        // Load existing SMS messages from device on first launch
        loadExistingSmsMessages()
        
        // Update badge counts when messages change
        combine(
            inboxMessages,
            spamMessages, 
            reviewMessages
        ) { inbox, spam, review ->
            _uiState.value = _uiState.value.copy(
                inboxCount = inbox.count { !it.isRead },
                spamCount = spam.count { !it.isRead },
                reviewCount = review.count { !it.isRead }
            )
        }.launchIn(viewModelScope)
    }
    
    fun updateMessageCategory(messageId: Long, category: MessageCategory) {
        viewModelScope.launch {
            val result = updateMessageCategoryUseCase(messageId, category)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        message = "Message moved to ${category.name.lowercase()}"
                    )
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.userMessage
                    )
                }
            )
        }
    }
    
    fun markAsRead(messageId: Long) {
        viewModelScope.launch {
            val result = markMessageAsReadUseCase(messageId)
            result.fold(
                onError = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.userMessage
                    )
                }
            )
        }
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    // Message Selection Actions - now with tab support
    fun onMessageLongPress(tab: MessageTab, messageId: Long) {
        selectionState.enterSelectionMode(tab)
        selectionState.toggleMessageSelection(tab, messageId)
    }
    
    fun onMessageSelectionToggle(tab: MessageTab, messageId: Long) {
        selectionState.toggleMessageSelection(tab, messageId)
    }
    
    fun clearSelection(tab: MessageTab) {
        selectionState.clearSelection(tab)
    }
    
    // Batch Message Management - now with tab support
    fun archiveSelectedMessages(tab: MessageTab) {
        val selectedIds = selectionState.getSelectedMessageIds(tab)
        if (selectedIds.isNotEmpty()) {
            viewModelScope.launch {
                val result = messageManagementUseCase.archiveMessages(selectedIds)
                result.fold(
                    onSuccess = {
                        selectionState.clearSelection(tab)
                        _uiState.value = _uiState.value.copy(
                            message = "${selectedIds.size} message(s) archived"
                        )
                    },
                    onError = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.userMessage
                        )
                    }
                )
            }
        }
    }
    
    fun deleteSelectedMessages(tab: MessageTab) {
        val selectedIds = selectionState.getSelectedMessageIds(tab)
        if (selectedIds.isNotEmpty()) {
            viewModelScope.launch {
                val result = messageManagementUseCase.deleteMessages(selectedIds)
                result.fold(
                    onSuccess = {
                        lastOperation = LastOperation(OperationType.Delete, selectedIds, null)
                        selectionState.clearSelection(tab)
                        _uiState.value = _uiState.value.copy(
                            message = "${selectedIds.size} message(s) deleted"
                        )
                    },
                    onError = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.userMessage
                        )
                    }
                )
            }
        }
    }
    
    fun markSelectedAsImportant(tab: MessageTab) {
        val selectedIds = selectionState.getSelectedMessageIds(tab)
        android.util.Log.d("SmsViewModel", "markSelectedAsImportant called with ${selectedIds.size} messages on tab $tab")
        if (selectedIds.isNotEmpty()) {
            viewModelScope.launch {
                // Mark each message as important individually
                var hasError = false
                selectedIds.forEach { messageId ->
                    android.util.Log.d("SmsViewModel", "Marking message $messageId as important")
                    val result = messageManagementUseCase.markAsImportant(messageId, true)
                    if (result.isError) {
                        hasError = true
                        android.util.Log.e("SmsViewModel", "Error marking message $messageId as important: ${(result as Result.Error).exception.message}")
                        _uiState.value = _uiState.value.copy(
                            error = (result as Result.Error).exception.userMessage
                        )
                        return@forEach
                    } else {
                        android.util.Log.d("SmsViewModel", "Successfully marked message $messageId as important")
                    }
                }
                
                // If marking as important from SPAM tab, also move messages to INBOX
                if (!hasError && tab == MessageTab.SPAM) {
                    android.util.Log.d("SmsViewModel", "Moving spam messages to inbox after marking as important")
                    val moveResult = messageManagementUseCase.moveToCategoryBatch(selectedIds, MessageCategory.INBOX)
                    moveResult.fold(
                        onSuccess = {
                            android.util.Log.d("SmsViewModel", "Successfully moved ${selectedIds.size} messages from SPAM to INBOX")
                            selectionState.clearSelection(tab)
                            _uiState.value = _uiState.value.copy(
                                message = "${selectedIds.size} message(s) marked as important and moved to inbox"
                            )
                        },
                        onError = { error ->
                            android.util.Log.e("SmsViewModel", "Error moving messages to inbox after marking important: ${error.message}")
                            // Still clear selection but show different message
                            selectionState.clearSelection(tab)
                            _uiState.value = _uiState.value.copy(
                                message = "${selectedIds.size} message(s) marked as important (but failed to move to inbox)",
                                error = error.userMessage
                            )
                        }
                    )
                } else if (!hasError) {
                    // For non-spam messages, just show the important message
                    selectionState.clearSelection(tab)
                    _uiState.value = _uiState.value.copy(
                        message = "${selectedIds.size} message(s) marked as important"
                    )
                    android.util.Log.d("SmsViewModel", "All ${selectedIds.size} messages marked as important successfully")
                }
            }
        }
    }
    
    fun moveSelectedToCategory(tab: MessageTab, category: MessageCategory) {
        val selectedIds = selectionState.getSelectedMessageIds(tab)
        android.util.Log.d("SmsViewModel", "moveSelectedToCategory called: ${selectedIds.size} messages from tab $tab to $category")
        android.util.Log.d("SmsViewModel", "Selected message IDs: $selectedIds")
        
        if (selectedIds.isNotEmpty()) {
            viewModelScope.launch {
                android.util.Log.d("SmsViewModel", "Starting batch move operation...")
                // Capture previous categories for undo
                val prevMap = mutableMapOf<Long, MessageCategory>()
                selectedIds.forEach { id ->
                    try { smsRepository.getMessageById(id)?.let { prevMap[id] = it.category } } catch (_: Exception) {}
                }
                val result = messageManagementUseCase.moveToCategoryBatch(selectedIds, category)
                result.fold(
                    onSuccess = {
                        android.util.Log.d("SmsViewModel", "Batch move successful, clearing selection")
                        lastOperation = LastOperation(OperationType.MoveCategory, selectedIds, prevMap)
                        selectionState.clearSelection(tab)
                        _uiState.value = _uiState.value.copy(
                            message = "${selectedIds.size} message(s) moved to ${category.name.lowercase()}"
                        )
                        android.util.Log.d("SmsViewModel", "Successfully moved ${selectedIds.size} messages to $category")
                    },
                    onError = { error ->
                        android.util.Log.e("SmsViewModel", "Error in batch move operation: ${error.message}", error)
                        _uiState.value = _uiState.value.copy(
                            error = error.userMessage
                        )
                    }
                )
            }
        } else {
            android.util.Log.w("SmsViewModel", "No messages selected for category move")
        }
    }
    
    // Individual message actions
    fun archiveMessage(messageId: Long) {
        viewModelScope.launch {
            try {
                messageManagementUseCase.archiveMessages(listOf(messageId))
                _uiState.value = _uiState.value.copy(
                    message = "Message archived"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to archive message: ${e.message}"
                )
            }
        }
    }
    
    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            try {
                messageManagementUseCase.deleteMessage(messageId)
                _uiState.value = _uiState.value.copy(
                    message = "Message deleted"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete message: ${e.message}"
                )
            }
        }
    }
    
    // Undo last operation
    fun undoLastOperation() {
        val op = lastOperation ?: return
        viewModelScope.launch {
            when (op.type) {
                OperationType.Delete -> {
                    smsRepository.restoreSoftDeletedMessages(op.affectedIds)
                    _uiState.value = _uiState.value.copy(message = "Restored ${op.affectedIds.size} message(s)")
                }
                OperationType.MoveCategory -> {
                    op.previousCategories?.forEach { (id, prevCat) ->
                        updateMessageCategoryUseCase(id, prevCat)
                    }
                    _uiState.value = _uiState.value.copy(message = "Move undone")
                }
            }
            lastOperation = null
        }
    }
    
    // Explainability (Why?)
    suspend fun fetchWhyForMessage(messageId: Long) {
        try {
            val reason = smsRepository.getLatestClassificationReason(messageId)
            val display = if (!reason.isNullOrBlank()) "Why: $reason" else "Why information unavailable"
            _uiState.value = _uiState.value.copy(message = display)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(message = "Why information unavailable")
        }
    }

    // Returns only the reasons (split) for Why?
    suspend fun getWhyReasons(messageId: Long): List<String> {
        return try {
            val raw = smsRepository.getLatestClassificationReason(messageId)
            if (!raw.isNullOrBlank()) {
                raw.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                // Fallback: generate reasons on-demand without storing
                val msg = smsRepository.getMessageById(messageId)
                if (msg != null) explainMessageUseCase(msg) else emptyList()
            }
        } catch (e: Exception) {
            // Fallback on any error
            val msg = try { smsRepository.getMessageById(messageId) } catch (_: Exception) { null }
            if (msg != null) explainMessageUseCase(msg) else emptyList()
        }
    }

    // User correction from Why? sheet
    fun correctClassification(messageId: Long, targetCategory: MessageCategory, reasons: List<String>) {
        viewModelScope.launch {
            // Capture previous category for undo support
            val prev = smsRepository.getMessageById(messageId)?.category
            val result = updateMessageCategoryUseCase(messageId, targetCategory)
            result.fold(
                onSuccess = {
                    // Log user feedback audit (non-blocking on UI feel)
                    try { smsRepository.insertUserFeedbackAudit(messageId, targetCategory, reasons) } catch (_: Exception) {}
                    // Prepare undo
                    lastOperation = LastOperation(
                        type = OperationType.MoveCategory,
                        affectedIds = listOf(messageId),
                        previousCategories = prev?.let { mapOf(messageId to it) }
                    )
                    _uiState.value = _uiState.value.copy(
                        message = "Message moved to ${targetCategory.name.lowercase()}"
                    )
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.userMessage
                    )
                }
            )
        }
    }

    // Public function to reload SMS messages - useful for permission changes or manual refresh
    fun reloadSmsMessages() {
        loadExistingSmsMessages()
    }
    
    private fun loadExistingSmsMessages() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                smsReader.loadRecentSmsMessages(100).collect { messages ->
                    // Save messages to database if they don't exist
                    messages.forEach { message ->
                        try {
                            smsRepository.insertMessage(message)
                        } catch (e: Exception) {
                            // Message might already exist, ignore
                        }
                    }
                }
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load existing messages: ${e.message}",
                    isLoading = false
                )
            }
        }
    }
}

enum class OperationType { Delete, MoveCategory }

data class LastOperation(
    val type: OperationType,
    val affectedIds: List<Long>,
    val previousCategories: Map<Long, MessageCategory>?
)

data class SmsUiState(
    val inboxCount: Int = 0,
    val spamCount: Int = 0,
    val reviewCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)
