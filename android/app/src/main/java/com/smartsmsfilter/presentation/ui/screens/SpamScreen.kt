package com.smartsmsfilter.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsmsfilter.presentation.viewmodel.SmsViewModel
import com.smartsmsfilter.ui.components.UnifiedMessageScreen
import com.smartsmsfilter.ui.state.MessageTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpamScreen(
    onNavigateToThread: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SmsViewModel = hiltViewModel(),
    onSettingsClick: (() -> Unit)? = null
) {
    UnifiedMessageScreen(
        messagesFlow = viewModel.spamMessages,
        viewModel = viewModel,
        tab = MessageTab.SPAM,
        onNavigateToThread = onNavigateToThread,
        screenTitle = "Spam",
        emptyStateTitle = "No spam right now",
        emptyStateMessage = "Nice. Filtered messages will show up here.",
        showCategoryInCards = false,
        enableCategoryChange = true,
        modifier = modifier,
        onSettingsClick = onSettingsClick
    )
}
