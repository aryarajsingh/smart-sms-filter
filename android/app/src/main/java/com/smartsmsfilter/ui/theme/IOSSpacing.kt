package com.smartsmsfilter.ui.theme

import androidx.compose.ui.unit.dp

// Premium spacing system with iOS-esque proportions
object PremiumSpacing {
    // Base spacing unit (4dp)
    val Base = 4.dp
    
    // Common spacing values following iOS HIG
    val XSmall = 4.dp      // 4dp  - Tight spacing
    val Small = 8.dp       // 8dp  - Small spacing
    val Medium = 16.dp     // 16dp - Standard spacing
    val Large = 24.dp      // 24dp - Large spacing
    val XLarge = 32.dp     // 32dp - Extra large spacing
    val XXLarge = 48.dp    // 48dp - Maximum spacing
    
    // Screen-level padding
    val ScreenHorizontal = 16.dp
    val ScreenVertical = 16.dp
    val ScreenTop = 20.dp
    
    // Component-specific spacing
    val ListItemVertical = 12.dp
    val ListItemHorizontal = 16.dp
    val ListItemIconSpacing = 12.dp
    
    // Message bubbles
    val MessageBubbleCornerRadius = 20.dp
    val MessageBubblePadding = 12.dp
    val MessageBubbleSpacing = 8.dp
    val MessageBubbleMaxWidth = 280.dp
    
    // Input fields
    val InputFieldCornerRadius = 10.dp
    val InputFieldPadding = 12.dp
    val InputFieldVerticalPadding = 8.dp
    
    // Buttons
    val ButtonCornerRadius = 10.dp
    val ButtonPadding = 16.dp
    val ButtonVerticalPadding = 12.dp
    
    // Cards and containers
    val CardCornerRadius = 12.dp
    val CardPadding = 16.dp
    val CardElevation = 0.dp // iOS uses subtle shadows, not elevation
    
    // Navigation
    val BottomNavHeight = 83.dp // iOS standard bottom tab bar height
    val BottomNavSafeArea = 34.dp // iOS home indicator space
    val TopNavHeight = 44.dp // iOS navigation bar height
    
    // Shadows (subtle iOS-style)
    val ShadowElevation = 1.dp
    val ShadowRadius = 4.dp
}

// iOS-inspired corner radius system
object PremiumCornerRadius {
    val None = 0.dp
    val Small = 6.dp
    val Medium = 10.dp
    val Large = 12.dp
    val XLarge = 16.dp
    val XXLarge = 20.dp
    val Circular = 50.dp
}

// iOS-inspired elevation/shadow system
object PremiumElevation {
    val None = 0.dp
    val Level1 = 1.dp  // Subtle shadow for cards
    val Level2 = 2.dp  // Slightly more prominent
    val Level3 = 4.dp  // For modals and overlays
    val Level4 = 8.dp  // For floating elements
    val Level5 = 16.dp // For the highest elevation elements
}

// IOSSpacing object with both capitalized and lowercase properties
object IOSSpacing {
    // Capitalized versions (matching PremiumSpacing)
    val Small = PremiumSpacing.Small
    val Medium = PremiumSpacing.Medium
    val Large = PremiumSpacing.Large
    val XSmall = PremiumSpacing.XSmall
    val XLarge = PremiumSpacing.XLarge
    
    // Lowercase versions for component compatibility
    @get:JvmName("getExtraSmallLowercase")
    val extraSmall = PremiumSpacing.XSmall
    @get:JvmName("getSmallLowercase")
    val small = PremiumSpacing.Small
    @get:JvmName("getMediumLowercase")
    val medium = PremiumSpacing.Medium
    @get:JvmName("getLargeLowercase")
    val large = PremiumSpacing.Large
}
