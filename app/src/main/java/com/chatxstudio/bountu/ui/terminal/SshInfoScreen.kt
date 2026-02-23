package com.chatxstudio.bountu.ui.terminal

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.chatxstudio.bountu.ssh.SshInfo

@Composable
fun SshInfoScreen() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val details = remember { SshInfo.currentDeviceAsServer() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("SSH Connection", style = MaterialTheme.typography.headlineSmall)
        if (details == null) {
            Text("No network IP found. Connect to Wi‑Fi or hotspot.")
        } else {
            Text("Host: ${details.host}")
            Text("Port: ${details.port}")
            Text("User: ${details.username}")
            val cli = details.toCli()
            OutlinedTextField(
                value = cli,
                onValueChange = {},
                label = { Text("CLI") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { clipboard.setText(AnnotatedString(cli)) }) { Text("Copy CLI") }
                Button(onClick = { SshInfo.openInTermius(context, details) }) { Text("Open in Termius") }
            }
        }
        Text("Tip: On Termius, create a new host and paste the CLI or use Open in Termius.", style = MaterialTheme.typography.bodySmall)
    }
}
