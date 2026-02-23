package com.chatxstudio.bountu.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatxstudio.bountu.network.ConnectionMonitor
import com.chatxstudio.bountu.network.ConnectionQuality
import com.chatxstudio.bountu.network.ConnectionState
import com.chatxstudio.bountu.sync.AutoSyncManager
import com.chatxstudio.bountu.sync.SyncError
import com.chatxstudio.bountu.sync.SyncState
import kotlinx.coroutines.launch

/**
 * Sync Error Screen
 * Displayed when app cannot sync with Git repository
 */
@Composable
fun SyncErrorScreen(
    syncManager: AutoSyncManager,
    connectionMonitor: ConnectionMonitor,
    onRetry: () -> Unit,
    onExit: () -> Unit
) {
    val syncState by syncManager.syncState.collectAsState()
    val retryCount by syncManager.retryCount.collectAsState()
    val connectionState by connectionMonitor.connectionState.collectAsState()
    val pingMs by connectionMonitor.pingMs.collectAsState()
    
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Error Icon
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = "Sync Error",
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title
            Text(
                text = "Cannot Connect",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Error Message
            when (val state = syncState) {
                is SyncState.Failed -> {
                    Text(
                        text = state.error.message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is SyncState.Syncing -> {
                    Text(
                        text = "Syncing... (Attempt ${state.attempt}/${state.maxAttempts})",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
                is SyncState.Retrying -> {
                    Text(
                        text = "Retrying... (${state.attempt}/${state.maxAttempts})",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    Text(
                        text = "Unable to sync with repository",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Connection Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Connection Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Network Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = when (connectionState) {
                                    is ConnectionState.Connected -> Icons.Default.Wifi
                                    is ConnectionState.Disconnected -> Icons.Default.WifiOff
                                    is ConnectionState.Limited -> Icons.Default.SignalWifiStatusbarConnectedNoInternet4
                                    else -> Icons.Default.WifiOff
                                },
                                contentDescription = null,
                                tint = when (connectionState) {
                                    is ConnectionState.Connected -> Color.Green
                                    else -> MaterialTheme.colorScheme.error
                                }
                            )
                            
                            Text(
                                text = "Network",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Text(
                            text = when (connectionState) {
                                is ConnectionState.Connected -> "Connected"
                                is ConnectionState.Disconnected -> "Disconnected"
                                is ConnectionState.Limited -> "Limited"
                                is ConnectionState.Checking -> "Checking..."
                                else -> "Unknown"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (connectionState) {
                                is ConnectionState.Connected -> Color.Green
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Ping
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null
                            )
                            
                            Text(
                                text = "Ping",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Text(
                            text = if (pingMs != null) "${pingMs}ms" else "N/A",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                pingMs == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                pingMs!! < 100 -> Color.Green
                                pingMs!! < 300 -> Color(0xFFFFA500) // Orange
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Connection Quality
                    val quality = connectionMonitor.getConnectionQuality()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SignalCellularAlt,
                                contentDescription = null
                            )
                            
                            Text(
                                text = "Quality",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Text(
                            text = when (quality) {
                                ConnectionQuality.Excellent -> "Excellent"
                                ConnectionQuality.Good -> "Good"
                                ConnectionQuality.Fair -> "Fair"
                                ConnectionQuality.Poor -> "Poor"
                                ConnectionQuality.VeryPoor -> "Very Poor"
                                ConnectionQuality.Unknown -> "Unknown"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Retry Button
                Button(
                    onClick = {
                        scope.launch {
                            onRetry()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = syncState !is SyncState.Syncing && syncState !is SyncState.Retrying,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (syncState is SyncState.Syncing || syncState is SyncState.Retrying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Retry Sync",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Exit Button
                OutlinedButton(
                    onClick = onExit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Exit App",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Retry Count
            if (retryCount > 0) {
                Text(
                    text = "Retry attempts: $retryCount",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
