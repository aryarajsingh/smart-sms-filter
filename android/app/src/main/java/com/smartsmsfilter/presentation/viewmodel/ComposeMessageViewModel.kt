package com.smartsmsfilter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsmsfilter.data.contacts.Contact
import com.smartsmsfilter.data.contacts.ContactManager
import com.smartsmsfilter.data.sms.SmsInfo
import com.smartsmsfilter.data.sms.SmsSenderManager
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.repository.SmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class ComposeMessageViewModel @Inject constructor(
    private val smsSenderManager: SmsSenderManager,
    private val contactManager: ContactManager,
    private val smsRepository: SmsRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ComposeMessageUiState())
    val uiState: StateFlow<ComposeMessageUiState> = _uiState.asStateFlow()
    
    // Contact search results - optimized for performance
    val filteredContacts = _uiState
        .map { it.recipientQuery }
        .debounce(300) // Wait 300ms after user stops typing
        .distinctUntilChanged()
        .filter { it.length >= 2 || it.isBlank() } // Only search with 2+ chars to reduce queries
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                contactManager.searchContacts(query)
                    .catch { emit(emptyList()) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    fun updateRecipientQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            recipientQuery = query
        )
    }
    
    fun setRecipient(contact: Contact) {
        _uiState.value = _uiState.value.copy(
            selectedRecipient = contact,
            recipientQuery = contact.name
        )
    }
    
    fun clearRecipient() {
        _uiState.value = _uiState.value.copy(
            selectedRecipient = null,
            recipientQuery = ""
        )
    }
    
    fun updateMessageText(text: String) {
        val smsInfo = smsSenderManager.calculateSmsInfo(text)
        _uiState.value = _uiState.value.copy(
            messageText = text,
            smsInfo = smsInfo
        )
    }
    
    fun sendMessage() {
        val currentState = _uiState.value
        val recipient = currentState.selectedRecipient
        val message = currentState.messageText
        
        if (recipient == null || message.isBlank()) {
            _uiState.value = currentState.copy(
                error = "Please select a recipient and enter a message"
            )
            return
        }
        
        _uiState.value = currentState.copy(isSending = true)
        
        viewModelScope.launch {
            try {
                // Send the SMS
                val result = smsSenderManager.sendSms(
                    phoneNumber = recipient.phoneNumber,
                    message = message
                )
                
                result.fold(
onSuccess = { _ ->
                        // Save sent message to database
                        val normalized = com.smartsmsfilter.ui.utils.normalizePhoneNumber(recipient.phoneNumber)
                        val sentMessage = SmsMessage(
                            sender = normalized, // Store counterparty address, not "You"
                            content = message,
                            timestamp = Date(),
                            category = MessageCategory.INBOX,
                            isRead = true, // Sent messages are considered "read"
                            threadId = normalized,
                            isOutgoing = true
                        )
                        
                        smsRepository.insertMessage(sentMessage)
                        
                        _uiState.value = _uiState.value.copy(
                            isSending = false,
                            message = "Message sent successfully!",
                            messageText = "", // Clear message after sending
                            smsInfo = null
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isSending = false,
                            error = "Failed to send message: ${exception.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = "Unexpected error: ${e.message}"
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
}

data class ComposeMessageUiState(
    val recipientQuery: String = "",
    val selectedRecipient: Contact? = null,
    val messageText: String = "",
    val smsInfo: SmsInfo? = null,
    val isSending: Boolean = false,
    val message: String? = null,
    val error: String? = null
) {
    val canSendMessage: Boolean
        get() = selectedRecipient != null && messageText.isNotBlank() && !isSending
}
