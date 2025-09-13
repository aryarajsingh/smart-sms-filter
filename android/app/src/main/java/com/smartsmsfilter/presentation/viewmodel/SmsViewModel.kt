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
import com.smartsmsfilter.domain.common.UiError
import com.smartsmsfilter.domain.common.toUiError
import com.smartsmsfilter.ui.state.MessageSelectionState
import com.smartsmsfilter.ui.state.MessageTab
import com.smartsmsfilter.ui.state.LoadingState
import com.smartsmsfilter.ui.state.LoadingStateManager
import com.smartsmsfilter.ui.state.LoadingOperation
import com.smartsmsfilter.ui.state.executeMainOperation
import com.smartsmsfilter.ui.state.executeOperation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import javax.inject.Inject
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony

@HiltViewModel
class SmsViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context,
    private val getAllMessagesUseCase: GetAllMessagesUseCase,
    private val getMessagesByCategoryUseCase: GetMessagesByCategoryUseCase,
    private val updateMessageCategoryUseCase: UpdateMessageCategoryUseCase,
    private val markMessageAsReadUseCase: MarkMessageAsReadUseCase,
    private val messageManagementUseCase: MessageManagementUseCase,
    private val smsReader: com.smartsmsfilter.data.sms.SmsReader,
    private val smsRepository: SmsRepository,
    private val explainMessageUseCase: com.smartsmsfilter.domain.usecase.ExplainMessageUseCase,
    private val classificationService: com.smartsmsfilter.domain.classifier.ClassificationService,
    private val senderLearningUseCase: com.smartsmsfilter.domain.usecase.SenderLearningUseCase
) : ViewModel() {
    
    // Undo support
    private var lastOperation: LastOperation? = null
    
    // Standardized loading state management
    private val loadingStateManager = LoadingStateManager()
    val mainLoadingState = loadingStateManager.mainLoadingState
    val isAnyOperationLoading = loadingStateManager.isLoading
    
    // Expose a single state object to the UI
    private val _uiState = MutableStateFlow(SmsUiState())
    val uiState: StateFlow<SmsUiState> = _uiState.asStateFlow()
    
    // Selection State
    val selectionState = MessageSelectionState()
    
    // Private flows for raw message data - optimized with sharing to avoid multiple queries
    private val inboxMessages = getMessagesByCategoryUseCase(MessageCategory.INBOX)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)
    private val spamMessages = getMessagesByCategoryUseCase(MessageCategory.SPAM)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)
    private val reviewMessages = getMessagesByCategoryUseCase(MessageCategory.NEEDS_REVIEW)
        .shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), replay = 1)
    
    // Broadcast receiver for real-time message updates (removed - relying on reactive Flows)

    init {
        // Register ContentObserver for SMS provider changes (for background updates)
        registerSmsContentObserver()
        
        // Register broadcast receiver for new message notifications
        registerMessageBroadcastReceiver()
        
        // Load ALL messages immediately on startup
        loadAllMessagesImmediately()

        // Combine raw data streams and transform them into UI state - optimized flow operators
        combine(
            inboxMessages, 
            spamMessages, 
            reviewMessages
        ) { inbox, spam, review ->
                // Group inbox messages by sender to show as conversation threads
                // Each thread shows the latest message but contains all messages internally
                val inboxThreads = inbox
                    .groupBy {
                        com.smartsmsfilter.ui.utils.normalizePhoneNumber(it.sender)
                    }
                    .map { (sender, messages) ->
                        // Get the latest INBOX message from this sender to display in the list
                        val latestInboxMessage = messages.maxByOrNull { it.timestamp }
                        // Mark thread as unread if ANY inbox message is unread (fast heuristic)
                        val hasUnread = messages.any { !it.isRead }
                        latestInboxMessage?.copy(
                            isRead = !hasUnread,
                            content = latestInboxMessage.content
                        ) to sender
                    }
                    .mapNotNull { (message, sender) -> 
                        message?.let { it to sender }
                    }
                    // Sort by latest inbox activity (most recent message from sender)
                    .sortedByDescending { (message, _) ->
                        message.timestamp
                    }
                    .map { it.first }

                // Spam and Review show all individual messages (not grouped)
                val spamSorted = spam.sortedByDescending { it.timestamp }
                val reviewSorted = review.sortedByDescending { it.timestamp }
                
                // Update the single UI state object
                _uiState.update {
                    it.copy(
                        inboxMessages = inboxThreads,
                        spamMessages = spamSorted,
                        reviewMessages = reviewSorted,
                        inboxUnreadCount = inbox.count { !it.isRead }, // Total unread messages
                        spamTotalCount = spam.size,
                        reviewUnreadCount = review.count { !it.isRead },
                        isLoading = false
                    )
                }
            }
            .flowOn(kotlinx.coroutines.Dispatchers.Default) // Move processing to background thread
            .launchIn(viewModelScope) // Use launchIn instead of collect() for better cancellation
    }
    
    private fun refreshMessageCounts() {
        // Force a refresh by manually collecting the current state from all flows
        viewModelScope.launch {
            try {
                val inbox = inboxMessages.first()
                val spam = spamMessages.first()
                val review = reviewMessages.first()

                // Group inbox messages by sender to show as conversation threads
                val inboxThreads = inbox
                    .groupBy {
                        val normalized = com.smartsmsfilter.ui.utils.normalizePhoneNumber(it.sender)
                        normalized
                    }
                    .map { (sender, messages) ->
                        val latestInboxMessage = messages.maxByOrNull { it.timestamp }
                        val hasUnread = messages.any { !it.isRead }
                        latestInboxMessage?.copy(
                            isRead = !hasUnread,
                            content = latestInboxMessage.content
                        ) to sender
                    }
                    .mapNotNull { (message, sender) -> 
                        message?.let { it to sender }
                    }
                    .sortedByDescending { (message, _) -> message.timestamp }
                    .map { it.first }

                // Spam and Review show all individual messages (not grouped)
                val spamSorted = spam.sortedByDescending { it.timestamp }
                val reviewSorted = review.sortedByDescending { it.timestamp }

                _uiState.update {
                    it.copy(
                        inboxMessages = inboxThreads,
                        spamMessages = spamSorted,
                        reviewMessages = reviewSorted,
                        inboxUnreadCount = inbox.count { !it.isRead },
                        spamTotalCount = spam.size,
                        reviewUnreadCount = review.count { !it.isRead },
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    fun updateMessageCategory(messageId: Long, category: MessageCategory) {
        viewModelScope.launch {
            val result = updateMessageCategoryUseCase(messageId, category)
            result.fold(
                onSuccess = {
                    // Force refresh of message counts
                    refreshMessageCounts()
                    showMessage("Message moved to ${category.name.lowercase()}")
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.toUiError { updateMessageCategory(messageId, category) }
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
                        error = error.toUiError { markAsRead(messageId) }
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
    
    // Helper function to show message with auto-clear
    private fun showMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
        // Auto-clear message after 4 seconds if not cleared by user action
        viewModelScope.launch {
            delay(4000)
            // Only clear if the message is still the same
            if (_uiState.value.message == message) {
                clearMessage()
            }
        }
    }
    
    // Retry the action from an error state
    fun retryFromError() {
        _uiState.value.error?.onAction?.invoke()
        clearError()
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
                val result = loadingStateManager.executeOperation(
                    key = LoadingStateManager.BULK_OPERATION,
                    operation = LoadingOperation.ARCHIVING_MESSAGE,
                    canCancel = false
                ) {
                    messageManagementUseCase.archiveMessages(selectedIds)
                }
                result?.fold(
                    onSuccess = {
                        selectionState.clearSelection(tab)
                        showMessage("${selectedIds.size} message(s) archived")
                    },
                    onError = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.toUiError { archiveSelectedMessages(tab) }
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
                val result = loadingStateManager.executeOperation(
                    key = LoadingStateManager.BULK_OPERATION,
                    operation = LoadingOperation.DELETING_MESSAGE,
                    canCancel = false
                ) {
                    messageManagementUseCase.deleteMessages(selectedIds)
                }
                result?.fold(
                    onSuccess = {
                        lastOperation = LastOperation(OperationType.Delete, selectedIds, null)
                        selectionState.clearSelection(tab)
                        showMessage("${selectedIds.size} message(s) deleted")
                    },
                    onError = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.toUiError { deleteSelectedMessages(tab) }
                        )
                    }
                )
            }
        }
    }
    
    
    fun moveSelectedToCategory(tab: MessageTab, category: MessageCategory) {
        val selectedIds = selectionState.getSelectedMessageIds(tab)
        
        if (selectedIds.isNotEmpty()) {
            viewModelScope.launch {
                
                val operationResult = loadingStateManager.executeOperation(
                    key = LoadingStateManager.BULK_OPERATION,
                    operation = LoadingOperation.MOVING_MESSAGE,
                    canCancel = false
                ) {
                    // Capture previous categories for undo
                    val prevMap = mutableMapOf<Long, MessageCategory>()
                    selectedIds.forEach { id ->
                        try { smsRepository.getMessageById(id)?.let { prevMap[id] = it.category } } catch (_: Exception) {}
                    }
                    
                    val result = messageManagementUseCase.moveToCategoryBatch(selectedIds, category)
                    Pair(result, prevMap)
                }
                operationResult?.let { (result, prevMap) ->
                    result.fold(
                        onSuccess = {
                            lastOperation = LastOperation(OperationType.MoveCategory, selectedIds, prevMap)
                            selectionState.clearSelection(tab)
                            // Force refresh of message counts after batch move
                            refreshMessageCounts()
                            showMessage("${selectedIds.size} message(s) moved to ${category.name.lowercase()}")
                        },
                        onError = { error ->
                            _uiState.value = _uiState.value.copy(
                                error = error.toUiError { moveSelectedToCategory(tab, category) }
                            )
                        }
                    )
                }
            }
        }
    }
    
    // Individual message actions
    fun archiveMessage(messageId: Long) {
        viewModelScope.launch {
            try {
                messageManagementUseCase.archiveMessages(listOf(messageId))
                showMessage("Message archived")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.toUiError { archiveMessage(messageId) }
                )
            }
        }
    }
    
    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            try {
                messageManagementUseCase.deleteMessage(messageId)
                showMessage("Message deleted")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.toUiError { deleteMessage(messageId) }
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
                    showMessage("Restored ${op.affectedIds.size} message(s)")
                }
                OperationType.MoveCategory -> {
                    op.previousCategories?.forEach { (id, prevCat) ->
                        updateMessageCategoryUseCase(id, prevCat)
                    }
                    showMessage("Move undone")
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
            showMessage(display)
        } catch (e: Exception) {
            showMessage("Why information unavailable")
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

    // Learn from user action when they move messages
    fun learnFromUserAction(messageId: Long, targetCategory: MessageCategory) {
        viewModelScope.launch {
            try {
                val message = smsRepository.getMessageById(messageId)
                message?.let { msg ->
                    // Trigger learning based on the correction
                    when (targetCategory) {
                        MessageCategory.INBOX -> senderLearningUseCase.learnFromInboxMove(msg)
                        MessageCategory.SPAM -> senderLearningUseCase.learnFromSpamMarking(msg)
                        MessageCategory.NEEDS_REVIEW -> { /* No specific learning for review */ }
                    }
                    // Also trigger classification service learning
                    classificationService.handleUserCorrection(messageId, targetCategory, "User manual correction")
                }
            } catch (e: Exception) {
                android.util.Log.w("SmsViewModel", "Failed to learn from user action", e)
            }
        }
    }
    
    // User correction from Why? sheet
    fun correctClassification(messageId: Long, targetCategory: MessageCategory, reasons: List<String>) {
        viewModelScope.launch {
            // Capture previous category and message for learning
            val message = smsRepository.getMessageById(messageId)
            val prev = message?.category
            val result = updateMessageCategoryUseCase(messageId, targetCategory)
            result.fold(
                onSuccess = {
                    // Log user feedback audit (non-blocking on UI feel)
                    try { smsRepository.insertUserFeedbackAudit(messageId, targetCategory, reasons) } catch (_: Exception) {}
                    
                    // Trigger learning from user correction (best-effort, non-blocking)
                    message?.let { msg ->
                        try {
                            // 1. Learn sender-level patterns for future messages
                            when (targetCategory) {
                                MessageCategory.INBOX -> senderLearningUseCase.learnFromInboxMove(msg)
                                MessageCategory.SPAM -> senderLearningUseCase.learnFromSpamMarking(msg)
                                MessageCategory.NEEDS_REVIEW -> { /* No specific sender learning for review */ }
                            }
                            
                            // 2. Learn content-based patterns via classification service
                            classificationService.handleUserCorrection(messageId, targetCategory, reasons.joinToString(", "))
            } catch (e: Exception) {
                // Learning failures should not break the UI flow
            }
                    }
                    
                    // Prepare undo
                    lastOperation = LastOperation(
                        type = OperationType.MoveCategory,
                        affectedIds = listOf(messageId),
                        previousCategories = prev?.let { mapOf(messageId to it) }
                    )
                    showMessage("Message moved to ${targetCategory.name.lowercase()}")
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.toUiError { correctClassification(messageId, targetCategory, reasons) }
                    )
                }
            )
        }
    }

    // Public function to reload SMS messages - useful for permission changes or manual refresh
    fun reloadSmsMessages() {
        loadAllMessagesImmediately()
    }
    
    // Get total message count for a sender (for displaying in thread)
    fun getMessageCountForSender(sender: String): Int {
        // This would need to be exposed from repository if needed
        return 0 // Placeholder
    }
    
    // Get a specific message by ID
    suspend fun getMessageById(messageId: Long): SmsMessage? {
        return try {
            smsRepository.getMessageById(messageId)
        } catch (e: Exception) {
            android.util.Log.e("SmsViewModel", "Failed to get message by ID: $messageId", e)
            null
        }
    }
    
    // Removed pull-to-refresh - messages auto-sync in real-time
    
    private fun loadAllMessagesImmediately() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                // Load ALL messages from device
                smsReader.loadAllSmsMessages().collect { messages ->
                    if (messages.isEmpty()) {
                        _uiState.update { it.copy(isLoading = false) }
                        return@collect
                    }
                    
                    // Process messages in batches for better performance
                    val batchSize = 100
                    var totalInserted = 0
                    
                    messages.chunked(batchSize).forEachIndexed { index, batch ->
                        val insertedInBatch = coroutineScope {
                            batch.map { message ->
                                async {
                                    try {
                                        // For initial sync only - check if message already exists
                                        val existing = smsRepository.getMessageById(message.id)
                                        if (existing == null) {
                                            // New message - needs classification
                                            if (!message.isOutgoing) {
                                                // Incoming messages should be classified
                                                val classification = classificationService.classifyAndStore(message)
                                                if (classification.messageId != null && classification.messageId > 0) {
                                                    1
                                                } else {
                                                    0
                                                }
                                            } else {
                                                // Outgoing messages just insert
                                                val result = smsRepository.insertMessage(message)
                                                if (result.isSuccess && result.getOrNull() ?: -1L > 0) {
                                                    1
                                                } else {
                                                    0
                                                }
                                            }
                                        } else {
                                            0 // Already exists
                                        }
                                    } catch (e: Exception) {
                                        0
                                    }
                                }
                            }.awaitAll().sum()
                        }
                        totalInserted += insertedInBatch
                    }
                    _uiState.update { it.copy(isLoading = false) }
                    
                    // Force immediate UI refresh
                    refreshMessageCounts()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.toUiError { loadAllMessagesImmediately() }) }
            }
        }
    }
    
    // Sync only new messages (for real-time updates)
    private fun syncNewMessages() {
        viewModelScope.launch {
            try {
                // Load only the most recent 50 messages for quick sync
                smsReader.loadRecentSmsMessages(50).collect { messages ->
                    messages.forEach { message ->
                        try {
                            smsRepository.insertMessage(message)
                        } catch (e: Exception) {
                            // Message might already exist, ignore
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently handle sync errors
            }
        }
    }
    
    // ContentObserver for monitoring SMS database changes
    private var smsContentObserver: ContentObserver? = null
    
    // BroadcastReceiver for new message notifications
    private val messageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.smartsmsfilter.NEW_MESSAGE_RECEIVED") {
                val messageId = intent.getLongExtra("message_id", -1L)
                val category = intent.getStringExtra("category")
                android.util.Log.d("SmsViewModel", "New message received: ID=$messageId, Category=$category")
                // Force refresh of message counts
                refreshMessageCounts()
            }
        }
    }
    
    private fun registerSmsContentObserver() {
        try {
            // Create the observer inline during registration
            smsContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    super.onChange(selfChange, uri)
                    // Note: Reactive Flows should handle real-time updates automatically
                    // ContentObserver is kept for background sync if needed
                }
            }
            
            // Register observer for SMS inbox changes
            smsContentObserver?.let { observer ->
                context.contentResolver.registerContentObserver(
                    Uri.parse("content://sms/inbox"),
                    true,
                    observer
                )
                // Also monitor all SMS changes
                context.contentResolver.registerContentObserver(
                    Uri.parse("content://sms"),
                    false,
                    observer
                )
            }
        } catch (e: Exception) {
            // Silently handle ContentObserver registration failure
        }
    }
    
    private fun registerMessageBroadcastReceiver() {
        try {
            val filter = IntentFilter("com.smartsmsfilter.NEW_MESSAGE_RECEIVED")
            context.registerReceiver(messageReceiver, filter)
        } catch (e: Exception) {
            android.util.Log.e("SmsViewModel", "Failed to register broadcast receiver", e)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Unregister content observer
        try {
            smsContentObserver?.let { observer ->
                context.contentResolver.unregisterContentObserver(observer)
            }
        } catch (e: Exception) {
            // Ignore if already unregistered
        }
        // Unregister broadcast receiver
        try {
            context.unregisterReceiver(messageReceiver)
        } catch (e: Exception) {
            // Ignore if already unregistered
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
    val inboxMessages: List<SmsMessage> = emptyList(),
    val spamMessages: List<SmsMessage> = emptyList(),
    val reviewMessages: List<SmsMessage> = emptyList(),
    val inboxUnreadCount: Int = 0,
    val spamTotalCount: Int = 0,
    val reviewUnreadCount: Int = 0,
    val isLoading: Boolean = true, // Start with loading state
    val error: UiError? = null,
    val message: String? = null
)
