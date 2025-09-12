package com.smartsmsfilter.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartsmsfilter.data.preferences.FilteringMode
import com.smartsmsfilter.data.preferences.ImportantMessageType
import com.smartsmsfilter.data.preferences.SpamTolerance
import com.smartsmsfilter.data.preferences.ThemeMode
import com.smartsmsfilter.presentation.viewmodel.OnboardingViewModel
import com.smartsmsfilter.ui.theme.PremiumCornerRadius
import com.smartsmsfilter.ui.theme.PremiumSpacing
import com.smartsmsfilter.BuildConfig
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import com.smartsmsfilter.MainActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val preferences by viewModel.userPreferences.collectAsState()
    val scrollState = rememberScrollState()

    val context = LocalContext.current
    val mainActivity = context as? com.smartsmsfilter.MainActivity
    val isDefaultSmsApp by (mainActivity?.isDefaultSmsApp ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(PremiumSpacing.Large),
            verticalArrangement = Arrangement.spacedBy(PremiumSpacing.Large)
        ) {
            // Theme
            SettingsSection(title = "Appearance") {
                ThemeModeRow(
                    selected = preferences.themeMode,
                    onChange = { mode ->
                        viewModel.saveUserPreferences(preferences.copy(themeMode = mode))
                    }
                )
            }

            // Filtering Mode
            SettingsSection(title = "Filtering strength") {
                FilteringModeRow(
                    selected = preferences.filteringMode,
                    onChange = { mode -> viewModel.saveUserPreferences(preferences.copy(filteringMode = mode)) }
                )
            }

            // Important message types
            SettingsSection(title = "Important messages") {
                ImportantTypesGrid(
                    selected = preferences.importantMessageTypes,
                    onToggle = { type, checked ->
                        val newSet = if (checked) preferences.importantMessageTypes + type else preferences.importantMessageTypes - type
                        viewModel.saveUserPreferences(preferences.copy(importantMessageTypes = newSet))
                    }
                )
            }

            // Spam tolerance
            SettingsSection(title = "Spam sensitivity") {
                SpamToleranceRow(
                    selected = preferences.spamTolerance,
                    onChange = { tol -> viewModel.saveUserPreferences(preferences.copy(spamTolerance = tol)) }
                )
            }

            // Smart features
            SettingsSection(title = "Smart features") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Smart notifications", style = MaterialTheme.typography.titleMedium)
                        Text("Prioritize important alerts and reduce noise", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = preferences.enableSmartNotifications,
                        onCheckedChange = { checked ->
                            viewModel.saveUserPreferences(preferences.copy(enableSmartNotifications = checked))
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Learn from corrections", style = MaterialTheme.typography.titleMedium)
                        Text("Improve filtering using your feedback", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = preferences.enableLearningFromFeedback,
                        onCheckedChange = { checked ->
                            viewModel.saveUserPreferences(preferences.copy(enableLearningFromFeedback = checked))
                        }
                    )
                }
            }

            // Diagnostics
            SettingsSection(title = "Diagnostics") {
                val statusText = if (isDefaultSmsApp) "Default SMS app: Yes" else "Default SMS app: No"
                Text(statusText, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { if (!isDefaultSmsApp) (context as? MainActivity)?.requestDefaultSmsApp() },
                        enabled = !isDefaultSmsApp
                    ) {
                        Text(if (isDefaultSmsApp) "Already default" else "Set default SMS app")
                    }
                    Spacer(Modifier.width(PremiumSpacing.Medium))
                    OutlinedButton(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }) { Text("App settings") }
                }
            }

            // About
            SettingsSection(title = "About") {
                Text("Version: ${BuildConfig.VERSION_NAME}")
                Text("Privacy: All processing happens on-device. Messages never leave your phone.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Vision: A calmer, smarter SMS inbox that respects your time and privacy.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(PremiumSpacing.Medium)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(PremiumSpacing.Medium), content = content)
        }
    }
}

@Composable
private fun ThemeModeRow(selected: ThemeMode, onChange: (ThemeMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeMode.values().forEach { mode ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected == mode, onClick = { onChange(mode) })
                Spacer(Modifier.width(8.dp))
                Text(mode.displayName)
            }
        }
    }
}

@Composable
private fun FilteringModeRow(selected: FilteringMode, onChange: (FilteringMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilteringMode.values().forEach { mode ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected == mode, onClick = { onChange(mode) })
                Spacer(Modifier.width(8.dp))
                Text(mode.displayName)
            }
        }
    }
}

@Composable
private fun ImportantTypesGrid(selected: Set<ImportantMessageType>, onToggle: (ImportantMessageType, Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ImportantMessageType.values().forEach { type ->
            val checked = selected.contains(type)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = checked, onCheckedChange = { onToggle(type, it) })
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(type.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(type.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SpamToleranceRow(selected: SpamTolerance, onChange: (SpamTolerance) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SpamTolerance.values().forEach { tol ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected == tol, onClick = { onChange(tol) })
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(tol.displayName)
                    Text(tol.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

