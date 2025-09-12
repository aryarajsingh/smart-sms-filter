package com.smartsmsfilter.ui.state

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents different tabs/screens in the app
 */
enum class MessageTab {
    INBOX,
    SPAM,
    REVIEW
}

/**
 * Individual selection state for a single tab
 */
data class TabSelectionState(
    val isSelectionMode: Boolean = false,
    val selectedMessages: Set<Long> = emptySet(),
    val selectedCount: Int = 0
)

@Stable
class MessageSelectionState {
    // Maintain separate selection state for each tab
    private val tabStates = mutableMapOf<MessageTab, MutableStateFlow<TabSelectionState>>(
        MessageTab.INBOX to MutableStateFlow(TabSelectionState()),
        MessageTab.SPAM to MutableStateFlow(TabSelectionState()),
        MessageTab.REVIEW to MutableStateFlow(TabSelectionState())
    )
    
    // Current active tab
    private val _currentTab = MutableStateFlow(MessageTab.INBOX)
    val currentTab: StateFlow<MessageTab> = _currentTab.asStateFlow()
    
    // Get state flow for a specific tab
    fun getTabState(tab: MessageTab): StateFlow<TabSelectionState> {
        return tabStates[tab]?.asStateFlow() ?: MutableStateFlow(TabSelectionState()).asStateFlow()
    }
    
    // Set the current active tab
    fun setCurrentTab(tab: MessageTab) {
        _currentTab.value = tab
    }
    
    fun enterSelectionMode(tab: MessageTab) {
        val currentState = tabStates[tab]?.value ?: TabSelectionState()
        tabStates[tab]?.value = currentState.copy(isSelectionMode = true)
    }
    
    fun exitSelectionMode(tab: MessageTab) {
        tabStates[tab]?.value = TabSelectionState()
    }
    
    @Synchronized
    fun toggleMessageSelection(tab: MessageTab, messageId: Long) {
        val currentState = tabStates[tab]?.value ?: TabSelectionState()
        val currentSelected = currentState.selectedMessages.toMutableSet()
        
        if (currentSelected.contains(messageId)) {
            currentSelected.remove(messageId)
        } else {
            currentSelected.add(messageId)
        }
        
        val newCount = currentSelected.size
        
        // Update state
        if (currentSelected.isEmpty()) {
            // Exit selection mode if no messages selected
            tabStates[tab]?.value = TabSelectionState()
        } else {
            tabStates[tab]?.value = currentState.copy(
                selectedMessages = currentSelected.toSet(), // Create immutable copy
                selectedCount = newCount
            )
        }
    }
    
    fun selectAll(tab: MessageTab, messageIds: List<Long>) {
        if (messageIds.isNotEmpty()) {
            tabStates[tab]?.value = TabSelectionState(
                isSelectionMode = true,
                selectedMessages = messageIds.toSet(),
                selectedCount = messageIds.size
            )
        }
    }
    
    fun clearSelection(tab: MessageTab) {
        exitSelectionMode(tab)
    }
    
    fun isMessageSelected(tab: MessageTab, messageId: Long): Boolean {
        return tabStates[tab]?.value?.selectedMessages?.contains(messageId) ?: false
    }
    
    fun getSelectedMessageIds(tab: MessageTab): List<Long> {
        return tabStates[tab]?.value?.selectedMessages?.toList() ?: emptyList()
    }
    
    fun isSelectionMode(tab: MessageTab): Boolean {
        return tabStates[tab]?.value?.isSelectionMode ?: false
    }
    
    fun getSelectedCount(tab: MessageTab): Int {
        return tabStates[tab]?.value?.selectedCount ?: 0
    }
}
