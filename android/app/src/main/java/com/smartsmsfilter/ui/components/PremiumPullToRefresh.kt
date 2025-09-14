package com.smartsmsfilter.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import com.smartsmsfilter.ui.theme.IOSSpacing
import com.smartsmsfilter.ui.theme.IOSTypography
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min

@Composable
fun PremiumPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var pullDistance by remember { mutableFloatStateOf(0f) }
    var isReleased by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    val maxPullDistance = with(density) { 120.dp.toPx() }
    val triggerDistance = with(density) { 80.dp.toPx() }

    // Spring animation for pull distance
    val animatedPullDistance by animateFloatAsState(
        targetValue = if (isRefreshing) triggerDistance else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pull_distance"
    )

    val shouldTriggerRefresh = pullDistance >= triggerDistance && !isRefreshing

    LaunchedEffect(isReleased, shouldTriggerRefresh) {
        if (isReleased && shouldTriggerRefresh) {
            onRefresh()
            isReleased = false
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            pullDistance = 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isRefreshing) {
                if (!isRefreshing) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            isReleased = true
                            if (!shouldTriggerRefresh) {
                                pullDistance = 0f
                            }
                        }
                    ) { _, dragAmount ->
                        if (dragAmount > 0) {
                            pullDistance = min(pullDistance + dragAmount * 0.5f, maxPullDistance)
                        } else if (pullDistance > 0) {
                            pullDistance = (pullDistance + dragAmount * 0.8f).coerceAtLeast(0f)
                        }
                    }
                }
            }
    ) {
        // Pull to refresh indicator
        val indicatorAlpha = (pullDistance / triggerDistance).coerceIn(0f, 1f)
        val indicatorScale = (pullDistance / triggerDistance * 0.8f + 0.2f).coerceIn(0.2f, 1f)

        if (pullDistance > 0 || isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { (if (isRefreshing) animatedPullDistance else pullDistance).toDp() })
                    .graphicsLayer {
                        alpha = if (isRefreshing) 1f else indicatorAlpha
                        scaleX = indicatorScale
                        scaleY = indicatorScale
                    },
                contentAlignment = Alignment.Center
            ) {
                PullToRefreshIndicator(
                    isRefreshing = isRefreshing,
                    shouldTrigger = shouldTriggerRefresh,
                    progress = (pullDistance / triggerDistance).coerceIn(0f, 1f)
                )
            }
        }

        // Content with offset
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = if (isRefreshing) animatedPullDistance else pullDistance
                }
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun PullToRefreshIndicator(
    isRefreshing: Boolean,
    shouldTrigger: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (shouldTrigger && !isRefreshing) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "arrow_rotation"
    )

    val infiniteRotation by rememberInfiniteTransition(label = "loading_rotation").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_animation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(IOSSpacing.small),
        modifier = modifier.padding(IOSSpacing.medium)
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .rotate(infiniteRotation),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            // Custom arrow or progress indicator
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation)
                    .scale(progress.coerceIn(0.5f, 1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = progress),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Status text
        AnimatedContent(
            targetState = when {
                isRefreshing -> "Refreshing..."
                shouldTrigger -> "Release to refresh"
                progress > 0.3f -> "Pull to refresh"
                else -> ""
            },
transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
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
