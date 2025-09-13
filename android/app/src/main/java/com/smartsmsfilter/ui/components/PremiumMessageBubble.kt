package com.smartsmsfilter.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.smartsmsfilter.domain.model.SmsMessage
import com.smartsmsfilter.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import android.text.format.DateUtils

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PremiumMessageBubble(
    message: SmsMessage,
    isStarred: Boolean = false,
    onLongPress: ((SmsMessage) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isOutgoing = message.isOutgoing
    // Improved timestamp display
    val timeText = when {
        DateUtils.isToday(message.timestamp.time) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp)
        }
        DateUtils.isToday(message.timestamp.time + DateUtils.DAY_IN_MILLIS) -> {
            "Yesterday ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp)}"
        }
        else -> {
            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(message.timestamp)
        }
    }
    val haptics = LocalHapticFeedback.current
    
    // Use dynamic colors from Material Theme
    val bubbleColor = if (isOutgoing) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (isOutgoing) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    // Premium gradient for sent messages
    val bubbleBackground = if (isOutgoing) {
        Brush.linearGradient(
            colors = listOf(
                bubbleColor,
                bubbleColor.copy(alpha = 0.8f)
            )
        )
    } else null
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onLongPress != null) {
                    Modifier.combinedClickable(
                        onClick = { /* Single tap - reserved for future use */ },
                        onLongClick = { 
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPress(message) 
                        }
                    )
                } else Modifier
            ),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
        ) {
            // Message bubble with premium styling
            Box(
                modifier = Modifier
                    .widthIn(max = PremiumSpacing.MessageBubbleMaxWidth)
                    .clip(
                        RoundedCornerShape(
                            topStart = PremiumSpacing.MessageBubbleCornerRadius,
                            topEnd = PremiumSpacing.MessageBubbleCornerRadius,
                            bottomStart = if (isOutgoing) PremiumSpacing.MessageBubbleCornerRadius else 6.dp,
                            bottomEnd = if (isOutgoing) 6.dp else PremiumSpacing.MessageBubbleCornerRadius
                        )
                    )
                    .then(
                        if (bubbleBackground != null) {
                            Modifier.background(bubbleBackground)
                        } else {
                            Modifier.background(bubbleColor)
                        }
                    )
                    .padding(PremiumSpacing.MessageBubblePadding)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = if (isOutgoing) FontWeight.Medium else FontWeight.Normal
                        ),
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Star indicator for starred messages with consistent gold color
                    if (isStarred) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Starred message",
                            tint = Color(0xFFFFD700), // Consistent gold color for all contexts
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Timestamp with premium styling and relative formatting
            Text(
                text = timeText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

/**
 * Premium conversation item for list display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumConversationItem(
    title: String,
    lastMessage: String,
    timestamp: String,
    unreadCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val backgroundColor by animateColorAsState(
        targetValue = if (isPressed) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(150),
        label = "conversation_item_background"
    )
    
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PremiumSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar placeholder or contact initial
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.firstOrNull()?.toString()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(PremiumSpacing.Medium))
            
            // Message content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
