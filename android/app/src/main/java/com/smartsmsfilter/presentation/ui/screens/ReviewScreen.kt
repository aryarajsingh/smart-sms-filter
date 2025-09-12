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
fun ReviewScreen(
    onNavigateToThread: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SmsViewModel = hiltViewModel(),
    onSettingsClick: (() -> Unit)? = null
) {
    UnifiedMessageScreen(
        messagesFlow = viewModel.reviewMessages,
        viewModel = viewModel,
        tab = MessageTab.REVIEW,
        onNavigateToThread = onNavigateToThread,
        screenTitle = "Review",
        emptyStateTitle = "Nothing to review",
        emptyStateMessage = "Messages requiring attention will appear here.",
        showCategoryInCards = true,
        enableCategoryChange = true,
        modifier = modifier,
        onSettingsClick = onSettingsClick
    )
}
