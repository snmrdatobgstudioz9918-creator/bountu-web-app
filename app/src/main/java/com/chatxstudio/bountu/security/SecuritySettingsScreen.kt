package com.chatxstudio.bountu.security

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(securityManager: SecurityManager) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Security Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Secure Mode",
                        fontSize = 16.sp
                    )
                    val secureMode by securityManager.isSecureMode.collectAsState()
                    Switch(
                        checked = secureMode,
                        onCheckedChange = { securityManager.toggleSecureMode() }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Secure mode adds extra validation to commands and network communications",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Permission Status",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                val permissionsGranted by securityManager.permissionsGranted.collectAsState()

                // Launcher to request permissions
                val launcher = rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
                ) { _ ->
                    // Refresh status after user responds
                    securityManager.checkPermissions()
                }
                
                if (permissionsGranted) {
                    Text(
                        text = "All required permissions granted ✓",
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Some permissions pending ✗",
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Button(
                        onClick = { 
                            val toRequest = securityManager.requestPermissions()
                            if (toRequest.isNotEmpty()) {
                                launcher.launch(toRequest)
                            } else {
                                securityManager.checkPermissions()
                            }
                        },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Request Permissions")
                    }
                }
            }
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Security Log",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Show last 10 security events
                val logEntries = securityManager.securityLog.takeLast(10)
                
                LazyColumn {
                    items(logEntries) { entry ->
                        Text(
                            text = entry,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}