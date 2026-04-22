package dev.capsule.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.capsule.data.prefs.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFontSizeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item {
                SettingsSection(title = "Appearance")
            }

            item {
                SettingsItem(
                    title = "Theme",
                    subtitle = uiState.themeMode.name,
                    onClick = { showThemeDialog = true }
                )
            }

            item {
                SettingsItem(
                    title = "Font Size",
                    subtitle = "${uiState.fontSize}sp",
                    onClick = { showFontSizeDialog = true }
                )
            }

            item {
                SettingsSwitchItem(
                    title = "Keep Screen On",
                    subtitle = "Prevent screen from turning off in terminal",
                    checked = uiState.keepScreenOn,
                    onCheckedChange = { viewModel.updateKeepScreenOn(it) }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSection(title = "Storage")
            }

            item {
                SettingsItem(
                    title = "Images Size",
                    subtitle = formatSize(uiState.imagesSizeBytes),
                    onClick = {}
                )
            }

            item {
                SettingsItem(
                    title = "Overlay Size",
                    subtitle = formatSize(uiState.overlaySizeBytes),
                    onClick = {}
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SettingsSection(title = "About")
            }

            item {
                SettingsItem(
                    title = "Version",
                    subtitle = "1.0.0",
                    onClick = {}
                )
            }
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Select Theme") },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.themeMode == mode,
                                onClick = {
                                    viewModel.updateThemeMode(mode)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(mode.name)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showFontSizeDialog) {
        var fontSizeValue by remember { mutableStateOf(uiState.fontSize.toFloat()) }
        AlertDialog(
            onDismissRequest = { showFontSizeDialog = false },
            title = { Text("Font Size") },
            text = {
                Column {
                    Slider(
                        value = fontSizeValue,
                        onValueChange = { fontSizeValue = it },
                        valueRange = 8f..24f,
                        steps = 15
                    )
                    Text("${fontSizeValue.toInt()}sp")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateFontSize(fontSizeValue.toInt())
                    showFontSizeDialog = false
                }) {
                    Text("Apply")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

private fun formatSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}