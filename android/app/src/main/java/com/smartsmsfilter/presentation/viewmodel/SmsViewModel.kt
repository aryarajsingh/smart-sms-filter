package com.smartsmsfilter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.usecase.GetAllMessagesUseCase
import com.smartsmsfilter.domain.usecase.GetMessagesByCategoryUseCase
import com.smartsmsfilter.domain.usecase.UpdateMessageCategoryUseCase
import com.smartsmsfilter.domain.usecase.MarkMessageAsReadUseCase
import com.smartsmsfilter.domain.repository.SmsRepository
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
    private val smsReader: com.smartsmsfilter.data.sms.SmsReader,
    private val smsRepository: SmsRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(SmsUiState())
    val uiState: StateFlow<SmsUiState> = _uiState.asStateFlow()
    
    // Messages by category
    val inboxMessages = getMessagesByCategoryUseCase(MessageCategory.INBOX)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val filteredMessages = getMessagesByCategoryUseCase(MessageCategory.FILTERED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val needsReviewMessages = getMessagesByCategoryUseCase(MessageCategory.NEEDS_REVIEW)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    init {
        // Load existing SMS messages from device on first launch
        loadExistingSmsMessages()
        
        // Update badge counts when messages change
        combine(
            inboxMessages,
            filteredMessages, 
            needsReviewMessages
        ) { inbox, filtered, needsReview ->
            _uiState.value = _uiState.value.copy(
                inboxCount = inbox.count { !it.isRead },
                filteredCount = filtered.count { !it.isRead },
                needsReviewCount = needsReview.count { !it.isRead }
            )
        }.launchIn(viewModelScope)
    }
    
    fun updateMessageCategory(messageId: Long, category: MessageCategory) {
        viewModelScope.launch {
            try {
                updateMessageCategoryUseCase(messageId, category)
                _uiState.value = _uiState.value.copy(
                    message = "Message moved to ${category.name.lowercase()}"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update message: ${e.message}"
                )
            }
        }
    }
    
    fun markAsRead(messageId: Long) {
        viewModelScope.launch {
            try {
                markMessageAsReadUseCase(messageId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to mark as read: ${e.message}"
                )
            }
        }
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    private fun loadExistingSmsMessages() {
        viewModelScope.launch {
            try {
                smsReader.loadRecentSmsMessages(50).collect { messages ->
                    // Save messages to database if they don't exist
                    messages.forEach { message ->
                        try {
                            smsRepository.insertMessage(message)
                        } catch (e: Exception) {
                            // Message might already exist, ignore
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load existing messages: ${e.message}"
                )
            }
        }
    }
}

data class SmsUiState(
    val inboxCount: Int = 0,
    val filteredCount: Int = 0,
    val needsReviewCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)
