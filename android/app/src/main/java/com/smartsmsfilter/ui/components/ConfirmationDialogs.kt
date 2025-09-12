package com.smartsmsfilter.ui.components

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartsmsfilter.ui.theme.IOSSpacing
import com.smartsmsfilter.ui.theme.IOSTypography

/**
 * Enhanced confirmation dialog for deleting messages with better Material Design
 */
@Composable
fun DeleteConfirmationDialog(
    showDialog: Boolean,
    messageCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        EnhancedConfirmationDialog(
            icon = Icons.Outlined.Delete,
            title = "Delete ${if (messageCount == 1) "Message" else "$messageCount Messages"}?",
            message = if (messageCount == 1) {
                "This message will be permanently deleted and cannot be recovered."
            } else {
                "These $messageCount messages will be permanently deleted and cannot be recovered."
            },
            confirmText = "Delete",
            dismissText = "Cancel",
            confirmButtonColors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ),
            onConfirm = {
                onConfirm()
                onDismiss()
            },
            onDismiss = onDismiss
        )
    }
}

/**
 * Enhanced confirmation dialog for archiving messages
 */
@Composable
fun ArchiveConfirmationDialog(
    showDialog: Boolean,
    messageCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        EnhancedConfirmationDialog(
            icon = Icons.Outlined.Archive,
            title = "Archive ${if (messageCount == 1) "Message" else "$messageCount Messages"}?",
            message = if (messageCount == 1) {
                "This message will be moved to archived messages and won't appear in your main inbox."
            } else {
                "These $messageCount messages will be moved to archived messages and won't appear in your main inbox."
            },
            confirmText = "Archive",
            dismissText = "Cancel",
            confirmButtonColors = ButtonDefaults.filledTonalButtonColors(),
            onConfirm = {
                onConfirm()
                onDismiss()
            },
            onDismiss = onDismiss
        )
    }
}

/**
 * Enhanced confirmation dialog for changing message category
 */
@Composable
fun CategoryChangeConfirmationDialog(
    showDialog: Boolean,
    messageCount: Int,
    targetCategory: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        EnhancedConfirmationDialog(
icon = Icons.AutoMirrored.Outlined.DriveFileMove,
            title = "Move to $targetCategory?",
            message = if (messageCount == 1) {
                "This message will be moved to $targetCategory."
            } else {
                "These $messageCount messages will be moved to $targetCategory."
            },
            confirmText = "Move",
            dismissText = "Cancel",
            confirmButtonColors = ButtonDefaults.filledTonalButtonColors(),
            onConfirm = {
                onConfirm()
                onDismiss()
            },
            onDismiss = onDismiss
        )
    }
}

/**
 * Enhanced confirmation dialog with Material Design 3 styling and animations
 */
@Composable
private fun EnhancedConfirmationDialog(
    icon: ImageVector,
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    confirmButtonColors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    val iconScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = 0.8f,
            stiffness = 300f
        )
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.scale(iconScale),
                tint = MaterialTheme.colorScheme.onSurface
            )
        },
        title = {
            Text(
                text = title,
                style = IOSTypography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = message,
                style = IOSTypography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            FilledTonalButton(
                onClick = onConfirm,
                colors = confirmButtonColors,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = confirmText,
                    style = IOSTypography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = dismissText,
                    style = IOSTypography.titleMedium.copy(
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}
