package com.smartsmsfilter.ui.utils

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Premium spring animation spec for sophisticated feel
 */
val PremiumSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
)

/**
 * Smooth spring for subtle animations
 */
val SmoothSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium
)

/**
 * Quick spring for responsive interactions
 */
val QuickSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessHigh
)

/**
 * Premium enter transition with scale and fade
 */
val premiumEnterTransition = scaleIn(
    animationSpec = tween(300, easing = FastOutSlowInEasing),
    initialScale = 0.92f
) + fadeIn(
    animationSpec = tween(300)
)

/**
 * Premium exit transition with scale and fade
 */
val premiumExitTransition = scaleOut(
    animationSpec = tween(200, easing = FastOutLinearInEasing),
    targetScale = 0.92f
) + fadeOut(
    animationSpec = tween(200)
)

/**
 * Shimmer effect for loading states
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    widthOfShadowBrush: Int = 200,
    angleOfAxisY: Float = 270f,
    durationMillis: Int = 1000,
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surface.copy(alpha = 1.0f),
        MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")

    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = widthOfShadowBrush + 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset(
            x = translateAnimation.value - widthOfShadowBrush,
            y = 0.0f
        ),
        end = androidx.compose.ui.geometry.Offset(
            x = translateAnimation.value,
            y = angleOfAxisY
        ),
    )

    Box(
        modifier = modifier.background(brush)
    )
}

/**
 * Pulse animation for attention-grabbing elements
 */
@Composable
fun PulseAnimation(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    Box(
        modifier = modifier.scale(scale),
        content = { content() }
    )
}

/**
 * Sophisticated success animation
 */
@Composable
fun SuccessAnimation(
    visible: Boolean,
    onComplete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        LaunchedEffect(Unit) {
            delay(1500)
            onComplete()
        }
        
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Success checkmark or icon would go here
        }
    }
}

/**
 * Bouncy press animation for buttons
 */
@Composable
fun Modifier.bouncyPress(
    isPressed: Boolean
): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = if (isPressed) {
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessHigh
            )
        } else {
            spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium
            )
        },
        label = "bouncy_press"
    )
    
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Parallax scroll effect modifier
 */
fun Modifier.parallaxScroll(
    scrollState: Float,
    rate: Float = 0.5f
): Modifier {
    return this.graphicsLayer {
        translationY = scrollState * rate
    }
}

/**
 * Sophisticated fade and slide animation
 */
@Composable
fun FadeSlideAnimation(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 4 },
            animationSpec = tween(300, easing = FastOutSlowInEasing)
        ) + fadeIn(
            animationSpec = tween(300)
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it / 4 },
            animationSpec = tween(200, easing = FastOutLinearInEasing)
        ) + fadeOut(
            animationSpec = tween(200)
        ),
        modifier = modifier
    ) {
        content()
    }
}

/**
 * Stagger animation for lists
 */
@Composable
fun <T> StaggeredAnimatedList(
    items: List<T>,
    delayMillis: Int = 50,
    content: @Composable (T, Int) -> Unit
) {
    items.forEachIndexed { index, item ->
        var visible by remember { mutableStateOf(false) }
        
        LaunchedEffect(key1 = item) {
            delay(index * delayMillis.toLong())
            visible = true
        }
        
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(
                animationSpec = tween(300)
            )
        ) {
            content(item, index)
        }
    }
}

/**
 * Gradient animation for dynamic backgrounds
 */
@Composable
fun AnimatedGradientBackground(
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_offset"
    )
    
    Box(
        modifier = modifier.background(
            brush = Brush.linearGradient(
                colors = colors,
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(
                    1000f * animatedOffset,
                    1000f * animatedOffset
                )
            )
        )
    )
}

/**
 * Sophisticated reveal animation
 */
@Composable
fun RevealAnimation(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transition = updateTransition(targetState = visible, label = "reveal")
    
    val scale by transition.animateFloat(
        transitionSpec = {
            if (targetState) {
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            } else {
                tween(200)
            }
        },
        label = "reveal_scale"
    ) { isVisible ->
        if (isVisible) 1f else 0f
    }
    
    val alpha by transition.animateFloat(
        transitionSpec = { tween(if (targetState) 300 else 200) },
        label = "reveal_alpha"
    ) { isVisible ->
        if (isVisible) 1f else 0f
    }
    
    if (visible || transition.currentState || transition.targetState) {
        Box(
            modifier = modifier
                .scale(scale)
                .alpha(alpha)
        ) {
            content()
        }
    }
}
