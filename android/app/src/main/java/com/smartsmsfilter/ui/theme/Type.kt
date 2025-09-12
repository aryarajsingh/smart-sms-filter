package com.smartsmsfilter.ui.theme

import androidx.compose.material3.Typography

// Bridge Material3 typography to our iOS-inspired ramp
val Typography = Typography(
    displayLarge = IOSTypography.LargeTitle,
    displayMedium = IOSTypography.Title1,
    displaySmall = IOSTypography.Title2,

    headlineLarge = IOSTypography.headlineLarge,
    headlineMedium = IOSTypography.headlineMedium,
    headlineSmall = IOSTypography.headlineSmall,

    titleLarge = IOSTypography.Title2,
    titleMedium = IOSTypography.titleMedium,
    titleSmall = IOSTypography.bodyMedium,

    bodyLarge = IOSTypography.bodyLarge,
    bodyMedium = IOSTypography.bodyMedium,
    bodySmall = IOSTypography.bodySmall,

    labelLarge = IOSTypography.labelLarge,
    labelMedium = IOSTypography.labelMedium,
    labelSmall = IOSTypography.labelSmall
)
