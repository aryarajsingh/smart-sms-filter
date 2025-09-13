package com.smartsmsfilter.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartsmsfilter.domain.common.UiError
import com.smartsmsfilter.ui.theme.IOSSpacing
import com.smartsmsfilter.ui.theme.IOSTypography

/**
 * Enhanced error state display with actionable retry options
 */
@Composable
fun ErrorStateDisplay(
    error: UiError,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    illustration: ImageVector = Icons.Default.ErrorOutline,
    showIllustration: Boolean = true
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut()
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(IOSSpacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (showIllustration) {
                Icon(
                    imageVector = illustration,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                
                Spacer(Modifier.height(IOSSpacing.medium))
            }
            
            Text(
                text = if (error.isRecoverable) "Oops! Something went wrong" else "Critical Error",
                style = IOSTypography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (error.isRecoverable) 
                    MaterialTheme.colorScheme.onSurface 
                else 
                    MaterialTheme.colorScheme.error
            )
            
            Spacer(Modifier.height(IOSSpacing.small))
            
            Text(
                text = error.message,
                style = IOSTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = IOSSpacing.medium)
            )
            
            if (error.canRetry && (onRetry != null || error.onAction != null)) {
                Spacer(Modifier.height(IOSSpacing.large))
                
                val actionHandler = error.onAction ?: onRetry
                actionHandler?.let { handler ->
                    Button(
                        onClick = handler,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (error.isRecoverable) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(IOSSpacing.small))
                        Text(error.actionLabel ?: "Try Again")
                    }
                }
            }
            
            if (!error.isRecoverable) {
                Spacer(Modifier.height(IOSSpacing.medium))
                
                Text(
                    text = "Please restart the app or contact support if the problem persists.",
                    style = IOSTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Inline error display for smaller contexts (like form fields)
 */
@Composable
fun InlineErrorDisplay(
    error: UiError,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(IOSSpacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(Modifier.width(IOSSpacing.small))
            
            Text(
                text = error.message,
                style = IOSTypography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            
            if (error.canRetry && (onRetry != null || error.onAction != null)) {
                val actionHandler = error.onAction ?: onRetry
                actionHandler?.let { handler ->
                    TextButton(
                        onClick = handler,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = error.actionLabel ?: "Retry",
                            style = IOSTypography.labelMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Loading overlay for operations with cancellation support
 */
@Composable
fun LoadingOverlay(
    isVisible: Boolean,
    message: String = "Loading...",
    canCancel: Boolean = false,
    onCancel: (() -> Unit)? = null,
    progress: Float? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(200)),
        exit = fadeOut(animationSpec = tween(200))
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(IOSSpacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(IOSSpacing.medium)
                ) {
                    if (progress != null) {
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth(),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = IOSTypography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp
                        )
                    }
                    
                    Text(
                        text = message,
                        style = IOSTypography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (canCancel && onCancel != null) {
                        TextButton(onClick = onCancel) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Network status indicator for offline states
 */
@Composable
fun NetworkStatusIndicator(
    isOffline: Boolean,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOffline,
        enter = androidx.compose.animation.slideInVertically { -it },
        exit = androidx.compose.animation.slideOutVertically { -it }
    ) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.errorContainer,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(IOSSpacing.medium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                
                Spacer(Modifier.width(IOSSpacing.small))
                
                Text(
                    text = "You're offline. Showing cached data.",
                    style = IOSTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
                
                TextButton(onClick = onRetryClick) {
                    Text(
                        text = "Retry",
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}