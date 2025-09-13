package com.smartsmsfilter.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartsmsfilter.ui.state.LoadingState
import com.smartsmsfilter.ui.state.LoadingOperation
import com.smartsmsfilter.ui.state.isLoading
import com.smartsmsfilter.ui.state.progress
import com.smartsmsfilter.ui.state.message
import com.smartsmsfilter.ui.state.canCancel
import com.smartsmsfilter.ui.theme.IOSSpacing
import com.smartsmsfilter.ui.theme.IOSTypography

/**
 * Standardized loading overlay that provides consistent loading experience
 * across all operations in the app
 */
@Composable
fun StandardizedLoadingOverlay(
    loadingState: LoadingState,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = loadingState.isLoading,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(200)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                LoadingContent(
                    loadingState = loadingState,
                    onCancel = onCancel,
                    modifier = Modifier.padding(IOSSpacing.large)
                )
            }
        }
    }
}

/**
 * Inline loading indicator for smaller contexts
 */
@Composable
fun StandardizedInlineLoading(
    loadingState: LoadingState,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    AnimatedVisibility(
        visible = loadingState.isLoading,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            shape = RoundedCornerShape(8.dp)
        ) {
            LoadingContent(
                loadingState = loadingState,
                onCancel = onCancel,
                modifier = Modifier.padding(
                    if (compact) IOSSpacing.small else IOSSpacing.medium
                ),
                isCompact = compact
            )
        }
    }
}

/**
 * Button-integrated loading state
 */
@Composable
fun StandardizedLoadingButton(
    onClick: () -> Unit,
    loadingState: LoadingState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: String,
    loadingText: String? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding
) {
    val isLoading = loadingState.isLoading
    val currentText = if (isLoading) {
        loadingText ?: loadingState.message ?: "Loading..."
    } else {
        text
    }

    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled && !isLoading,
        colors = colors,
        contentPadding = contentPadding
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(IOSSpacing.small)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = colors.contentColor
                )
            }
            
            Text(
                text = currentText,
                style = IOSTypography.labelLarge.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

/**
 * Skeleton loading for list items
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StandardizedSkeletonLoader(
    count: Int = 6,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = IOSSpacing.medium,
            vertical = IOSSpacing.small
        ),
        verticalArrangement = Arrangement.spacedBy(IOSSpacing.small)
    ) {
        items(count) { index ->
            SkeletonItem(
                modifier = Modifier.animateItemPlacement(),
                delay = index * 50 // Staggered animation
            )
        }
    }
}

/**
 * Pull-to-refresh loading indicator
 */
@Composable
fun StandardizedPullToRefreshIndicator(
    isRefreshing: Boolean,
    progress: Float = 0f,
    onRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val rotation by rememberInfiniteTransition(label = "refresh_rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_animation"
    )

    Column(
        modifier = modifier.padding(IOSSpacing.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(IOSSpacing.small)
    ) {
        if (isRefreshing) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation),
                tint = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .rotate(if (progress > 0.8f) 180f else 0f),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = progress)
            )
        }

        AnimatedContent(
            targetState = when {
                isRefreshing -> "Refreshing..."
                progress > 0.8f -> "Release to refresh"
                progress > 0.3f -> "Pull to refresh"
                else -> ""
            },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "status_text"
        ) { text ->
            if (text.isNotEmpty()) {
                Text(
                    text = text,
                    style = IOSTypography.caption.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (isRefreshing) 0.8f else progress
                    )
                )
            }
        }
    }
}

/**
 * Internal loading content component
 */
@Composable
private fun LoadingContent(
    loadingState: LoadingState,
    onCancel: (() -> Unit)?,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    val progress = loadingState.progress
    val message = loadingState.message ?: "Loading..."
    val canCancel = loadingState.canCancel && onCancel != null

    if (isCompact) {
        // Compact horizontal layout
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(IOSSpacing.small)
        ) {
            LoadingIndicator(progress = progress)
            
            Text(
                text = message,
                style = IOSTypography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            if (canCancel) {
                TextButton(
                    onClick = { onCancel?.invoke() },
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Cancel",
                        style = IOSTypography.labelSmall
                    )
                }
            }
        }
    } else {
        // Full vertical layout
        Column(
            modifier = modifier.widthIn(min = 200.dp, max = 280.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(IOSSpacing.medium)
        ) {
            LoadingIndicator(progress = progress)
            
            if (progress != null) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = IOSTypography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Text(
                text = message,
                style = IOSTypography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            if (canCancel) {
                TextButton(
                    onClick = { onCancel?.invoke() }
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

/**
 * Standardized loading indicator
 */
@Composable
private fun LoadingIndicator(
    progress: Float?,
    modifier: Modifier = Modifier
) {
    if (progress != null) {
        LinearProgressIndicator(
            progress = progress,
            modifier = modifier
                .fillMaxWidth()
                .height(4.dp),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            color = MaterialTheme.colorScheme.primary
        )
    } else {
        CircularProgressIndicator(
            modifier = modifier.size(40.dp),
            strokeWidth = 3.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Individual skeleton item for loading lists
 */
@Composable
private fun SkeletonItem(
    modifier: Modifier = Modifier,
    delay: Int = 0
) {
    val alpha by rememberInfiniteTransition(label = "skeleton_alpha").animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                delayMillis = delay,
                easing = EaseInOutCubic
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_animation"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(IOSSpacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar skeleton
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.3f),
                        RoundedCornerShape(20.dp)
                    )
            )
            
            Spacer(modifier = Modifier.width(IOSSpacing.medium))
            
            // Content skeleton
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(IOSSpacing.small)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(16.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.4f),
                            RoundedCornerShape(8.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.3f),
                            RoundedCornerShape(7.dp)
                        )
                )
            }
        }
    }
}

/**
 * Convenience functions for common operations
 */
object StandardizedLoadingStates {
    fun loading(operation: LoadingOperation, progress: Float? = null, canCancel: Boolean = false) = 
        LoadingState.Loading(progress, operation.defaultMessage, canCancel)
    
    fun success(message: String? = null) = LoadingState.Success(message)
    
    fun error(error: Throwable, canRetry: Boolean = true) = LoadingState.Error(error, canRetry)
    
    val idle = LoadingState.Idle
    val starting = LoadingState.Starting
}