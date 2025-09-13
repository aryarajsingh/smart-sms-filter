package com.smartsmsfilter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.smartsmsfilter.presentation.ui.navigation.NavRoutes
import com.smartsmsfilter.presentation.ui.screens.*
import com.smartsmsfilter.presentation.viewmodel.OnboardingViewModel
import com.smartsmsfilter.data.preferences.PreferencesManager
import com.smartsmsfilter.ui.theme.SmartSmsFilterTheme
import com.smartsmsfilter.utils.PermissionManager
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsmsfilter.presentation.viewmodel.SmsViewModel
import com.smartsmsfilter.ui.state.MessageTab
import com.smartsmsfilter.utils.DefaultSmsAppHelper
import kotlinx.coroutines.flow.first
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.content.Intent
import android.app.role.RoleManager
import android.provider.Telephony
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    // Track if we should reload messages after permissions are granted
    private var shouldReloadMessages = false
    
    // StateFlow to hold the default SMS app status
    private val _isDefaultSmsApp = MutableStateFlow(false)
    val isDefaultSmsApp = _isDefaultSmsApp.asStateFlow()

    // StateFlow for core permissions status
    private val _hasAllCorePermissions = MutableStateFlow(false)
    val hasAllCorePermissions = _hasAllCorePermissions.asStateFlow()

    // StateFlow for minimal SMS permissions to proceed to onboarding
    private val _hasSmsCorePermissions = MutableStateFlow(false)
    val hasSmsCorePermissions = _hasSmsCorePermissions.asStateFlow()
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted && shouldReloadMessages) {
            shouldReloadMessages = false
        }
        updatePermissionStatus()
    }
    
    // Register launcher for default SMS app in onCreate
    private val defaultSmsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Log.d(TAG, "Default SMS launcher result received. Result code: ${result.resultCode}")
        // Add a small delay to ensure system has processed the change
        lifecycleScope.launch {
            delay(500) // Give system time to process
            checkDefaultSmsAppStatus()
            
            // Silent: no user toast here. Logging only.
            if (_isDefaultSmsApp.value) {
                Log.d(TAG, "Default SMS set successfully")
            } else {
                Log.d(TAG, "Default SMS not set by user")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called. Checking default SMS app status.")
        checkDefaultSmsAppStatus()
        updatePermissionStatus()
    }

    private fun checkDefaultSmsAppStatus() {
        val wasDefault = _isDefaultSmsApp.value
        
        // Use role manager for API 29+ (Android 10+), fallback to telephony API for older versions
        val isCurrentlyDefault = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                val roleManager = getSystemService(RoleManager::class.java)
                roleManager.isRoleHeld(RoleManager.ROLE_SMS)
            } catch (e: Exception) {
                Log.e(TAG, "Error checking SMS role", e)
                false
            }
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            android.provider.Telephony.Sms.getDefaultSmsPackage(this) == packageName
        } else {
            false
        }
        
        // Also check via telephony API for comparison
        val telephonyDefault = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            android.provider.Telephony.Sms.getDefaultSmsPackage(this)
        } else {
            null
        }

        _isDefaultSmsApp.value = isCurrentlyDefault
        Log.d(TAG, "checkDefaultSmsAppStatus: roleHeld=$isCurrentlyDefault, telephonyDefault='$telephonyDefault', ourPackage='$packageName'")

        if (isCurrentlyDefault && !wasDefault) {
            Log.d(TAG, "Smart SMS Filter became the default SMS app")
        } else if (!isCurrentlyDefault && telephonyDefault != null) {
            Log.d(TAG, "Current telephony default SMS app is: $telephonyDefault")
        }
    }

    fun requestDefaultSmsApp() {
        Log.d(TAG, "requestDefaultSmsApp() called")
        
        // Comprehensive diagnostics
        diagnoseSmsAppStatus()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            if (android.provider.Telephony.Sms.getDefaultSmsPackage(this) == packageName) {
                Log.d(TAG, "Already the default SMS app.")
                checkDefaultSmsAppStatus() // Ensure state is correct
                return
            }
        }

        try {
            // Launch role selection without extra toasts overlaying the system sheet
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val roleManager = getSystemService(RoleManager::class.java)
                
                // Check if role is held first
                val isRoleHeld = roleManager.isRoleHeld(RoleManager.ROLE_SMS)
                Log.d(TAG, "SMS role is currently held: $isRoleHeld")
                
                if (isRoleHeld) {
                    Toast.makeText(this, "App is already set as SMS role holder", Toast.LENGTH_SHORT).show()
                    return
                }
                
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS)
                Log.d(TAG, "Launching role manager intent for API ${android.os.Build.VERSION.SDK_INT}")
                Log.d(TAG, "Intent: $intent")
                
                // Try direct launch first
                try {
                    defaultSmsLauncher.launch(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch role manager intent", e)
                    // Fallback to manual settings
                    openManualSmsSettings()
                }
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                    putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
                }
                Log.d(TAG, "Launching telephony intent for API ${android.os.Build.VERSION.SDK_INT}")
                defaultSmsLauncher.launch(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not launch default SMS app intent", e)
            openManualSmsSettings()
        }
    }
    
    private fun diagnoseSmsAppStatus() {
        Log.d(TAG, "=== SMS App Diagnostics ===")
        Log.d(TAG, "Android Version: ${android.os.Build.VERSION.SDK_INT}")
        Log.d(TAG, "Our package: $packageName")
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            val currentDefault = android.provider.Telephony.Sms.getDefaultSmsPackage(this)
            Log.d(TAG, "Current default SMS app: '$currentDefault'")
            
            // Check if we have required permissions
            val permissions = arrayOf(
                android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.RECEIVE_SMS,
                android.Manifest.permission.READ_SMS,
                "android.permission.WRITE_SMS",
                android.Manifest.permission.RECEIVE_MMS
            )
            
            permissions.forEach { permission ->
                val hasPermission = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
                Log.d(TAG, "Permission $permission: $hasPermission")
            }
            
            // Check if role manager is available
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                try {
                    val roleManager = getSystemService(RoleManager::class.java)
                    val isRoleAvailable = roleManager.isRoleAvailable(RoleManager.ROLE_SMS)
                    Log.d(TAG, "SMS role available: $isRoleAvailable")
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking SMS role availability", e)
                }
            }
        }
        Log.d(TAG, "=== End SMS App Diagnostics ===")
    }
    
    private fun openManualSmsSettings() {
        try {
            val intent = Intent("android.settings.MANAGE_DEFAULT_APPS_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Toast.makeText(this, "Please find 'SMS app' and select Smart SMS Filter", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Could not open manual settings", e)
            try {
                // Fallback to general app settings
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS)
                startActivity(intent)
                Toast.makeText(this, "Go to Default Apps > SMS app > Smart SMS Filter", Toast.LENGTH_LONG).show()
            } catch (e2: Exception) {
                Log.e(TAG, "Could not open any settings", e2)
                Toast.makeText(this, "Please manually set in Settings > Apps > Default apps > SMS app", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Public method to request app permissions after explaining to the user
    fun requestAppPermissions() {
        val allPermissions = mutableListOf<String>()

        if (!PermissionManager.hasAllSmsPermissions(this)) {
            allPermissions.addAll(PermissionManager.SMS_PERMISSIONS)
            shouldReloadMessages = true
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            allPermissions.add(android.Manifest.permission.READ_CONTACTS)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                allPermissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (allPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(allPermissions.toTypedArray())
        } else {
            updatePermissionStatus()
        }
    }

    private fun updatePermissionStatus() {
        val smsPerms = PermissionManager.hasAllSmsPermissions(this)
        val contacts = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val notifications = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        _hasSmsCorePermissions.value = smsPerms
        _hasAllCorePermissions.value = smsPerms && contacts && notifications
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize permission status
        updatePermissionStatus()

        setContent {
            val prefs by preferencesManager.userPreferences.collectAsState(initial = com.smartsmsfilter.data.preferences.UserPreferences())
            val darkTheme = when (prefs.themeMode) {
                com.smartsmsfilter.data.preferences.ThemeMode.DARK -> true
                com.smartsmsfilter.data.preferences.ThemeMode.LIGHT -> false
                com.smartsmsfilter.data.preferences.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            SmartSmsFilterTheme(darkTheme = darkTheme) {
                SmartSmsFilterApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartSmsFilterApp() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf(0) }
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val smsViewModel: SmsViewModel = hiltViewModel() // Shared SmsViewModel instance
    val context = LocalContext.current
    val mainActivity = context as? MainActivity
    val hapticFeedback = LocalHapticFeedback.current

    // Check if onboarding is completed
    val userPreferences by onboardingViewModel.userPreferences.collectAsState()
    val onboardingUiState by onboardingViewModel.uiState.collectAsState()

    // Track current destination to hide FAB appropriately
    val currentDestination by navController.currentBackStackEntryAsState()

    // Collect the default SMS app status from MainActivity's StateFlow
    val isDefaultSmsApp by mainActivity?.isDefaultSmsApp?.collectAsState() ?: mutableStateOf(false)
    val hasSmsCorePermissions by mainActivity?.hasSmsCorePermissions?.collectAsState() ?: mutableStateOf(false)

    // Determine start destination based on onboarding status
    val preferences = userPreferences
    val startDestination = when {
        !preferences.isOnboardingCompleted -> NavRoutes.Welcome.route // Not completed onboarding
        else -> NavRoutes.Inbox.route // Completed onboarding, go to main app
    }
    
    Scaffold(
        topBar = {
            // Only show the SMS default banner post-onboarding
            val currentRoute = currentDestination?.destination?.route
            val onOnboardingScreen = currentRoute == NavRoutes.Onboarding.route
            val onWelcomeScreen = currentRoute == NavRoutes.Welcome.route
            if (!isDefaultSmsApp && preferences.isOnboardingCompleted && !onOnboardingScreen && !onWelcomeScreen) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    var dismissed by remember { mutableStateOf(false) }
                    if (!dismissed) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Make Smart SMS Filter your default SMS app",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { mainActivity?.requestDefaultSmsApp() }) {
                                    Text("Set default", color = MaterialTheme.colorScheme.primary)
                                }
                                TextButton(onClick = { dismissed = true }) {
                                    Text("Dismiss", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            // Only show FAB on main screens, not on welcome/onboarding/settings/compose/thread screens
            val currentRoute = currentDestination?.destination?.route
            val onThreadScreen = currentRoute?.startsWith("thread/") == true
            val onComposeScreen = currentRoute == NavRoutes.ComposeMessage.route
            val onOnboardingScreen = currentRoute == NavRoutes.Onboarding.route
            val onWelcomeScreen = currentRoute == NavRoutes.Welcome.route
            val onSettingsScreen = currentRoute == NavRoutes.Settings.route
            if (!onThreadScreen && !onComposeScreen && !onOnboardingScreen && !onWelcomeScreen && !onSettingsScreen) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(NavRoutes.ComposeMessage.route)
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New message")
                }
            }
        },
        bottomBar = {
            // Hide bottom navigation during welcome, onboarding, and settings
            val currentRoute = currentDestination?.destination?.route
            if (currentRoute != NavRoutes.Welcome.route && currentRoute != NavRoutes.Onboarding.route && currentRoute != NavRoutes.Settings.route) {
                NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Inbox, contentDescription = null) },
                    label = { Text("Inbox") },
                    selected = selectedTab == 0,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedTab = 0
                        smsViewModel.selectionState.setCurrentTab(MessageTab.INBOX)
                        navController.navigate(NavRoutes.Inbox.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Block, contentDescription = null) },
                    label = { Text("Spam") },
                    selected = selectedTab == 1,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedTab = 1
                        smsViewModel.selectionState.setCurrentTab(MessageTab.SPAM)
                        navController.navigate(NavRoutes.Spam.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Outlined.QuestionMark, contentDescription = null) },
                    label = { Text("Review") },
                    selected = selectedTab == 2,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedTab = 2
                        smsViewModel.selectionState.setCurrentTab(MessageTab.REVIEW)
                        navController.navigate(NavRoutes.Review.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(NavRoutes.Settings.route) {
                SettingsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(NavRoutes.Welcome.route) {
                // Track if we initiated a permission request from Welcome
                var requestedPermFromWelcome by remember { mutableStateOf(false) }

                // Navigate automatically after permissions are granted
                LaunchedEffect(hasSmsCorePermissions, requestedPermFromWelcome) {
                    if (requestedPermFromWelcome && hasSmsCorePermissions) {
                        navController.navigate(NavRoutes.Onboarding.route) {
                            popUpTo(NavRoutes.Welcome.route) { inclusive = true }
                        }
                    }
                }

                WelcomeScreen(
                    onGetStarted = {
                        if (!hasSmsCorePermissions) {
                            requestedPermFromWelcome = true
                            mainActivity?.requestAppPermissions()
                        } else {
                            navController.navigate(NavRoutes.Onboarding.route) {
                                popUpTo(NavRoutes.Welcome.route) { inclusive = true }
                            }
                        }
                    },
                    onLearnMore = {
                        // Show an info dialog about app features, privacy, and how it works
                        // For now, navigate to settings where detailed info is available
                        navController.navigate(NavRoutes.Settings.route)
                    },
                    onRequestPermissions = { /* Get Started handles permission prompts */ },
                    onRequestDefaultSms = { /* Defer to onboarding dialog to avoid overlap on welcome */ }
                )
            }
            
            composable(NavRoutes.Onboarding.route) {
                PremiumOnboardingScreen(
                    preferences = userPreferences,
                    onComplete = { preferences ->
                        onboardingViewModel.saveUserPreferences(preferences)
                    },
                    onBack = {
                        navController.navigate(NavRoutes.Welcome.route) {
                            popUpTo(NavRoutes.Onboarding.route) {
                                inclusive = true
                            }
                        }
                    }
                )
                
                // Navigate to main app when onboarding is completed
                LaunchedEffect(onboardingUiState.isCompleted) {
                    if (onboardingUiState.isCompleted) {
                        navController.navigate(NavRoutes.Inbox.route) {
                            popUpTo(NavRoutes.Onboarding.route) {
                                inclusive = true
                            }
                        }
                    }
                }
            }
            composable(NavRoutes.Inbox.route) {
                InboxScreen(
                    onNavigateToThread = { address ->
                        navController.navigate(NavRoutes.Thread.createRoute(address))
                    },
                    viewModel = smsViewModel,
                    onSettingsClick = { navController.navigate(NavRoutes.Settings.route) }
                )
            }
            composable(NavRoutes.Spam.route) {
                SpamScreen(
                    onNavigateToThread = { address ->
                        navController.navigate(NavRoutes.Thread.createRoute(address))
                    },
                    viewModel = smsViewModel,
                    onSettingsClick = { navController.navigate(NavRoutes.Settings.route) }
                )
            }
            composable(NavRoutes.Review.route) {
                ReviewScreen(
                    onNavigateToThread = { address ->
                        navController.navigate(NavRoutes.Thread.createRoute(address))
                    },
                    viewModel = smsViewModel,
                    onSettingsClick = { navController.navigate(NavRoutes.Settings.route) }
                )
            }
            composable(
                route = NavRoutes.Thread.route,
                arguments = listOf(navArgument("address") { type = NavType.StringType })
            ) { backStackEntry ->
                val address = backStackEntry.arguments?.getString("address") ?: ""
                ThreadScreen(
                    address = address,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.ComposeMessage.route) {
                ComposeMessageScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
