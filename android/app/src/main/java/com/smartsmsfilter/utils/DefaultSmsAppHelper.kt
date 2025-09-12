package com.smartsmsfilter.utils

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

/**
 * Helper class for managing default SMS app status
 */
class DefaultSmsAppHelper(private val activity: ComponentActivity) {
    
    companion object {
        const val REQUEST_CODE_SET_DEFAULT_SMS = 1001
        
        /**
         * Check if our app is the default SMS app
         */
        fun isDefaultSmsApp(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Prefer RoleManager on Android 10+
                try {
                    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                    roleManager.isRoleHeld(RoleManager.ROLE_SMS)
                } catch (e: Exception) {
                    // Fallback to Telephony API if RoleManager is unavailable
                    val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context)
                    defaultSmsPackage == context.packageName
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context)
                defaultSmsPackage == context.packageName
            } else {
                // Pre-KitKat, there's no default SMS app concept
                true
            }
        }
        
        /**
         * Launch the default SMS app setting intent directly
         * This doesn't track the result but avoids lifecycle issues
         */
        fun launchDefaultSmsSettings(context: Context) {
            if (isDefaultSmsApp(context)) {
                return
            }
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Android 10+ uses RoleManager
                    val roleManager = context.getSystemService(Context.ROLE_SERVICE) as RoleManager
                    if (roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } else {
                        // Role not available, try alternative method
                        launchAlternativeSettings(context)
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    // Android 4.4 to 9 uses Telephony.Sms
                    val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                        putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                // If the specific intent fails, open general app settings
                launchAlternativeSettings(context)
            }
        }
        
        /**
         * Launch alternative settings if the default SMS intent fails
         */
        private fun launchAlternativeSettings(context: Context) {
            try {
                // Try to open the app's specific settings page
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                // Last resort: open general settings
                val intent = Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        }
    }
    
    private var resultCallback: ((Boolean) -> Unit)? = null
    
    private val defaultSmsLauncher: ActivityResultLauncher<Intent> = 
        activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val isNowDefault = isDefaultSmsApp(activity)
            resultCallback?.invoke(isNowDefault)
            resultCallback = null
        }
    
    /**
     * Request to set our app as the default SMS app
     */
    fun requestDefaultSmsApp(onResult: (Boolean) -> Unit) {
        if (isDefaultSmsApp(activity)) {
            onResult(true)
            return
        }
        
        resultCallback = onResult
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ uses RoleManager
            val roleManager = activity.getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (!roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                onResult(false)
                return
            }
            
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
            defaultSmsLauncher.launch(intent)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Android 4.4 to 9 uses Telephony.Sms
            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, activity.packageName)
            }
            defaultSmsLauncher.launch(intent)
        } else {
            // Pre-KitKat doesn't have default SMS app
            onResult(true)
        }
    }
}

/**
 * Composable dialog for setting default SMS app
 */
@Composable
fun DefaultSmsAppDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
Icon(
                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.Message,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Set as Default SMS App",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = "To use all features and ensure proper message filtering, Smart SMS Filter needs to be your default SMS app.\n\n" +
                       "This allows us to:\n" +
                       "• Automatically categorize incoming messages\n" +
                       "• Provide smart notifications\n" +
                       "• Manage your SMS inbox efficiently\n\n" +
                       "You can change this anytime in Settings.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Set as Default")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        }
    )
}

/**
 * Composable that handles the complete flow for setting default SMS app
 */
@Composable
fun rememberDefaultSmsAppLauncher(
    activity: ComponentActivity = LocalContext.current as ComponentActivity,
    onResult: (Boolean) -> Unit = {}
): () -> Unit {
    val helper = remember { DefaultSmsAppHelper(activity) }
    val scope = rememberCoroutineScope()
    
    return remember {
        {
            scope.launch {
                helper.requestDefaultSmsApp { isDefault ->
                    onResult(isDefault)
                }
            }
        }
    }
}

/**
 * Show a snackbar prompt for setting default SMS app
 */
@Composable
fun DefaultSmsAppSnackbar(
    snackbarHostState: SnackbarHostState,
    onSetDefault: () -> Unit
) {
    val context = LocalContext.current
    val isDefault = remember { DefaultSmsAppHelper.isDefaultSmsApp(context) }
    
    if (!isDefault) {
        LaunchedEffect(Unit) {
            val result = snackbarHostState.showSnackbar(
                message = "Set as default SMS app for full functionality",
                actionLabel = "Set Default",
                duration = SnackbarDuration.Long
            )
            
            if (result == SnackbarResult.ActionPerformed) {
                onSetDefault()
            }
        }
    }
}
