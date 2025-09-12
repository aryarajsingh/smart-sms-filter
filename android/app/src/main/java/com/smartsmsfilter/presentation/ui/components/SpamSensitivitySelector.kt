package com.smartsmsfilter.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsmsfilter.data.preferences.SpamTolerance
import com.smartsmsfilter.ui.utils.HapticManager

@Composable
fun SpamSensitivitySelector(
    selectedTolerance: SpamTolerance,
    onToleranceChange: (SpamTolerance) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = rememberHapticFeedback()
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "How sensitive should spam detection be?",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Choose your protection level",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Tolerance options
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Low Tolerance - Aggressive filtering
            SensitivityCard(
                tolerance = SpamTolerance.LOW,
                title = "Maximum Protection",
                description = "Blocks most promotional content aggressively",
                icon = Icons.Default.Shield,
                iconColor = MaterialTheme.colorScheme.error,
                examples = listOf(
                    "✓ All promotional SMS",
                    "✓ Marketing offers",
                    "✓ Most notifications",
                    "⚠ May block some legitimate messages"
                ),
                isSelected = selectedTolerance == SpamTolerance.LOW,
                onClick = {
                    haptic(HapticManager.FeedbackType.IMPACT_MEDIUM)
                    onToleranceChange(SpamTolerance.LOW)
                }
            )
            
            // Moderate Tolerance - Balanced
            SensitivityCard(
                tolerance = SpamTolerance.MODERATE,
                title = "Balanced Protection",
                description = "Smart filtering with good accuracy",
                icon = Icons.Default.Balance,
                iconColor = MaterialTheme.colorScheme.primary,
                examples = listOf(
                    "✓ Obvious spam & scams",
                    "✓ Aggressive promotions",
                    "○ Some promotional content allowed",
                    "✓ Important messages protected"
                ),
                isSelected = selectedTolerance == SpamTolerance.MODERATE,
                isRecommended = true,
                onClick = {
                    haptic(HapticManager.FeedbackType.IMPACT_LIGHT)
                    onToleranceChange(SpamTolerance.MODERATE)
                }
            )
            
            // High Tolerance - Lenient
            SensitivityCard(
                tolerance = SpamTolerance.HIGH,
                title = "Minimal Filtering",
                description = "Only blocks obvious spam",
                icon = Icons.Default.OpenInFull,
                iconColor = MaterialTheme.colorScheme.tertiary,
                examples = listOf(
                    "✓ Only clear scams",
                    "✗ Most promotions allowed",
                    "✗ Marketing messages pass through",
                    "✓ Nothing important blocked"
                ),
                isSelected = selectedTolerance == SpamTolerance.HIGH,
                onClick = {
                    haptic(HapticManager.FeedbackType.IMPACT_LIGHT)
                    onToleranceChange(SpamTolerance.HIGH)
                }
            )
        }
    }
}

@Composable
private fun SensitivityCard(
    tolerance: SpamTolerance,
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    examples: List<String>,
    isSelected: Boolean,
    isRecommended: Boolean = false,
    onClick: () -> Unit
) {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) iconColor else Color.Transparent,
        animationSpec = tween(300),
        label = "border_color"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(animatedScale)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                iconColor.copy(alpha = 0.1f)
            else 
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon container
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (isSelected) iconColor.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (isSelected) iconColor 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isSelected) iconColor 
                                        else MaterialTheme.colorScheme.onSurface
                            )
                            
                            if (isRecommended) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "RECOMMENDED",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Selection indicator
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Examples section
            if (isSelected) {
                AnimatedVisibility(
                    visible = true,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "What gets filtered:",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        examples.forEach { example ->
                            Text(
                                text = example,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun rememberHapticFeedback(): (HapticManager.FeedbackType) -> Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    return remember {
        { feedbackType ->
            HapticManager.performHapticFeedback(context, feedbackType)
        }
    }
}
