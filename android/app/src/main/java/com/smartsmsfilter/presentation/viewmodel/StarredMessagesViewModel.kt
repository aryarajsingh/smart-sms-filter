package com.smartsmsfilter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsmsfilter.domain.model.StarredSenderGroup
import com.smartsmsfilter.domain.usecase.GetStarredMessagesUseCase
import com.smartsmsfilter.domain.usecase.ToggleMessageStarUseCase
import com.smartsmsfilter.domain.repository.SmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StarredMessagesViewModel @Inject constructor(
    private val getStarredMessagesUseCase: GetStarredMessagesUseCase,
    private val toggleMessageStarUseCase: ToggleMessageStarUseCase,
    private val repository: SmsRepository
) : ViewModel() {

    private val _starredSenderGroups = MutableStateFlow<List<StarredSenderGroup>>(emptyList())
    val starredSenderGroups: StateFlow<List<StarredSenderGroup>> = _starredSenderGroups.asStateFlow()

    private val _uiState = MutableStateFlow(StarredMessagesUiState())
    val uiState: StateFlow<StarredMessagesUiState> = _uiState.asStateFlow()

    fun loadStarredMessages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                getStarredMessagesUseCase.getStarredMessagesBySender().collect { senderGroups ->
                    _starredSenderGroups.value = senderGroups
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load starred messages: ${e.message}"
                )
            }
        }
    }

    fun unstarAllFromSender(sender: String) {
        viewModelScope.launch {
            try {
                // Get all starred messages from this sender first
                val senderMessages = _starredSenderGroups.value.find { it.sender == sender }
                
                if (senderMessages != null) {
                    getStarredMessagesUseCase.getStarredMessagesForSender(sender).collect { messages ->
                        var successCount = 0
                        var failedCount = 0
                        
                        // Unstar each message using repository directly
                        messages.forEach { starredMessage ->
                            try {
                                val result = repository.unstarMessage(starredMessage.messageId)
                                result.fold(
                                    onSuccess = { successCount++ },
                                    onError = { failedCount++ }
                                )
                            } catch (e: Exception) {
                                failedCount++
                            }
                        }
                        
                        if (failedCount > 0) {
                            _uiState.value = _uiState.value.copy(
                                error = "Failed to unstar $failedCount messages"
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                message = "Removed stars from $successCount messages"
                            )
                        }
                        
                        // Refresh the list
                        loadStarredMessages()
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to unstar messages: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

data class StarredMessagesUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)