package com.smartsmsfilter.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartsmsfilter.ui.theme.IOSSpacing
import com.smartsmsfilter.ui.theme.IOSTypography

data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumBottomTabNavigation(
    selectedRoute: String,
    onTabSelected: (String) -> Unit,
    inboxCount: Int = 0,
    spamCount: Int = 0,
    reviewCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val tabsWithCounts = listOf(
        BottomNavItem("inbox", "Inbox", Icons.Filled.Inbox, Icons.Outlined.Inbox, inboxCount),
        BottomNavItem("spam", "Spam", Icons.Filled.Block, Icons.Outlined.Block, spamCount),
        BottomNavItem("review", "Review", Icons.Filled.QuestionMark, Icons.Outlined.QuestionMark, reviewCount),
        BottomNavItem("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = IOSSpacing.medium,
                    vertical = IOSSpacing.small
                ),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabsWithCounts.forEach { tab ->
                PremiumTabItem(
                    tab = tab,
                    isSelected = selectedRoute == tab.route,
                    onClick = { onTabSelected(tab.route) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PremiumTabItem(
    tab: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // Smooth animations
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tab_scale"
    )
    
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) 
            MaterialTheme.colorScheme.primary 
        else 
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "icon_color"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isSelected) 
            MaterialTheme.colorScheme.primary 
        else 
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "text_color"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(vertical = IOSSpacing.small),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Icon with badge
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = tab.title,
                    tint = iconColor,
                    modifier = Modifier
                        .size(24.dp)
                        .scale(scale)
                )
                
                // Badge for counts
                if (tab.badgeCount > 0) {
                    @OptIn(ExperimentalMaterial3Api::class)
                    Badge(
                        modifier = Modifier
                            .offset(x = 12.dp, y = (-12).dp)
                            .scale(0.8f),
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ) {
                        Text(
                            text = if (tab.badgeCount > 99) "99+" else tab.badgeCount.toString(),
                            style = IOSTypography.caption.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
            
            // Label
            Text(
                text = tab.title,
                style = IOSTypography.caption.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = textColor
            )
        }
    }
}
