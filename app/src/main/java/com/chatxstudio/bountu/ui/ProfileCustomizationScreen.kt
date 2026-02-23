package com.chatxstudio.bountu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chatxstudio.bountu.auth.AccountManager
import kotlinx.coroutines.launch

@Composable
fun ProfileCustomizationScreen(accountManager: AccountManager) {
    val scope = rememberCoroutineScope()
    var font by remember { mutableStateOf("monospace") }
    var size by remember { mutableStateOf(14) }

    // Load current prefs
    LaunchedEffect(Unit) {
        val user = accountManager.currentUser.value
        if (user != null) {
            font = user.preferences.terminalFont
            size = user.preferences.terminalFontSize
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Profile & Terminal Preferences", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = font,
            onValueChange = { font = it },
            label = { Text("Terminal font family") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Font size: $size sp", modifier = Modifier.weight(1f))
            Slider(
                value = size.toFloat(),
                onValueChange = { size = it.toInt() },
                valueRange = 10f..24f,
                steps = 14,
                modifier = Modifier.weight(2f)
            )
        }
        Button(onClick = {
            scope.launch {
                val user = accountManager.currentUser.value ?: return@launch
                accountManager.updatePreferences(
                    user.preferences.copy(
                        terminalFont = font,
                        terminalFontSize = size
                    )
                )
            }
        }) { Text("Save") }
        Text("Note: These settings affect Terminal only. App theme remains unchanged.", style = MaterialTheme.typography.bodySmall)
    }
}
