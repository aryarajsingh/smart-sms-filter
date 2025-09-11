package com.smartsmsfilter.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// iOS-esque fallback colors for when dynamic colors aren't available
private val DarkColorScheme = darkColorScheme(
    primary = PremiumColors.Blue,
    onPrimary = PremiumColors.Dark.Label,
    secondary = PremiumColors.Dark.SecondarySystemBackground,
    onSecondary = PremiumColors.Dark.Label,
    background = PremiumColors.Dark.SystemBackground,
    onBackground = PremiumColors.Dark.Label,
    surface = PremiumColors.Dark.SecondarySystemBackground,
    onSurface = PremiumColors.Dark.Label
)

private val LightColorScheme = lightColorScheme(
    primary = PremiumColors.Blue,
    onPrimary = PremiumColors.MessageBubbleSentText,
    secondary = PremiumColors.SecondarySystemBackground,
    onSecondary = PremiumColors.Label,
    background = PremiumColors.SystemBackground,
    onBackground = PremiumColors.Label,
    surface = PremiumColors.SecondarySystemBackground,
    onSurface = PremiumColors.Label
)

@Composable
fun SmartSmsFilterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true, // Enable dynamic colors
    content: @Composable () -> Unit
) {
    // Use dynamic colors when available (Android 12+) but with iOS-esque refinements
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
