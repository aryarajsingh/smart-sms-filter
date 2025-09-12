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
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMarkImportantClick: () -> Unit,
    onMoveCategoryClick: (MessageCategory) -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier
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
                    // Archive button
                    IconButton(onClick = onArchiveClick) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Archive",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Important button
                    IconButton(onClick = onMarkImportantClick) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Mark Important",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Category dropdown menu
                    var showCategoryMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showCategoryMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = "Change Category",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Move to Inbox") },
                                onClick = {
                                    onMoveCategoryClick(MessageCategory.INBOX)
                                    showCategoryMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Inbox, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Move to Spam") },
                                onClick = {
                                    onMoveCategoryClick(MessageCategory.SPAM)
                                    showCategoryMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Block, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Needs Review") },
                                onClick = {
                                    onMoveCategoryClick(MessageCategory.NEEDS_REVIEW)
                                    showCategoryMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.QuestionMark, contentDescription = null)
                                }
                            )
                        }
                    }
                    
                    // Delete button
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
