package com.smartsmsfilter.presentation.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsmsfilter.domain.model.StarredSenderGroup
import com.smartsmsfilter.presentation.viewmodel.StarredMessagesViewModel
import com.smartsmsfilter.ui.theme.IOSSpacing
import com.smartsmsfilter.ui.theme.IOSTypography
import com.smartsmsfilter.ui.utils.formatRelativeTime
import com.smartsmsfilter.ui.utils.resolveDisplayName
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StarredMessagesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSender: (String) -> Unit,
    viewModel: StarredMessagesViewModel = hiltViewModel()
) {
    val starredSenders by viewModel.starredSenderGroups.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        viewModel.loadStarredMessages()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Starred Messages",
                        style = IOSTypography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (starredSenders.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(IOSSpacing.medium)
                    ) {
                        Icon(
                            imageVector = Icons.Default.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "No Starred Messages",
                            style = IOSTypography.headlineSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Long press messages in conversations to star them",
                            style = IOSTypography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = IOSSpacing.large)
                        )
                    }
                }
            } else {
                // Starred messages list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(IOSSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(IOSSpacing.small)
                ) {
                    items(
                        items = starredSenders,
                        key = { it.sender },
                        contentType = { "sender_group" }
                    ) { senderGroup ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(200)) + 
                                   slideInVertically(animationSpec = tween(200)) { it / 4 },
                            exit = fadeOut(animationSpec = tween(150)) + 
                                  slideOutVertically(animationSpec = tween(150)) { -it / 4 }
                        ) {
                            StarredSenderCard(
                                senderGroup = senderGroup,
                                context = context,
                                onClick = { onNavigateToSender(senderGroup.sender) },
                                onUnstarAll = { viewModel.unstarAllFromSender(senderGroup.sender) }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Show error messages
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }
    
    // Show success messages
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessage()
        }
    }
}

@Composable
private fun StarredSenderCard(
    senderGroup: StarredSenderGroup,
    context: android.content.Context,
    onClick: () -> Unit,
    onUnstarAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = resolveDisplayName(context, senderGroup.sender)
    
    var showUnstarDialog by remember { mutableStateOf(false) }
    
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(IOSSpacing.medium)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Contact Avatar
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName.take(1).uppercase(),
                        style = IOSTypography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.width(IOSSpacing.medium))
                
                // Sender info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = displayName,
                        style = IOSTypography.bodyLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${senderGroup.messageCount} starred message${if (senderGroup.messageCount != 1) "s" else ""}",
                        style = IOSTypography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Actions
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(IOSSpacing.small)
                ) {
                    // Star count badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = senderGroup.messageCount.toString(),
                                style = IOSTypography.labelMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    
                    // Unstar all button
                    IconButton(
                        onClick = { showUnstarDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.StarBorder,
                            contentDescription = "Remove all stars",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            // Latest message preview
            val latestMessage = senderGroup.starredMessages.maxByOrNull { it.starredAt }
            latestMessage?.let { message ->
                if (message.messagePreview.isNotBlank()) {
                    Spacer(modifier = Modifier.height(IOSSpacing.small))
                    Text(
                        text = message.messagePreview,
                        style = IOSTypography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 60.dp) // Align with sender name
                    )
                }
            }
            
            // Timestamp
            Spacer(modifier = Modifier.height(IOSSpacing.small))
            Text(
                text = "Last starred: ${formatRelativeTime(senderGroup.lastStarredAt.time)}",
                style = IOSTypography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 60.dp)
            )
        }
    }
    
    // Unstar all confirmation dialog
    if (showUnstarDialog) {
        AlertDialog(
            onDismissRequest = { showUnstarDialog = false },
            title = { Text("Remove All Stars?") },
            text = { 
                Text("This will remove stars from all ${senderGroup.messageCount} messages from $displayName.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUnstarAll()
                        showUnstarDialog = false
                    }
                ) {
                    Text("Remove All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnstarDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}