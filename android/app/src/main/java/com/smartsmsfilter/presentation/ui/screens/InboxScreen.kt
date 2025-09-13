package com.smartsmsfilter.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsmsfilter.presentation.viewmodel.SmsViewModel
import com.smartsmsfilter.ui.components.UnifiedMessageScreen
import com.smartsmsfilter.ui.state.MessageTab
import com.smartsmsfilter.utils.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onNavigateToThread: (String) -> Unit,
    onNavigateToStarred: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SmsViewModel = hiltViewModel(),
    onSettingsClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    // Messages are automatically loaded in ViewModel init, no need to reload here
    
    UnifiedMessageScreen(
        messages = uiState.inboxMessages,
        viewModel = viewModel,
        tab = MessageTab.INBOX,
        onNavigateToThread = onNavigateToThread,
        onNavigateToStarred = onNavigateToStarred,
        screenTitle = "Inbox",
        emptyStateTitle = "All clear! 📬",
        emptyStateMessage = "Important messages appear here: OTPs, banking alerts, delivery updates, and messages from your contacts.",
        showCategoryInCards = false,
        enableCategoryChange = true,
        modifier = modifier,
        onSettingsClick = onSettingsClick,
        groupBySender = false, // Grouping is now done in ViewModel
        unreadCount = uiState.inboxUnreadCount,
        showStarredAccess = true // Enable starred messages access for Inbox
    )
}
