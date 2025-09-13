package com.smartsmsfilter.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.smartsmsfilter.R
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.ui.theme.IOSSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionBar(
    visible: Boolean,
    selectedCount: Int,
    onMoveToSpamClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
    onMoveToImportantClick: (() -> Unit)? = null,
    showMoveToImportant: Boolean = false
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(IOSSpacing.medium),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(IOSSpacing.medium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selection count and clear button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(IOSSpacing.small)
                ) {
                    IconButton(onClick = onClearSelection) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear selection",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "$selectedCount selected",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(IOSSpacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Move to Important button (Review screen only)
                    if (showMoveToImportant && onMoveToImportantClick != null) {
                        FilledTonalButton(
                            onClick = onMoveToImportantClick,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Important", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    
                    // Move to Spam button
                    FilledTonalButton(
                        onClick = onMoveToSpamClick,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Spam", style = MaterialTheme.typography.labelMedium)
                    }
                    
                    // Delete button
                    FilledTonalButton(
                        onClick = onDeleteClick,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
