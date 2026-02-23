package com.chatxstudio.bountu.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import com.chatxstudio.bountu.git.GitPackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeConfigurationScreen(
    themeManager: ThemeManager,
    onThemeChanged: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val gitManager = remember { GitPackageManager(context) }

    var themeConfig by remember { mutableStateOf(ThemeConfig()) }
    val persisted by themeManager.getThemeConfigFlow().collectAsState(initial = ThemeConfig())
    LaunchedEffect(persisted) { themeConfig = persisted }

    var loadStatus by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Theme Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // App Theme Selection
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "App Theme",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(modifier = Modifier.selectableGroup()) {
                    AppTheme.values().forEach { theme ->
                        val isSelected = themeConfig.appTheme == theme
                        ListItem(
                            headlineContent = { Text(theme.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton
                                ) {
                                    themeConfig = themeConfig.copy(appTheme = theme)
                                },
                            trailingContent = {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null
                                )
                            }
                        )
                    }
                }
            }
        }

        // Color Scheme Selection
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Color Scheme",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(modifier = Modifier.selectableGroup()) {
                    ColorScheme.values().forEach { colorScheme ->
                        val isSelected = themeConfig.colorScheme == colorScheme
                        ListItem(
                            headlineContent = { Text(colorScheme.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            supportingContent = {
                                when (colorScheme) {
                                    ColorScheme.DEFAULT -> Text("Default Material 3 colors")
                                    ColorScheme.UBUNTU -> Text("Ubuntu-inspired colors")
                                    ColorScheme.MONOKAI -> Text("Monokai dark theme")
                                    ColorScheme.SOLARIZED -> Text("Solarized color palette")
                                    ColorScheme.TERMINAL -> Text("Classic terminal green/black")
                                    ColorScheme.CUSTOM -> Text("From community JSON (Git)")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = isSelected,
                                    role = Role.RadioButton
                                ) {
                                    themeConfig = themeConfig.copy(colorScheme = colorScheme)
                                },
                            trailingContent = {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null
                                )
                            }
                        )
                    }
                }
            }
        }

        // Custom theme paste box + Git loader
        if (themeConfig.colorScheme == ColorScheme.CUSTOM) {
            var jsonText by remember { mutableStateOf(themeConfig.customThemeJson ?: "") }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Custom Theme JSON", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = jsonText,
                        onValueChange = { jsonText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        label = { Text("Paste theme JSON (Windows Terminal style)") }
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            coroutineScope.launch {
                                themeManager.saveThemeConfig(
                                    themeConfig.copy(customThemeJson = jsonText)
                                )
                                onThemeChanged()
                            }
                        }) {
                            Text("Apply Custom Theme")
                        }
                        OutlinedButton(onClick = {
                            coroutineScope.launch {
                                loadStatus = "Searching themes in Git repo..."
                                // Ensure repo is initialized and get local path
                                val info = gitManager.getRepositoryInfo()
                                if (info is com.chatxstudio.bountu.git.GitResult.Success) {
                                    val localPath = info.data.localPath
                                    val themesDir = File(localPath, "config/themes")
                                    val candidates = withContext(Dispatchers.IO) {
                                        themesDir.listFiles { f -> f.isFile && f.name.endsWith(".json", ignoreCase = true) }
                                            ?.sortedBy { it.name.lowercase() }
                                            ?: emptyList()
                                    }
                                    if (candidates.isNotEmpty()) {
                                        // Prefer a file that contains "bountu"
                                        val preferred = candidates.firstOrNull { it.name.contains("bountu", ignoreCase = true) }
                                            ?: candidates.first()
                                        val content = withContext(Dispatchers.IO) { preferred.readText() }
                                        jsonText = content
                                        themeManager.saveThemeConfig(themeConfig.copy(customThemeJson = content))
                                        onThemeChanged()
                                        loadStatus = "Loaded: ${preferred.name}"
                                    } else {
                                        // Fallback to bundled asset
                                        try {
                                            val assetManager = context.assets
                                            val path = "config/themes/bountu_empty_terminal.json"
                                            val bundled = withContext(Dispatchers.IO) { assetManager.open(path).bufferedReader().use { it.readText() } }
                                            jsonText = bundled
                                            themeManager.saveThemeConfig(themeConfig.copy(customThemeJson = bundled))
                                            onThemeChanged()
                                            loadStatus = "Loaded bundled theme: bountu_empty_terminal.json"
                                        } catch (e: Exception) {
                                            loadStatus = "No JSON themes found in Git or assets"
                                        }
                                    }
                                } else if (info is com.chatxstudio.bountu.git.GitResult.Error) {
                                    // Directly fallback to assets
                                    try {
                                        val assetManager = context.assets
                                        val path = "config/themes/bountu_empty_terminal.json"
                                        val bundled = withContext(Dispatchers.IO) { assetManager.open(path).bufferedReader().use { it.readText() } }
                                        jsonText = bundled
                                        themeManager.saveThemeConfig(themeConfig.copy(customThemeJson = bundled))
                                        onThemeChanged()
                                        loadStatus = "Loaded bundled theme: bountu_empty_terminal.json"
                                    } catch (e: Exception) {
                                        loadStatus = "Repo not initialized and no bundled theme found"
                                    }
                                }
                            }
                        }) {
                            Text("Load from Git/Assets")
                        }
                    }
                    if (loadStatus.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(loadStatus, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }


        // Font Size Slider
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Font Size",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${themeConfig.fontSize.toInt()} sp",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Slider(
                        value = themeConfig.fontSize,
                        onValueChange = { newValue ->
                            themeConfig = themeConfig.copy(fontSize = newValue)
                        },
                        valueRange = 10f..24f,
                        steps = 14,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Save Button
        Button(
            onClick = {
                coroutineScope.launch {
                    themeManager.saveThemeConfig(themeConfig)
                    onThemeChanged()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text("Apply Changes")
        }
    }
}


