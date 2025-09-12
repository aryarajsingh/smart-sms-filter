package com.smartsmsfilter.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartsmsfilter.data.contacts.Contact
import com.smartsmsfilter.presentation.viewmodel.ComposeMessageViewModel
import com.smartsmsfilter.ui.components.PremiumComposerBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeMessageScreen(
    onNavigateBack: () -> Unit,
    selectedContact: Contact? = null,
    viewModel: ComposeMessageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contacts by viewModel.filteredContacts.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current

    // Set initial recipient if provided
    LaunchedEffect(selectedContact) {
        selectedContact?.let { contact ->
            viewModel.setRecipient(contact)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New message") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
        // Recipient Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "To:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = uiState.recipientQuery,
                    onValueChange = { viewModel.updateRecipientQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter phone number or contact name") },
                    leadingIcon = {
Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true
                )
                
                // Selected recipient display
                uiState.selectedRecipient?.let { recipient ->
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = recipient.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${recipient.phoneNumber} • ${recipient.phoneType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            TextButton(
                                onClick = { viewModel.clearRecipient() }
                            ) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
        }
        
        // Contact suggestions (shown when typing and no recipient selected)
        if (uiState.selectedRecipient == null && uiState.recipientQuery.isNotBlank() && contacts.isNotEmpty()) {
            Text(
                text = "Suggestions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(contacts) { contact ->
                    ContactSuggestionItem(
                        contact = contact,
                        onContactSelected = { viewModel.setRecipient(it) }
                    )
                }
            }
        } else {
            // Message composition area scrollable context (templates or hints)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                if (uiState.messageText.isBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val quicks = listOf("On my way", "Call you later", "Thank you!")
                        quicks.forEach { q ->
                            OutlinedButton(onClick = { viewModel.updateMessageText(q) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(q)
                            }
                        }
                    }
                }
            }
        }

        // Bottom composer (iOS-style)
        PremiumComposerBar(
            text = uiState.messageText,
            onTextChange = { viewModel.updateMessageText(it) },
            onSend = {
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                viewModel.sendMessage()
            },
            canSend = uiState.canSendMessage,
            isSending = uiState.isSending,
            placeholder = if (uiState.selectedRecipient == null) "Select a recipient to start" else "Type a message"
        )
        }
        // Close Scaffold content lambda
    }

    // Handle success/error messages
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            // Show success message and optionally navigate back
            if (message.contains("sent", ignoreCase = true)) {
                snackbarHostState.showSnackbar(message)
                onNavigateBack()
            } else {
                snackbarHostState.showSnackbar(message)
            }
            viewModel.clearMessage()
        }
    }


    uiState.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactSuggestionItem(
    contact: Contact,
    onContactSelected: (Contact) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = { onContactSelected(contact) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contact avatar placeholder
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.firstOrNull()?.toString()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${contact.phoneNumber} • ${contact.phoneType}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
