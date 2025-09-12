package com.smartsmsfilter.presentation.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.smartsmsfilter.data.preferences.*
import com.smartsmsfilter.ui.theme.*
import com.smartsmsfilter.ui.utils.*
import com.smartsmsfilter.utils.DefaultSmsAppDialog
import com.smartsmsfilter.utils.DefaultSmsAppHelper
import com.smartsmsfilter.MainActivity

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PremiumOnboardingScreen(
    preferences: UserPreferences,
    onComplete: (UserPreferences) -> Unit,
    onBack: () -> Unit = {}
) {
    val haptic = rememberHapticFeedback()
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // State for showing default SMS app dialog
    var showDefaultSmsDialog by remember { mutableStateOf(false) }
    
    // Mutable preferences
    var filteringMode by remember { mutableStateOf(preferences.filteringMode) }
    var selectedTypes by remember { mutableStateOf(preferences.importantMessageTypes) }
    var spamTolerance by remember { mutableStateOf(preferences.spamTolerance) }
    var enableSmartNotifications by remember { mutableStateOf(preferences.enableSmartNotifications) }
    var enableLearningFromFeedback by remember { mutableStateOf(preferences.enableLearningFromFeedback) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Animated gradient background
        AnimatedGradientBackground(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.03f),
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.02f)
            ),
            modifier = Modifier.fillMaxSize()
        )
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Removed top header for cleaner onboarding
            
            // Main content pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> FilteringStrengthPage(
                        filteringMode = filteringMode,
                        onModeChange = { 
                            haptic(HapticManager.FeedbackType.IMPACT_LIGHT)
                            filteringMode = it 
                        }
                    )
                    1 -> ImportantMessagesPage(
                        selectedTypes = selectedTypes,
                        onTypesChange = { 
                            haptic(HapticManager.FeedbackType.SELECTION)
                            selectedTypes = it 
                        }
                    )
                    2 -> SpamSensitivityPage(
                        spamTolerance = spamTolerance,
                        onToleranceChange = { 
                            haptic(HapticManager.FeedbackType.TICK)
                            spamTolerance = it 
                        }
                    )
                    3 -> SmartFeaturesPage(
                        enableSmartNotifications = enableSmartNotifications,
                        enableLearning = enableLearningFromFeedback,
                        onNotificationsChange = { 
                            haptic(HapticManager.FeedbackType.IMPACT_MEDIUM)
                            enableSmartNotifications = it 
                        },
                        onLearningChange = { 
                            haptic(HapticManager.FeedbackType.IMPACT_MEDIUM)
                            enableLearningFromFeedback = it 
                        }
                    )
                }
            }
            
            // Bottom navigation
            OnboardingBottomNav(
                currentPage = pagerState.currentPage,
                totalPages = pagerState.pageCount,
                onNext = {
                    scope.launch {
                        if (pagerState.currentPage < pagerState.pageCount - 1) {
                            haptic(HapticManager.FeedbackType.IMPACT_LIGHT)
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        } else {
                            haptic(HapticManager.FeedbackType.SUCCESS)
                            // Save preferences first
// Show default SMS app dialog
                            showDefaultSmsDialog = true
                            // Will call onComplete after dialog
                        }
                    }
                },
                onSkip = {
                    haptic(HapticManager.FeedbackType.SELECTION)
                    onComplete(preferences.copy(isOnboardingCompleted = true))
                }
            )
        }
    }
    
    // Default SMS App Dialog
    if (showDefaultSmsDialog) {
        DefaultSmsAppDialog(
            onDismiss = {
                showDefaultSmsDialog = false
                // Complete onboarding even if user dismisses
                onComplete(
                    preferences.copy(
                        filteringMode = filteringMode,
                        importantMessageTypes = selectedTypes,
                        spamTolerance = spamTolerance,
                        enableSmartNotifications = enableSmartNotifications,
                        enableLearningFromFeedback = enableLearningFromFeedback,
                        isOnboardingCompleted = true
                    )
                )
            },
            onConfirm = {
                showDefaultSmsDialog = false
                // Use MainActivity's method to request default SMS app
                val mainActivity = context as? MainActivity
                if (mainActivity != null) {
                    mainActivity.requestDefaultSmsApp()
                } else {
                    // Fallback: try to launch the default SMS app intent directly
                    try {
                        val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                            val roleManager = context.getSystemService(android.app.role.RoleManager::class.java)
                            roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_SMS)
                        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                            android.content.Intent(android.provider.Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                                putExtra(android.provider.Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                            }
                        } else null
                        
                        if (intent != null) {
                            context.startActivity(intent)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("PremiumOnboarding", "Failed to launch default SMS app intent", e)
                    }
                }
                // Complete onboarding immediately
                onComplete(
                    preferences.copy(
                        filteringMode = filteringMode,
                        importantMessageTypes = selectedTypes,
                        spamTolerance = spamTolerance,
                        enableSmartNotifications = enableSmartNotifications,
                        enableLearningFromFeedback = enableLearningFromFeedback,
                        isOnboardingCompleted = true
                    )
                )
            }
        )
    }
}

