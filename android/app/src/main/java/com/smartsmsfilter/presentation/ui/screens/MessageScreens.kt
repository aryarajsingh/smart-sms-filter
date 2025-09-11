package com.smartsmsfilter.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.presentation.ui.components.MessageList
import com.smartsmsfilter.presentation.ui.components.PremiumConversationItem
import com.smartsmsfilter.presentation.viewmodel.SmsViewModel
import com.smartsmsfilter.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onNavigateToThread: (String) -> Unit = {},
    viewModel: SmsViewModel = hiltViewModel()
) {
    val messages by viewModel.inboxMessages.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Premium large title header (iOS-esque)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.padding(
                    start = PremiumSpacing.Medium,
                    end = PremiumSpacing.Medium,
                    top = PremiumSpacing.Large,
                    bottom = PremiumSpacing.Small
                )
            ) {
                Text(
                    text = "Inbox",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (messages.count { !it.isRead } > 0) {
                    Text(
                        text = "${messages.count { !it.isRead }} unread",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = PremiumSpacing.XSmall)
                    )
                }
            }
        }
        
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(PremiumSpacing.Small)
                ) {
                    Text(
                        text = "All caught up! 🎉",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Important messages will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = PremiumSpacing.Small),
                verticalArrangement = Arrangement.spacedBy(PremiumSpacing.XSmall)
            ) {
                items(messages) { message ->
                    PremiumConversationItem(
                        title = if (message.sender == "You") "You" else message.sender,
                        lastMessage = message.content,
                        timestamp = formatRelativeTime(message.timestamp),
                        unreadCount = if (!message.isRead) 1 else 0,
                        onClick = {
                            if (!message.isRead) {
                                viewModel.markAsRead(message.id)
                            }
                            onNavigateToThread(message.sender)
                        },
                        modifier = Modifier.padding(horizontal = PremiumSpacing.Small)
                    )
            }
        }
    }
}

private fun formatRelativeTime(date: Date): String {
    val now = Calendar.getInstance()
    val messageDate = Calendar.getInstance().apply { time = date }
    
    val diffInMillis = now.timeInMillis - messageDate.timeInMillis
    val diffInMinutes = diffInMillis / (1000 * 60)
    val diffInHours = diffInMinutes / 60
    val diffInDays = diffInHours / 24
    
    return when {
        diffInMinutes < 1 -> "now"
        diffInMinutes < 60 -> "${diffInMinutes}m"
        diffInHours < 24 -> "${diffInHours}h"
        diffInDays == 1L -> "yesterday"
        diffInDays < 7 -> "${diffInDays}d"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
    }
}
    
    // Show snackbar messages
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            // Show snackbar here if needed
            viewModel.clearMessage()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilteredScreen(
    onNavigateToThread: (String) -> Unit = {},
    viewModel: SmsViewModel = hiltViewModel()
) {
    val messages by viewModel.filteredMessages.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Premium large title header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.padding(
                    start = PremiumSpacing.Medium,
                    end = PremiumSpacing.Medium,
                    top = PremiumSpacing.Large,
                    bottom = PremiumSpacing.Small
                )
            ) {
                Text(
                    text = "Filtered",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (messages.count { !it.isRead } > 0) {
                    Text(
                        text = "${messages.count { !it.isRead }} filtered",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = PremiumSpacing.XSmall)
                    )
                }
            }
        }
        
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(PremiumSpacing.Small)
                ) {
                    Text(
                        text = "No spam found \ud83d\udc4c",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Filtered messages will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = PremiumSpacing.Small),
                verticalArrangement = Arrangement.spacedBy(PremiumSpacing.XSmall)
            ) {
                items(messages) { message ->
                    PremiumConversationItem(
                        title = message.sender,
                        lastMessage = message.content,
                        timestamp = formatRelativeTime(message.timestamp),
                        unreadCount = if (!message.isRead) 1 else 0,
                        onClick = {
                            if (!message.isRead) {
                                viewModel.markAsRead(message.id)
                            }
                            onNavigateToThread(message.sender)
                        },
                        modifier = Modifier.padding(horizontal = PremiumSpacing.Small)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeedsReviewScreen(
    onNavigateToThread: (String) -> Unit = {},
    viewModel: SmsViewModel = hiltViewModel()
) {
    val messages by viewModel.needsReviewMessages.collectAsStateWithLifecycle()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { 
                Text("Needs Review (${messages.count { !it.isRead }})") 
            }
        )
        
        if (messages.isNotEmpty()) {
            Text(
                text = "Review these messages and move them to the correct category to help improve filtering.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        MessageList(
            messages = messages,
            onMessageClick = { message ->
                if (!message.isRead) {
                    viewModel.markAsRead(message.id)
                }
                onNavigateToThread(message.sender)
            },
            onMoveToCategory = { messageId, category ->
                viewModel.updateMessageCategory(messageId, category)
            }
        )
    }
}
