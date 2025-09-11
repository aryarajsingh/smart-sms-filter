package com.smartsmsfilter

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Reviews
import androidx.compose.material.icons.filled.Add
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
import kotlinx.coroutines.flow.first
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            // Handle permission denial - show explanation
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request all necessary permissions
        val allPermissions = mutableListOf<String>()
        
        if (!PermissionManager.hasAllSmsPermissions(this)) {
            allPermissions.addAll(PermissionManager.SMS_PERMISSIONS)
        }
        
        // Add contact permissions
        allPermissions.add(android.Manifest.permission.READ_CONTACTS)
        
        // Add notification permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            allPermissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (allPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(allPermissions.toTypedArray())
        }
        
        setContent {
            SmartSmsFilterTheme {
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
    
    // Check if onboarding is completed
    val userPreferences by onboardingViewModel.userPreferences.collectAsState()
    val onboardingUiState by onboardingViewModel.uiState.collectAsState()
    
    // Track current destination to hide FAB appropriately
    val currentDestination by navController.currentBackStackEntryAsState()
    
    // Determine start destination based on onboarding status
    val preferences = userPreferences
    val startDestination = when {
        preferences == null -> NavRoutes.Welcome.route // First time, no preferences yet
        !preferences.isOnboardingCompleted -> NavRoutes.Welcome.route // Started but not completed
        else -> NavRoutes.Inbox.route // Completed onboarding, go to main app
    }
    
    Scaffold(
        floatingActionButton = {
            // Only show FAB on main screens, not on welcome/onboarding/compose/thread screens
            val currentRoute = currentDestination?.destination?.route
            val onThreadScreen = currentRoute?.startsWith("thread/") == true
            val onComposeScreen = currentRoute == NavRoutes.ComposeMessage.route
            val onOnboardingScreen = currentRoute == NavRoutes.Onboarding.route
            val onWelcomeScreen = currentRoute == NavRoutes.Welcome.route
            if (!onThreadScreen && !onComposeScreen && !onOnboardingScreen && !onWelcomeScreen) {
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
            // Hide bottom navigation during welcome and onboarding
            val currentRoute = currentDestination?.destination?.route
            if (currentRoute != NavRoutes.Welcome.route && currentRoute != NavRoutes.Onboarding.route) {
                NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Inbox, contentDescription = null) },
                    label = { Text("Inbox") },
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        navController.navigate(NavRoutes.Inbox.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                    label = { Text("Filtered") },
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        navController.navigate(NavRoutes.Filtered.route) {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Reviews, contentDescription = null) },
                    label = { Text("Review") },
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        navController.navigate(NavRoutes.NeedsReview.route) {
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
            composable(NavRoutes.Welcome.route) {
                WelcomeScreen(
                    onGetStarted = {
                        navController.navigate(NavRoutes.Onboarding.route) {
                            popUpTo(NavRoutes.Welcome.route) {
                                inclusive = true
                            }
                        }
                    },
                    onLearnMore = {
                        // TODO: Open website or show info dialog about privacy, features, how it works
                        // Future: Launch browser intent to company website/privacy policy
                    }
                )
            }
            
            composable(NavRoutes.Onboarding.route) {
                OnboardingScreen(
                    preferences = userPreferences,
                    onSave = { preferences ->
                        onboardingViewModel.saveUserPreferences(preferences)
                    },
                    onSkip = {
                        onboardingViewModel.skipOnboarding()
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
                    }
                )
            }
            composable(NavRoutes.Filtered.route) {
                FilteredScreen(
                    onNavigateToThread = { address ->
                        navController.navigate(NavRoutes.Thread.createRoute(address))
                    }
                )
            }
            composable(NavRoutes.NeedsReview.route) {
                NeedsReviewScreen(
                    onNavigateToThread = { address ->
                        navController.navigate(NavRoutes.Thread.createRoute(address))
                    }
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
