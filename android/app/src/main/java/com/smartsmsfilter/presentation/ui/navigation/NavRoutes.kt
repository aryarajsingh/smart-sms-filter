package com.smartsmsfilter.presentation.ui.navigation

sealed class NavRoutes(val route: String) {
    object Welcome : NavRoutes("welcome")
    object Onboarding : NavRoutes("onboarding")
    object Inbox : NavRoutes("inbox")
    object Spam : NavRoutes("spam")  
    object Review : NavRoutes("review")
    object Settings : NavRoutes("settings")
    object Permissions : NavRoutes("permissions")
    object ComposeMessage : NavRoutes("compose_message")
    object Thread : NavRoutes("thread/{address}") {
        fun createRoute(address: String) = "thread/$address"
    }
}

// Bottom navigation items
data class BottomNavItem(
    val route: String,
    val icon: androidx.compose.material.icons.Icons,
    val label: String,
    val badgeCount: Int = 0
)
