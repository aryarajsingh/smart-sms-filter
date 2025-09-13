package com.smartsmsfilter.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.ui.theme.IOSSpacing

/**
 * Bottom sheet for message actions in chat context - only shows Star/Unstar and Delete
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionBottomSheet(
    message: SmsMessage,
    isStarred: Boolean,
    onStarToggle: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onCopy: ((String) -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(IOSSpacing.medium)
        ) {
            // Header with message preview
            Text(
                text = "Message Actions",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = IOSSpacing.small)
            )
            
            Text(
                text = if (message.content.length > 120) {
                    // Enhanced preview: show beginning and end for better context
                    "${message.content.take(80)}...${message.content.takeLast(20)}"
                } else message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = IOSSpacing.large)
            )
            
            // Action buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(IOSSpacing.small)
            ) {
                // Star/Unstar action
                Card(
                    onClick = {
                        onStarToggle()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(IOSSpacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(IOSSpacing.medium)
                    ) {
                        Icon(
                            imageVector = if (isStarred) Icons.Default.StarBorder else Icons.Default.Star,
                            contentDescription = if (isStarred) "Remove star" else "Add star",
                            tint = androidx.compose.ui.graphics.Color(0xFFFFD700), // Consistent gold color
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (isStarred) "Remove Star" else "Add Star",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
                
                // Copy action
                if (onCopy != null) {
                    Card(
                        onClick = {
                            onCopy(message.content)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(IOSSpacing.medium),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(IOSSpacing.medium)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy message",
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Copy Message",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
                
                // Delete action
                Card(
                    onClick = {
                        onDelete()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(IOSSpacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(IOSSpacing.medium)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Delete Message",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
            
            // Bottom padding
            Spacer(modifier = Modifier.height(IOSSpacing.large))
        }
    }
}