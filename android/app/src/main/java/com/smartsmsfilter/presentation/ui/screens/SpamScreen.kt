package com.smartsmsfilter.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    UnifiedMessageScreen(
        messages = uiState.spamMessages,
        viewModel = viewModel,
        tab = MessageTab.SPAM,
        onNavigateToThread = onNavigateToThread,
        screenTitle = "Spam",
        emptyStateTitle = "Great work! 🛡️",
        emptyStateMessage = "We're automatically blocking promotional texts and spam. Filtered messages appear here.",
        showCategoryInCards = false,
        enableCategoryChange = true,
        modifier = modifier,
        onSettingsClick = onSettingsClick,
        totalCount = uiState.spamTotalCount
    )
}
