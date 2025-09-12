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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Unified message screen component that provides consistent UI, selection, and functionality
 * across all message categories (Inbox, Spam, Review).
 * 
 * This eliminates code duplication and ensures consistent behavior across tabs.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun UnifiedMessageScreen(
    messagesFlow: StateFlow<List<SmsMessage>>,
    viewModel: SmsViewModel,
    tab: MessageTab, // Add tab parameter to identify which tab this is
    onNavigateToThread: (String) -> Unit,
    screenTitle: String,
    emptyStateTitle: String,
    emptyStateMessage: String,
    showCategoryInCards: Boolean = false,
    enableCategoryChange: Boolean = false,
    modifier: Modifier = Modifier,
    onSettingsClick: (() -> Unit)? = null
) {
    val messages by messagesFlow.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showWhyDialog by remember { mutableStateOf(false) }
    var whyReasons by remember { mutableStateOf<List<String>>(emptyList()) }
    var whyCategory by remember { mutableStateOf<MessageCategory?>(null) }
    var whyMessageId by remember { mutableStateOf<Long?>(null) }
    var selectedReasons by remember { mutableStateOf(setOf<String>()) }
    
    // Selection state - now tab-specific
    val tabSelectionState by viewModel.selectionState.getTabState(tab).collectAsStateWithLifecycle()
    val isSelectionMode = tabSelectionState.isSelectionMode
    val selectedCount = tabSelectionState.selectedCount
    val selectedMessages = tabSelectionState.selectedMessages
    
    // Dialog states
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var targetCategory by remember { mutableStateOf<MessageCategory?>(null) }
    
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(modifier = modifier.fillMaxSize().padding(innerPadding)) {
        // Unified Action Bar - appears in selection mode for all screens
        MessageActionBar(
            visible = isSelectionMode,
            selectedCount = selectedCount,
            onArchiveClick = { showArchiveDialog = true },
            onDeleteClick = { showDeleteDialog = true },
            onMarkImportantClick = { viewModel.markSelectedAsImportant(tab) },
            onMoveCategoryClick = if (enableCategoryChange) { category ->
                android.util.Log.d("UnifiedMessageScreen", "Category change clicked: $category")
                targetCategory = category
                showCategoryDialog = true
            } else { { _ -> } },
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
                
                // Unread count with appropriate color per screen
                if (messages.count { !it.isRead } > 0) {
                    Text(
                        text = "${messages.count { !it.isRead }} ${if (screenTitle == "Spam") "spam messages" else "unread"}",
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
                        text = "Long-press to select messages, then use the action bar to categorize them as Important or Spam.",
                        style = IOSTypography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = IOSSpacing.small)
                    )
                }
            }
        }
        
        // Content area - unified empty state and message list
        if (uiState.isLoading) {
            // Lightweight loading skeletons for premium perceived performance
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = IOSSpacing.medium,
                    vertical = IOSSpacing.small
                ),
                verticalArrangement = Arrangement.spacedBy(IOSSpacing.small)
            ) {
                items(6) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 1.dp
                    ) {}
                }
            }
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = IOSSpacing.medium,
                    vertical = IOSSpacing.small
                ),
                verticalArrangement = Arrangement.spacedBy(IOSSpacing.small)
            ) {
                items(messages, key = { it.id }) { message ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(120)) + scaleIn(initialScale = 0.98f, animationSpec = tween(120)),
                        exit = fadeOut(animationSpec = tween(100))
                    ) {
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
            }
        }
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
    
    // Unified confirmation dialogs - consistent across all screens
    DeleteConfirmationDialog(
        showDialog = showDeleteDialog,
        messageCount = selectedCount,
        onConfirm = { viewModel.deleteSelectedMessages(tab) },
        onDismiss = { showDeleteDialog = false }
    )
    
    ArchiveConfirmationDialog(
        showDialog = showArchiveDialog,
        messageCount = selectedCount,
        onConfirm = { viewModel.archiveSelectedMessages(tab) },
        onDismiss = { showArchiveDialog = false }
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

    // Category change dialog - only for Review screen
    if (enableCategoryChange) {
        targetCategory?.let { category ->
            CategoryChangeConfirmationDialog(
                showDialog = showCategoryDialog,
                messageCount = selectedCount,
                targetCategory = category.displayName(),
                onConfirm = { viewModel.moveSelectedToCategory(tab, category) },
                onDismiss = {
                    showCategoryDialog = false
                    targetCategory = null
                }
            )
        }
    }
}
