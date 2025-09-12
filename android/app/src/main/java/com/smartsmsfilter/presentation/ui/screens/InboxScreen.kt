package com.smartsmsfilter.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsmsfilter.presentation.viewmodel.SmsViewModel
import com.smartsmsfilter.ui.components.UnifiedMessageScreen
import com.smartsmsfilter.ui.state.MessageTab
import com.smartsmsfilter.utils.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onNavigateToThread: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SmsViewModel = hiltViewModel(),
    onSettingsClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    
    // Trigger message reload when screen appears for the first time or when permissions change
    LaunchedEffect(Unit) {
        // Check if we have SMS permissions and reload messages if we do
        if (PermissionManager.hasAllSmsPermissions(context)) {
            viewModel.reloadSmsMessages()
        }
    }
    
    UnifiedMessageScreen(
        messagesFlow = viewModel.inboxMessages,
        viewModel = viewModel,
        tab = MessageTab.INBOX,
        onNavigateToThread = onNavigateToThread,
        screenTitle = "Inbox",
        emptyStateTitle = "All clear",
        emptyStateMessage = "Your inbox is quiet. New messages will appear here.",
        showCategoryInCards = false,
        enableCategoryChange = true,
        modifier = modifier,
        onSettingsClick = onSettingsClick
    )
}
