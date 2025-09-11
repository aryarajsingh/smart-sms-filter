package com.smartsmsfilter.ui.theme

import androidx.compose.ui.graphics.Color

// iOS-esque dynamic color palette that works with Material You
object PremiumColors {
    // Primary blue (iOS-inspired but adaptable)
    val Blue = Color(0xFF007AFF)
    val BlueLight = Color(0xFF5AC8FA)
    val BlueDark = Color(0xFF0051D0)
    
    // System Grays (iOS)
    val Gray = Color(0xFF8E8E93)
    val Gray2 = Color(0xFFAEAEB2)
    val Gray3 = Color(0xFFC7C7CC)
    val Gray4 = Color(0xFFD1D1D6)
    val Gray5 = Color(0xFFE5E5EA)
    val Gray6 = Color(0xFFF2F2F7)
    
    // Text Colors
    val Label = Color(0xFF000000)
    val SecondaryLabel = Color(0xFF3C3C43).copy(alpha = 0.6f)
    val TertiaryLabel = Color(0xFF3C3C43).copy(alpha = 0.3f)
    val PlaceholderText = Color(0xFF3C3C43).copy(alpha = 0.3f)
    
    // Background Colors
    val SystemBackground = Color(0xFFFFFFFF)
    val SecondarySystemBackground = Color(0xFFF2F2F7)
    val TertiarySystemBackground = Color(0xFFFFFFFF)
    val GroupedBackground = Color(0xFFF2F2F7)
    val SecondaryGroupedBackground = Color(0xFFFFFFFF)
    
    // Status Colors
    val Green = Color(0xFF34C759)
    val Red = Color(0xFFFF3B30)
    val Orange = Color(0xFFFF9500)
    val Yellow = Color(0xFFFFCC00)
    
    // Message Bubble Colors
    val MessageBubbleSent = Blue
    val MessageBubbleReceived = Gray6
    val MessageBubbleSentText = Color.White
    val MessageBubbleReceivedText = Label
    
    // Dark Mode Colors
    object Dark {
        val Label = Color(0xFFFFFFFF)
        val SecondaryLabel = Color(0xFFEBEBF5).copy(alpha = 0.6f)
        val TertiaryLabel = Color(0xFFEBEBF5).copy(alpha = 0.3f)
        val PlaceholderText = Color(0xFFEBEBF5).copy(alpha = 0.3f)
        
        val SystemBackground = Color(0xFF000000)
        val SecondarySystemBackground = Color(0xFF1C1C1E)
        val TertiarySystemBackground = Color(0xFF2C2C2E)
        val GroupedBackground = Color(0xFF000000)
        val SecondaryGroupedBackground = Color(0xFF1C1C1E)
        
        val MessageBubbleReceived = Color(0xFF3A3A3C)
        val MessageBubbleReceivedText = Color.White
    }
}
