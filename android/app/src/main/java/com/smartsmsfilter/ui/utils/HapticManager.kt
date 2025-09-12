package com.smartsmsfilter.ui.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Sophisticated haptic feedback manager for premium feel
 */
object HapticManager {
    
    enum class FeedbackType {
        // Light feedback
        SELECTION,      // Light tap for selections
        TICK,           // Very light tick for scrolling
        
        // Medium feedback  
        IMPACT_LIGHT,   // Light impact for button presses
        IMPACT_MEDIUM,  // Medium impact for toggles
        
        // Heavy feedback
        IMPACT_HEAVY,   // Heavy impact for important actions
        SUCCESS,        // Success pattern
        WARNING,        // Warning pattern
        ERROR,          // Error pattern
        
        // Custom patterns
        DOUBLE_TAP,     // Double tap pattern
        LONG_PRESS,     // Long press feedback
        SWIPE           // Swipe gesture feedback
    }
    
    fun performHapticFeedback(context: Context, type: FeedbackType) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (!vibrator.hasVibrator()) return
        
        when (type) {
            FeedbackType.SELECTION -> vibrateTick(vibrator)
            FeedbackType.TICK -> vibrateVeryLight(vibrator)
            FeedbackType.IMPACT_LIGHT -> vibrateLight(vibrator)
            FeedbackType.IMPACT_MEDIUM -> vibrateMedium(vibrator)
            FeedbackType.IMPACT_HEAVY -> vibrateHeavy(vibrator)
            FeedbackType.SUCCESS -> vibrateSuccess(vibrator)
            FeedbackType.WARNING -> vibrateWarning(vibrator)
            FeedbackType.ERROR -> vibrateError(vibrator)
            FeedbackType.DOUBLE_TAP -> vibrateDoubleTap(vibrator)
            FeedbackType.LONG_PRESS -> vibrateLongPress(vibrator)
            FeedbackType.SWIPE -> vibrateSwipe(vibrator)
        }
    }
    
    private fun vibrateTick(vibrator: Vibrator) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(5, 50))
        }
    }
    
    private fun vibrateVeryLight(vibrator: Vibrator) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(3, 30))
        }
    }
    
    private fun vibrateLight(vibrator: Vibrator) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(10, 100))
        }
    }
    
    private fun vibrateMedium(vibrator: Vibrator) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(20, 150))
        }
    }
    
    private fun vibrateHeavy(vibrator: Vibrator) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, 200))
        }
    }
    
    private fun vibrateSuccess(vibrator: Vibrator) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 50, 50, 50)
            val amplitudes = intArrayOf(0, 100, 0, 200)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }
    
    private fun vibrateWarning(vibrator: Vibrator) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 100, 100, 100)
            val amplitudes = intArrayOf(0, 150, 0, 150)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }
    
    private fun vibrateError(vibrator: Vibrator) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 100, 50, 100, 50, 100)
            val amplitudes = intArrayOf(0, 200, 0, 200, 0, 200)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }
    
    private fun vibrateDoubleTap(vibrator: Vibrator) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 50, 100, 50)
            val amplitudes = intArrayOf(0, 150, 0, 150)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }
    
    private fun vibrateLongPress(vibrator: Vibrator) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(100, 150))
        }
    }
    
    private fun vibrateSwipe(vibrator: Vibrator) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 20, 20, 20, 20, 20)
            val amplitudes = intArrayOf(0, 50, 0, 100, 0, 150)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        }
    }
}

/**
 * Composable helper for haptic feedback
 */
@Composable
fun rememberHapticFeedback(): (HapticManager.FeedbackType) -> Unit {
    val context = LocalContext.current
    return remember {
        { type -> HapticManager.performHapticFeedback(context, type) }
    }
}

/**
 * Extension function for View-based haptic feedback
 */
fun View.performHaptic(type: HapticManager.FeedbackType = HapticManager.FeedbackType.IMPACT_LIGHT) {
    when (type) {
        HapticManager.FeedbackType.SELECTION,
        HapticManager.FeedbackType.TICK -> {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        HapticManager.FeedbackType.LONG_PRESS -> {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
        else -> {
            performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }
}
