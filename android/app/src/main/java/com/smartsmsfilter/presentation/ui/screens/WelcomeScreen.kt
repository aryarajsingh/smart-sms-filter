package com.smartsmsfilter.presentation.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.smartsmsfilter.ui.theme.*
import com.smartsmsfilter.ui.utils.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onLearnMore: () -> Unit = {},
    onRequestPermissions: () -> Unit = {},
    onRequestDefaultSms: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val haptic = rememberHapticFeedback()
    // Removed unused scrollState
    
    // Stagger animations for features
    var showFeature1 by remember { mutableStateOf(false) }
    var showFeature2 by remember { mutableStateOf(false) }
    var showFeature3 by remember { mutableStateOf(false) }
    
    // Start animation after composition
    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
        delay(400)
        showFeature1 = true
        delay(150)
        showFeature2 = true
        delay(150)
        showFeature3 = true
    }
    
    // Sophisticated animations
    val logoScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.6f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )
    
    val contentOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 50f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "welcome_content_offset"
    )
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "welcome_content_alpha"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Subtle gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.03f)
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PremiumSpacing.Large)
                .offset(y = with(density) { contentOffset.dp }),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))
            
            // Premium animated logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(logoScale)
            ) {
                // Animated gradient background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                )
                
                // Icon with subtle animation
Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = "Smart SMS Filter logo",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(50.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(PremiumSpacing.XLarge))
            
            // Main headline
            Text(
                text = "A calmer, smarter SMS inbox",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = contentAlpha
                    }
            )
            
            Spacer(modifier = Modifier.height(PremiumSpacing.Medium))
            
            // Subheadline
            Text(
                text = "Smart SMS Filter automatically organizes your messages, so important ones never get lost in spam.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = contentAlpha
                    }
            )
            
            Spacer(modifier = Modifier.height(PremiumSpacing.XXLarge))
            
            // Feature highlights with stagger animation
            Column(
                verticalArrangement = Arrangement.spacedBy(PremiumSpacing.Large)
            ) {
                FadeSlideAnimation(
                    visible = showFeature1
                ) {
                    FeatureHighlight(
                        icon = Icons.Filled.AutoAwesome,
                        title = "Silences spam automatically",
                        description = "Advanced filtering keeps promotional messages quiet while prioritizing what matters.",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                FadeSlideAnimation(
                    visible = showFeature2
                ) {
                    FeatureHighlight(
                        icon = Icons.Filled.Speed,
                        title = "Prioritizes OTPs, banking & real people", 
                        description = "Important messages from banks, services, and contacts always reach your inbox.",
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                FadeSlideAnimation(
                    visible = showFeature3
                ) {
                    FeatureHighlight(
                        icon = Icons.Filled.Lock,
                        title = "100% private by design",
                        description = "All processing happens on your device. Your messages never leave your phone.",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(PremiumSpacing.XXLarge))
            
            // Why we need permissions section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = PremiumSpacing.Large),
                verticalArrangement = Arrangement.spacedBy(PremiumSpacing.Small)
            ) {
                // Privacy pledge
                Text(
                    text = "Privacy first: all processing happens on your device. No data leaves your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Why we need permissions",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "• SMS: to read and organize your messages on-device\n• Contacts: to show names instead of numbers\n• Notifications: to alert you only when it matters",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // No inline buttons here; Get Started handles permission prompts.
            }

            // Extra bottom padding to ensure content is visible above action buttons
            Spacer(modifier = Modifier.height(220.dp)) // Generous padding to ensure full scrollable rationale
        }
        
        // Bottom action area
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PremiumSpacing.Large)
                    .graphicsLayer {
                        alpha = contentAlpha
                    },
                verticalArrangement = Arrangement.spacedBy(PremiumSpacing.Small)
            ) {
                // Primary CTA with haptic feedback
                val primaryInteractionSource = remember { MutableInteractionSource() }
                val isPrimaryPressed by primaryInteractionSource.collectIsPressedAsState()
                
                // Soft gating: minimal checklist hint (button still enabled)
                val mainActivity = LocalContext.current as? com.smartsmsfilter.MainActivity
                val hasAllPermissions by (mainActivity?.hasAllCorePermissions ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
                val isDefaultSms by (mainActivity?.isDefaultSmsApp ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()

                Button(
                    onClick = {
                        haptic(HapticManager.FeedbackType.IMPACT_MEDIUM)
                        onGetStarted()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .bouncyPress(isPrimaryPressed),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    interactionSource = primaryInteractionSource
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Get Started",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Secondary CTA with subtle haptic
                TextButton(
                    onClick = {
                        haptic(HapticManager.FeedbackType.SELECTION)
                        onLearnMore()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Learn more",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureHighlight(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "feature_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                color = color.copy(alpha = pulseAlpha)
            )
            .padding(PremiumSpacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(PremiumSpacing.Medium),
        verticalAlignment = Alignment.Top
    ) {
        // Premium icon container with gradient
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            color.copy(alpha = 0.2f),
                            color.copy(alpha = 0.3f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = color
            )
        }
        
        // Text content with better typography
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp,
                    letterSpacing = 0.25.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
            )
        }
    }
}
