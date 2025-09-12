package com.smartsmsfilter.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.smartsmsfilter.domain.model.SmsMessage

/**
 * Swipeable message card with selection support
 * For now, swipe actions are accessible through action bar during selection mode
 */
@Composable
fun SwipeableMessageCard(
    message: SmsMessage,
    onClick: () -> Unit,
    onArchive: () -> Unit, // Reserved for future swipe implementation
    onDelete: () -> Unit, // Reserved for future swipe implementation  
    modifier: Modifier = Modifier,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onLongPress: (() -> Unit)? = null,
    onSelectionToggle: (() -> Unit)? = null,
    showCategory: Boolean = true,
    onWhyRequested: (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    
    PremiumConversationCard(
        message = message,
        onClick = onClick,
        showCategory = showCategory,
        isSelectionMode = isSelectionMode,
        isSelected = isSelected,
        onLongPress = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongPress?.invoke()
        },
        onSelectionToggle = onSelectionToggle,
        onWhyRequested = onWhyRequested,
        modifier = modifier
    )
}
