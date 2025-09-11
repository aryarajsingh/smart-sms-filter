package com.smartsmsfilter.presentation.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.smartsmsfilter.ui.theme.*

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onLearnMore: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    
    // Start animation after composition
    LaunchedEffect(Unit) {
        delay(150)
        isVisible = true
    }
    
    // Spring animation for content
    val contentOffset by animateFloatAsState(
        targetValue = if (isVisible) 0f else 100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "welcome_content_offset"
    )
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(600, easing = EaseOutCubic),
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
            
            // App logo area (placeholder for now)
            Card(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.SmartToy,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
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
            
            // Feature highlights
            Column(
                verticalArrangement = Arrangement.spacedBy(PremiumSpacing.Large),
                modifier = Modifier.graphicsLayer {
                    alpha = contentAlpha
                }
            ) {
                FeatureHighlight(
                    icon = Icons.Outlined.SmartToy,
                    title = "Silences spam automatically",
                    description = "Advanced filtering keeps promotional messages quiet while prioritizing what matters."
                )
                
                FeatureHighlight(
                    icon = Icons.Outlined.Speed,
                    title = "Prioritizes OTPs, banking & real people", 
                    description = "Important messages from banks, services, and contacts always reach your inbox."
                )
                
                FeatureHighlight(
                    icon = Icons.Outlined.Security,
                    title = "100% private by design",
                    description = "All processing happens on your device. Your messages never leave your phone."
                )
            }
            
            Spacer(modifier = Modifier.height(PremiumSpacing.XXLarge))
            
            // Extra bottom padding to ensure content is visible above action buttons
            Spacer(modifier = Modifier.height(120.dp)) // Height of bottom action area + safe margin
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
                // Primary CTA
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(PremiumCornerRadius.Large),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                
                // Secondary CTA
                TextButton(
                    onClick = onLearnMore,
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PremiumSpacing.Medium),
        verticalAlignment = Alignment.Top
    ) {
        // Icon container
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        // Text content
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