@Composable
private fun OnboardingHeader(
    currentPage: Int,
    totalPages: Int,
    onBack: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Progress indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(totalPages) { index ->
                        ProgressDot(
                            isActive = index == currentPage,
                            isCompleted = index < currentPage
                        )
                    }
                }
                
                // Placeholder for balance
                Box(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
private fun ProgressDot(
    isActive: Boolean,
    isCompleted: Boolean
) {
    val color = when {
        isActive -> MaterialTheme.colorScheme.primary
        isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    }
    
    val size by animateDpAsState(
        targetValue = if (isActive) 8.dp else 6.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "dot_size"
    )
    
    Box(
        modifier = Modifier
            .size(size)
            .background(color, CircleShape)
    )
}

@Composable
private fun FilteringStrengthPage(
    filteringMode: FilteringMode,
    onModeChange: (FilteringMode) -> Unit
) {
    var selectedMode by remember { mutableStateOf(filteringMode) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Icon animation
        val iconScale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "icon_scale"
        )
        
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 32.dp)
        ) {
            Icon(
                Icons.Filled.FilterList,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .scale(iconScale),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        Text(
            text = "How should we filter?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Choose your filtering strength. This directly affects how messages are categorized.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Filter mode options with visual feedback
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilterModeCard(
                mode = FilteringMode.STRICT,
                title = "Strict",
                description = "Only essential messages in inbox",
                icon = Icons.Filled.Shield,
                isSelected = selectedMode == FilteringMode.STRICT,
                impactPreview = "🔒 Banks, OTPs only",
                onClick = {
                    selectedMode = FilteringMode.STRICT
                    onModeChange(FilteringMode.STRICT)
                }
            )
            
            FilterModeCard(
                mode = FilteringMode.MODERATE,
                title = "Moderate",
                description = "Important messages + trusted services",
                icon = Icons.Filled.Balance,
                isSelected = selectedMode == FilteringMode.MODERATE,
                impactPreview = "⚖️ Smart filtering",
                onClick = {
                    selectedMode = FilteringMode.MODERATE
                    onModeChange(FilteringMode.MODERATE)
                }
            )
            
            FilterModeCard(
                mode = FilteringMode.LENIENT,
                title = "Lenient",
                description = "Most messages allowed, obvious spam filtered",
                icon = Icons.Filled.Mood,
                isSelected = selectedMode == FilteringMode.LENIENT,
                impactPreview = "😊 Minimal filtering",
                onClick = {
                    selectedMode = FilteringMode.LENIENT
                    onModeChange(FilteringMode.LENIENT)
                }
            )
        }
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun FilterModeCard(
    mode: FilteringMode,
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    impactPreview: String,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(300),
        label = "border_color"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "card_scale"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isSelected) {
                    Text(
                        text = impactPreview,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            // Selection indicator
            if (isSelected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun ImportantMessagesPage(
    selectedTypes: Set<ImportantMessageType>,
    onTypesChange: (Set<ImportantMessageType>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Icon(
            Icons.Filled.Star,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.CenterHorizontally),
            tint = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "What's important to you?",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "These messages will always reach your inbox instantly.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Message type chips with real-time impact
        val messageTypes = listOf(
            ImportantMessageType.BANKING to Pair("💳 Banking", "Transaction alerts, balance updates"),
            ImportantMessageType.OTPS to Pair("🔐 OTPs", "Verification codes, 2FA"),
            ImportantMessageType.ECOMMERCE to Pair("📦 E-commerce", "Order updates, delivery notifications"),
            ImportantMessageType.PERSONAL to Pair("👤 Personal", "Messages from contacts"),
            ImportantMessageType.GOVERNMENT to Pair("🏛️ Government", "Official communications"),
            ImportantMessageType.HEALTHCARE to Pair("🏥 Healthcare", "Appointments, test results")
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            messageTypes.forEach { (type, details) ->
                MessageTypeChip(
                    type = type,
                    title = details.first,
                    description = details.second,
                    isSelected = selectedTypes.contains(type),
                    onClick = {
                        onTypesChange(
                            if (selectedTypes.contains(type))
                                selectedTypes - type
                            else
                                selectedTypes + type
                        )
                    }
                )
            }
        }

        // Extra bottom space to avoid overlap with bottom navigation area
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun MessageTypeChip(
    type: ImportantMessageType,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.secondaryContainer
        else
            MaterialTheme.colorScheme.surface,
        animationSpec = tween(300),
        label = "chip_bg"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.secondary
                )
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SpamSensitivityPage(
    spamTolerance: SpamTolerance,
    onToleranceChange: (SpamTolerance) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        // Use the new improved SpamSensitivitySelector
        com.smartsmsfilter.presentation.ui.components.SpamSensitivitySelector(
            selectedTolerance = spamTolerance,
            onToleranceChange = onToleranceChange
        )
    }
}

// SpamSensitivitySlider removed - using SpamSensitivitySelector component instead

@Composable
private fun SmartFeaturesPage(
    enableSmartNotifications: Boolean,
    enableLearning: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    onLearningChange: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.CenterHorizontally),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Smart features",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Enable AI-powered features for the best experience.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Smart features toggles
        SmartFeatureToggle(
            title = "Smart Notifications",
            description = "Prioritize important alerts, silence spam automatically",
            icon = Icons.Filled.NotificationsActive,
            isEnabled = enableSmartNotifications,
            onToggle = onNotificationsChange
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SmartFeatureToggle(
            title = "Adaptive Learning",
            description = "Improve filtering accuracy based on your corrections",
            icon = Icons.Filled.Psychology,
            isEnabled = enableLearning,
            onToggle = onLearningChange
        )
    }
}

@Composable
private fun SmartFeatureToggle(
    title: String,
    description: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        ),
        border = if (isEnabled) BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isEnabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isEnabled)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
private fun OnboardingBottomNav(
    currentPage: Int,
    totalPages: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Skip button
            TextButton(
                onClick = onSkip,
                modifier = Modifier.alpha(if (currentPage < totalPages - 1) 1f else 0f)
            ) {
                Text("Skip")
            }
            
            // Next/Complete button
            Button(
                onClick = onNext,
                modifier = Modifier
                    .bouncyPress(isPressed),
                interactionSource = interactionSource,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentPage == totalPages - 1)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentPage == totalPages - 1) "Get Started" else "Continue",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (currentPage == totalPages - 1)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (currentPage < totalPages - 1) {
                        Spacer(modifier = Modifier.width(8.dp))
Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
