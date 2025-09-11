package com.smartsmsfilter.presentation.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.smartsmsfilter.data.preferences.*
import com.smartsmsfilter.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    preferences: UserPreferences?,
    onSave: (UserPreferences) -> Unit,
    onSkip: () -> Unit,
    onBack: () -> Unit = {}
) {
    var filteringMode by remember { mutableStateOf(preferences?.filteringMode ?: FilteringMode.MODERATE) }
    var selectedTypes by remember { mutableStateOf(preferences?.importantMessageTypes ?: emptySet()) }
    var spamTolerance by remember { mutableStateOf(preferences?.spamTolerance ?: SpamTolerance.MODERATE) }
    var enableSmartNotifications by remember { mutableStateOf(preferences?.enableSmartNotifications ?: true) }
    var enableLearningFromFeedback by remember { mutableStateOf(preferences?.enableLearningFromFeedback ?: true) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Setup your filtering", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text(
                            "Step 1 of 1 · Personalize preferences",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(PremiumSpacing.Medium)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(PremiumSpacing.Large)
        ) {
            // Introduction text
            Column(verticalArrangement = Arrangement.spacedBy(PremiumSpacing.Small)) {
                Text(
                    "Customize your experience",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Tell us your preferences so we can filter messages exactly how you like. You can change these anytime.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Filtering mode section
            Column(verticalArrangement = Arrangement.spacedBy(PremiumSpacing.Small)) {
                Text(
                    "Filtering strength",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "How aggressively should we filter promotional messages?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FilterModeSelector(filteringMode) { filteringMode = it }
            }

            // Important message types section
            Column(verticalArrangement = Arrangement.spacedBy(PremiumSpacing.Small)) {
                Text(
                    "What's important to you?",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "Select message types you want to see in your inbox.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ImportantTypesSelector(selectedTypes) { selectedTypes = it }
            }

            // Spam tolerance section
            Column(verticalArrangement = Arrangement.spacedBy(PremiumSpacing.Small)) {
                Text(
                    "Spam sensitivity",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "How sensitive should spam detection be?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SpamToleranceSelector(spamTolerance) { spamTolerance = it }
            }

            // Additional preferences section
            Column(verticalArrangement = Arrangement.spacedBy(PremiumSpacing.Small)) {
                Text(
                    "Additional preferences",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                // Smart notifications toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PremiumSpacing.Medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Smart notifications",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Prioritize important alerts and silence spam",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableSmartNotifications,
                            onCheckedChange = { enableSmartNotifications = it }
                        )
                    }
                }
                
                // Learning toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PremiumSpacing.Medium),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Learn from corrections",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Improve filtering based on your feedback",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableLearningFromFeedback,
                            onCheckedChange = { enableLearningFromFeedback = it }
                        )
                    }
                }
            }

            Spacer(Modifier.height(PremiumSpacing.Medium))
            
            // Action buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(PremiumSpacing.Small)
            ) {
                Button(
                    onClick = {
                        onSave(
                            UserPreferences(
                                isOnboardingCompleted = true,
                                filteringMode = filteringMode,
                                importantMessageTypes = selectedTypes,
                                spamTolerance = spamTolerance,
                                enableSmartNotifications = enableSmartNotifications,
                                enableLearningFromFeedback = enableLearningFromFeedback
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(PremiumCornerRadius.Large)
                ) {
                    Text(
                        "Complete setup",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
                
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Skip for now",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterModeSelector(selected: FilteringMode, onSelect: (FilteringMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilteringMode.values().forEach { mode ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (mode == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                )
            ) {
                ListItem(
                    headlineContent = { Text(mode.displayName) },
                    supportingContent = { Text(mode.description) },
                    trailingContent = { RadioButton(selected = selected == mode, onClick = { onSelect(mode) }) },
                    modifier = Modifier.toggleable(value = selected == mode, onValueChange = { onSelect(mode) })
                )
            }
        }
    }
}

@Composable
private fun ImportantTypesSelector(selected: Set<ImportantMessageType>, onChange: (Set<ImportantMessageType>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ImportantMessageType.values().forEach { type ->
            val isChecked = selected.contains(type)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isChecked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                )
            ) {
                ListItem(
                    headlineContent = { Text(type.displayName) },
                    supportingContent = { Text(type.description) },
                    trailingContent = { Checkbox(checked = isChecked, onCheckedChange = { checked ->
                        onChange(if (checked) selected + type else selected - type)
                    }) },
                    modifier = Modifier.toggleable(value = isChecked, onValueChange = { checked ->
                        onChange(if (checked) selected + type else selected - type)
                    })
                )
            }
        }
    }
}

@Composable
private fun SpamToleranceSelector(selected: SpamTolerance, onSelect: (SpamTolerance) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SpamTolerance.values().forEach { tol ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (tol == selected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surface
                )
            ) {
                ListItem(
                    headlineContent = { Text(tol.displayName) },
                    supportingContent = { Text(tol.description) },
                    trailingContent = { RadioButton(selected = selected == tol, onClick = { onSelect(tol) }) },
                    modifier = Modifier.toggleable(value = selected == tol, onValueChange = { onSelect(tol) })
                )
            }
        }
    }
}

