package com.smartsmsfilter.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsmsfilter.domain.model.MessageCategory
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.ui.theme.IOSSpacing
import com.smartsmsfilter.ui.theme.IOSTypography
import com.smartsmsfilter.ui.utils.formatRelativeTime
import com.smartsmsfilter.ui.utils.getSenderDisplayName

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PremiumConversationCard(
    message: SmsMessage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showCategory: Boolean = true,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onSelectionToggle: (() -> Unit)? = null,
    onWhyRequested: (() -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    
    // Enhanced animations
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "card_scale"
    )
    
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 2f else if (isSelected) 6f else 4f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "card_elevation"
    )
    
    // Selection mode animations
    val selectionAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.8f else 1f,
        animationSpec = tween(300),
        label = "selection_alpha"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(300),
        label = "border_color"
    )

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = selectionAlpha
            }
            .then(
                if (isSelectionMode) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = rememberRipple(bounded = true, radius = 300.dp)
                    ) {
                        onSelectionToggle?.invoke()
                    }
                } else {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = rememberRipple(bounded = true, radius = 300.dp),
                        onClick = onClick,
                        onLongClick = {
                            onLongPress?.invoke()
                        }
                    )
                }
            ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = elevation.dp
        ),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            }
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(IOSSpacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection checkbox (only visible in selection mode)
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = slideInHorizontally() + fadeIn(),
                exit = slideOutHorizontally() + fadeOut()
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectionToggle?.invoke() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                )
                Spacer(modifier = Modifier.width(IOSSpacing.small))
            }
            
            // Contact Avatar with category indicator
            ContactAvatar(
                sender = message.sender,
                category = message.category,
                isUnread = !message.isRead
            )
            
            Spacer(modifier = Modifier.width(IOSSpacing.medium))
            
            // Message Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(IOSSpacing.small)
            ) {
                // Header row with sender and time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = getSenderDisplayName(message.sender),
                        style = IOSTypography.bodyLarge.copy(
                            fontWeight = if (!message.isRead) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(IOSSpacing.small))
                    
                    TimeStamp(
                        timestamp = message.timestamp.time,
                        isUnread = !message.isRead
                    )
                }
                
                // Message preview
                Text(
                    text = message.content,
                    style = IOSTypography.bodyMedium.copy(
                        fontWeight = if (!message.isRead) FontWeight.Medium else FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = if (!message.isRead) 0.9f else 0.7f
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Category badge and Why? button
                Row(
                    horizontalArrangement = Arrangement.spacedBy(IOSSpacing.small),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showCategory) {
                        CategoryBadge(
                            category = message.category,
                        modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    if (onWhyRequested != null) {
                        CompositionLocalProvider(androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement provides false) {
                            IconButton(onClick = onWhyRequested, modifier = Modifier.size(28.dp)) {
Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Help,
                                    contentDescription = "Why is this classified?",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Unread indicator and chevron
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(IOSSpacing.small)
            ) {
                if (!message.isRead) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                CircleShape
                            )
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ContactAvatar(
    sender: String,
    category: MessageCategory,
    isUnread: Boolean,
    modifier: Modifier = Modifier
) {
    val avatarColor = getAvatarColor(sender)
    val categoryIcon = getCategoryIcon(category)
    
    Box(
        modifier = modifier.size(52.dp),
        contentAlignment = Alignment.Center
    ) {
        // Main avatar circle
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    brush = if (isUnread) {
                        Brush.radialGradient(
                            colors = listOf(
                                avatarColor.copy(alpha = 0.8f),
                                avatarColor
                            )
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(
                                avatarColor.copy(alpha = 0.6f),
                                avatarColor.copy(alpha = 0.8f)
                            )
                        )
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = getSenderInitials(sender),
                style = IOSTypography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
        
        // Category indicator
        Box(
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.BottomEnd)
                .background(
                    MaterialTheme.colorScheme.surface,
                    CircleShape
                )
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIcon,
                contentDescription = null,
                tint = getCategoryColor(category),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun TimeStamp(
    timestamp: Long,
    isUnread: Boolean,
    modifier: Modifier = Modifier
) {
    val timeText = remember(timestamp) {
        formatRelativeTime(timestamp)
    }
    
    Text(
        text = timeText,
        style = IOSTypography.caption.copy(
            fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal,
            fontSize = 12.sp
        ),
        color = MaterialTheme.colorScheme.onSurface.copy(
            alpha = if (isUnread) 0.7f else 0.5f
        ),
        modifier = modifier
    )
}

@Composable
fun CategoryBadge(
    category: MessageCategory,
    modifier: Modifier = Modifier
) {
    val (text, color) = when (category) {
        MessageCategory.INBOX -> "Important" to MaterialTheme.colorScheme.primary
        MessageCategory.SPAM -> "Spam" to MaterialTheme.colorScheme.tertiary
        MessageCategory.NEEDS_REVIEW -> "Review" to MaterialTheme.colorScheme.secondary
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            style = IOSTypography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = 10.sp
            ),
            color = color,
            modifier = Modifier.padding(
                horizontal = IOSSpacing.small,
                vertical = 2.dp
            )
        )
    }
}

// Helper functions

private fun getSenderInitials(sender: String): String {
    val displayName = getSenderDisplayName(sender)
    return when {
        displayName.contains(" ") -> {
            displayName.split(" ")
                .take(2)
                .map { it.firstOrNull()?.uppercaseChar() ?: "" }
                .joinToString("")
        }
        displayName.length >= 2 -> displayName.take(2).uppercase()
        else -> displayName.take(1).uppercase()
    }
}

private fun getAvatarColor(sender: String): Color {
    val colors = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFF8B5CF6), // Purple  
        Color(0xFFEC4899), // Pink
        Color(0xFFEF4444), // Red
        Color(0xFFF59E0B), // Amber
        Color(0xFF10B981), // Emerald
        Color(0xFF06B6D4), // Cyan
        Color(0xFF3B82F6), // Blue
    )
    
    val hash = sender.hashCode()
    return colors[Math.abs(hash) % colors.size]
}

private fun getCategoryIcon(category: MessageCategory): ImageVector {
    return when (category) {
        MessageCategory.INBOX -> Icons.Filled.Star
        MessageCategory.SPAM -> Icons.Outlined.FilterList
        MessageCategory.NEEDS_REVIEW -> Icons.Outlined.Help
    }
}

private fun getCategoryColor(category: MessageCategory): Color {
    return when (category) {
        MessageCategory.INBOX -> Color(0xFF10B981) // Green
        MessageCategory.SPAM -> Color(0xFF6B7280) // Gray
        MessageCategory.NEEDS_REVIEW -> Color(0xFFF59E0B) // Amber
    }
}

