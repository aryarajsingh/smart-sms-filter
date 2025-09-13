package com.smartsmsfilter.presentation.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.presentation.viewmodel.ThreadViewModel
import com.smartsmsfilter.ui.components.PremiumMessageBubble
import com.smartsmsfilter.ui.components.PremiumComposerBar
import com.smartsmsfilter.ui.components.MessageActionBottomSheet
import kotlinx.coroutines.launch
import com.smartsmsfilter.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadScreen(
    address: String,
    onNavigateBack: () -> Unit,
    viewModel: ThreadViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val contactName by viewModel.contactName.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    
    // State for message actions bottom sheet
    var selectedMessage by remember { mutableStateOf<SmsMessage?>(null) }
    var showMessageActions by remember { mutableStateOf(false) }
    var starredMessageIds by remember { mutableStateOf(setOf<Long>()) }
    
    LaunchedEffect(address) {
        viewModel.loadThread(address)
    }
    
    // Load starred status for messages
    LaunchedEffect(messages) {
        scope.launch {
            val starredIds = messages.mapNotNull { message ->
                if (viewModel.isMessageStarred(message.id)) message.id else null
            }.toSet()
            starredMessageIds = starredIds
        }
    }
    
    // Clear message status after sending
    LaunchedEffect(uiState.message) {
        if (uiState.message == "Message sent successfully!") {
            viewModel.clearMessage()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { 
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = contactName ?: address,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        
        // Messages list - show oldest at top, newest at bottom
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = false
        ) {
            items(
                items = messages,
                key = { it.id },
                contentType = { "message" }
            ) { message ->
                PremiumMessageBubble(
                    message = message,
                    isStarred = starredMessageIds.contains(message.id),
                    onLongPress = { selectedMsg ->
                        selectedMessage = selectedMsg
                        showMessageActions = true
                    },
                    modifier = Modifier.padding(horizontal = PremiumSpacing.Small)
                )
            }
        }
        
        PremiumComposerBar(
            text = uiState.messageText,
            onTextChange = { viewModel.updateMessageText(it) },
            onSend = { if (uiState.canSendMessage) viewModel.sendMessage() },
            canSend = uiState.canSendMessage,
            isSending = uiState.isSending,
            placeholder = "Type a message"
        )
    }
    
    // Message action bottom sheet
    selectedMessage?.let { message ->
        if (showMessageActions) {
            MessageActionBottomSheet(
                message = message,
                isStarred = starredMessageIds.contains(message.id),
                onStarToggle = {
                    viewModel.toggleMessageStar(message)
                    // Update starred status immediately
                    starredMessageIds = if (starredMessageIds.contains(message.id)) {
                        starredMessageIds - message.id
                    } else {
                        starredMessageIds + message.id
                    }
                },
                onDelete = {
                    viewModel.deleteMessage(message.id)
                    // Remove from starred if it was starred
                    starredMessageIds = starredMessageIds - message.id
                },
                onDismiss = {
                    showMessageActions = false
                    selectedMessage = null
                },
                onCopy = { text ->
                    clipboardManager.setText(AnnotatedString(text))
                }
            )
        }
    }
    
    // Show snackbar for messages
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            // Auto-clear message after showing
            kotlinx.coroutines.delay(2000)
            viewModel.clearMessage()
        }
    }
}

