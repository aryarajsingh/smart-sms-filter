package com.smartsmsfilter.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsmsfilter.data.contacts.ContactManager
import com.smartsmsfilter.data.sms.SmsSenderManager
import com.smartsmsfilter.data.sms.SmsInfo
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.repository.SmsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ThreadViewModel @Inject constructor(
    private val smsRepository: SmsRepository,
    private val contactManager: ContactManager,
    private val smsSenderManager: SmsSenderManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<SmsMessage>>(emptyList())
    val messages: StateFlow<List<SmsMessage>> = _messages.asStateFlow()

    private val _contactName = MutableStateFlow<String?>(null)
    val contactName: StateFlow<String?> = _contactName.asStateFlow()
    
    private val _uiState = MutableStateFlow(ThreadUiState())
    val uiState: StateFlow<ThreadUiState> = _uiState.asStateFlow()
    
    private var currentAddress: String = ""

    fun loadThread(address: String) {
        currentAddress = address
        viewModelScope.launch {
            // Load messages for this address
            smsRepository.getMessagesByAddress(address).collect { messageList ->
                _messages.value = messageList
            }
        }
        
        viewModelScope.launch {
            // Load contact name for this address
            val contact = contactManager.getContactByPhoneNumber(address)
            _contactName.value = contact?.name
        }
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
        val message = currentState.messageText
        
        if (currentAddress.isBlank() || message.isBlank()) {
            _uiState.value = currentState.copy(
                error = "Cannot send empty message"
            )
            return
        }
        
        _uiState.value = currentState.copy(isSending = true)
        
        viewModelScope.launch {
            try {
                val result = smsSenderManager.sendSms(
                    phoneNumber = currentAddress,
                    message = message
                )
                
                result.fold(
onSuccess = { _ ->
                        // Save sent message to database
                        val sentMessage = SmsMessage(
                            sender = "You", // Indicates outgoing message
                            content = message,
                            timestamp = Date(),
                            category = MessageCategory.INBOX,
                            isRead = true, // Sent messages are considered "read"
                            threadId = currentAddress
                        )
                        
                        smsRepository.insertMessage(sentMessage)
                        
                        _uiState.value = _uiState.value.copy(
                            isSending = false,
                            messageText = "", // Clear message after sending
                            smsInfo = null,
                            message = "Message sent successfully!"
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

data class ThreadUiState(
    val messageText: String = "",
    val smsInfo: SmsInfo? = null,
    val isSending: Boolean = false,
    val message: String? = null,
    val error: String? = null
) {
    val canSendMessage: Boolean
        get() = messageText.isNotBlank() && !isSending
}
