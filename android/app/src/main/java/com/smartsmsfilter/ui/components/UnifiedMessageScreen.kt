package com.smartsmsfilter.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.domain.model.displayName
import com.smartsmsfilter.presentation.viewmodel.SmsViewModel
import com.smartsmsfilter.ui.theme.IOSSpacing
import com.smartsmsfilter.ui.theme.IOSTypography
import com.smartsmsfilter.ui.state.MessageTab
import com.smartsmsfilter.ui.state.isLoading
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
// Pull-to-refresh removed - auto-sync is always active

/**
 * Unified message screen component that provides consistent UI, selection, and functionality
 * across all message categories (Inbox, Spam, Review).
 * 
 * This eliminates code duplication and ensures consistent behavior across tabs.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun UnifiedMessageScreen(
    messages: List<SmsMessage>,
    viewModel: SmsViewModel,
    tab: MessageTab,
    onNavigateToThread: (String) -> Unit,
    onNavigateToStarred: () -> Unit = {},
    screenTitle: String,
    emptyStateTitle: String,
    emptyStateMessage: String,
    showCategoryInCards: Boolean = false,
    enableCategoryChange: Boolean = false,
    modifier: Modifier = Modifier,
    onSettingsClick: (() -> Unit)? = null,
    groupBySender: Boolean = false, // Kept for flexibility, though logic moved
    unreadCount: Int? = null,
    totalCount: Int? = null,
    showStarredAccess: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val mainLoadingState by viewModel.mainLoadingState.collectAsStateWithLifecycle()
    val isAnyOperationLoading by viewModel.isAnyOperationLoading.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showWhyDialog by remember { mutableStateOf(false) }
    var whyReasons by remember { mutableStateOf<List<String>>(emptyList()) }
    var whyCategory by remember { mutableStateOf<MessageCategory?>(null) }
    var whyMessageId by remember { mutableStateOf<Long?>(null) }
    var selectedReasons by remember { mutableStateOf(setOf<String>()) }
    
    // Optimize expensive calculations
    val countText = remember(totalCount, unreadCount) {
        when {
            totalCount != null -> if (totalCount > 0) "$totalCount spam message${if (totalCount != 1) "s" else ""}" else null
            unreadCount != null -> if (unreadCount > 0) "$unreadCount unread" else null
            else -> null
        }
    }
    
    // Selection state - now tab-specific
    val tabSelectionState by viewModel.selectionState.getTabState(tab).collectAsStateWithLifecycle()
    val isSelectionMode = tabSelectionState.isSelectionMode
    val selectedCount = tabSelectionState.selectedCount
    val selectedMessages = tabSelectionState.selectedMessages
    
    // Dialog states
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(modifier = modifier.fillMaxSize().padding(innerPadding)) {
        // Unified Action Bar - appears in selection mode for all screens
        MessageActionBar(
            visible = isSelectionMode,
            selectedCount = selectedCount,
            currentTab = tab,
            onMoveToInboxClick = { 
                viewModel.moveSelectedToCategory(tab, MessageCategory.INBOX)
                // Trigger learning when moving to inbox
                if (tab == MessageTab.SPAM || tab == MessageTab.REVIEW) {
                    selectedMessages.forEach { messageId ->
                        viewModel.learnFromUserAction(messageId, MessageCategory.INBOX)
                    }
                }
            },
            onMoveToSpamClick = { 
                viewModel.moveSelectedToCategory(tab, MessageCategory.SPAM)
                // Trigger learning when marking as spam
                if (tab == MessageTab.REVIEW) {
                    selectedMessages.forEach { messageId ->
                        viewModel.learnFromUserAction(messageId, MessageCategory.SPAM)
                    }
                }
            },
            onDeleteClick = { showDeleteDialog = true },
            onClearSelection = { viewModel.clearSelection(tab) }
        )
        
        // Premium iOS-style header - consistent across all screens
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.padding(
                    start = IOSSpacing.medium,
                    end = IOSSpacing.medium,
                    top = IOSSpacing.medium,
                    bottom = IOSSpacing.small
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = screenTitle,
                        style = IOSTypography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    if (showStarredAccess && !isSelectionMode) {
                        IconButton(onClick = onNavigateToStarred) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.Star,
                                contentDescription = "Starred Messages",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (onSettingsClick != null) {
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
                
                // Message count with appropriate color per screen
                if (countText != null) {
                    Text(
                        text = countText,
                        style = IOSTypography.titleMedium,
                        color = when (screenTitle) {
                            "Spam" -> MaterialTheme.colorScheme.error
                            "Review" -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.padding(top = IOSSpacing.small)
                    )
                }
                
                // Special instruction for Review screen
                if (screenTitle == "Review" && messages.isNotEmpty() && enableCategoryChange) {
                    Text(
                        text = "Sort messages: Long-press → Select → Tap Important or Spam",
                        style = IOSTypography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = IOSSpacing.small)
                    )
                }
            }
        }
        
        // Content area - unified empty state and message list with standardized loading
        if (mainLoadingState.isLoading) {
            // Use standardized skeleton loader for consistent loading experience
            StandardizedSkeletonLoader(
                count = 6,
                modifier = Modifier.fillMaxSize()
            )
        } else if (messages.isEmpty()) {
            // Consistent empty state design
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(IOSSpacing.medium)
                ) {
                    Text(
                        text = emptyStateTitle,
                        style = IOSTypography.headlineSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = emptyStateMessage,
                        style = IOSTypography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Unified message list with consistent behavior
            val listToShow = messages // Data is now pre-processed
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = IOSSpacing.medium,
                    vertical = IOSSpacing.small
                ),
                verticalArrangement = Arrangement.spacedBy(IOSSpacing.small)
            ) {
                items(
                    items = listToShow, 
                    key = { it.id },
                    contentType = { "message" } // Help compose optimize
                ) { message ->
                    SwipeableMessageCard(
                            message = message,
                            modifier = Modifier,
                        onClick = {
                            if (isSelectionMode) {
                                viewModel.onMessageSelectionToggle(tab, message.id)
                            } else {
                                if (!message.isRead) viewModel.markAsRead(message.id)
                                onNavigateToThread(message.sender)
                            }
                        },
                        onArchive = { viewModel.archiveMessage(message.id) },
                        onDelete = { viewModel.deleteMessage(message.id) },
                        isSelectionMode = isSelectionMode,
                        isSelected = selectedMessages.contains(message.id),
                        onLongPress = { 
                            viewModel.onMessageLongPress(tab, message.id) 
                        },
                        onSelectionToggle = { viewModel.onMessageSelectionToggle(tab, message.id) },
                        showCategory = showCategoryInCards,
                        onWhyRequested = {
                            val id = message.id
                            scope.launch {
                                val reasons = viewModel.getWhyReasons(id)
                                whyReasons = reasons
                                whyCategory = message.category
                                whyMessageId = id
                                selectedReasons = emptySet()
                                showWhyDialog = true
                            }
                        }
                        )
                }
            }
                // Pull-to-refresh removed - messages auto-sync in real-time
            }
        }
    }
    }
    
    // Show error display if there's an error
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            val result = snackbarHostState.showSnackbar(
                message = error.message,
                actionLabel = error.actionLabel,
                duration = if (error.canRetry) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed && error.canRetry) {
                error.onAction?.invoke()
            }
            viewModel.clearError()
        }
    }
    
    // Show snackbar messages with Undo capability
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Undo"
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoLastOperation()
            }
            viewModel.clearMessage()
        }
    }
    
    // Delete confirmation dialog
    DeleteConfirmationDialog(
        showDialog = showDeleteDialog,
        messageCount = selectedCount,
        onConfirm = { viewModel.deleteSelectedMessages(tab) },
        onDismiss = { showDeleteDialog = false }
    )
    
    // Compact "Why?" dialog
    if (showWhyDialog) {
AlertDialog(
            onDismissRequest = { showWhyDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val id = whyMessageId
                    if (id != null) {
                        viewModel.correctClassification(id, MessageCategory.INBOX, selectedReasons.toList())
                    }
                    showWhyDialog = false
                }) { Text("Move to Inbox") }
            },
            dismissButton = {
                TextButton(onClick = {
                    val id = whyMessageId
                    if (id != null) {
                        viewModel.correctClassification(id, MessageCategory.SPAM, selectedReasons.toList())
                    }
                    showWhyDialog = false
                }) { Text("Mark Spam") }
            },
            title = {
                val cat = whyCategory?.displayName() ?: "Classification"
                Text("Why is this $cat?")
            },
            text = {
                AnimatedVisibility(
                    visible = showWhyDialog,
                    enter = fadeIn(animationSpec = tween(120)) + scaleIn(initialScale = 0.98f, animationSpec = tween(120)),
                    exit = fadeOut(animationSpec = tween(100))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (whyReasons.isEmpty()) {
                        Text("No explanation recorded.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            whyReasons.forEach { r -> Text("• $r") }
                        }
                    }
                    // Feedback chips (compact)
                    val feedbackOptions = listOf("Not spam", "Important", "OTP", "Banking", "Promo")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        feedbackOptions.forEach { label ->
                            val selected = selectedReasons.contains(label)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    selectedReasons = if (selected) selectedReasons - label else selectedReasons + label
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    }
                }
            }
        )
    }
    
    // Loading overlay for bulk operations
    Box(modifier = Modifier.fillMaxSize()) {
        if (isAnyOperationLoading) {
            StandardizedLoadingOverlay(
                loadingState = mainLoadingState,
                onCancel = null,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

}
