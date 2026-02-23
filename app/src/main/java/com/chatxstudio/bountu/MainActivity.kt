package com.chatxstudio.bountu

import android.Manifest
import android.content.pm.PackageManager as AndroidPackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.chatxstudio.bountu.communication.CommunicationManager
import com.chatxstudio.bountu.git.GitPackageManager
import com.chatxstudio.bountu.git.GitResult
import com.chatxstudio.bountu.git.MaintenanceStatus
import com.chatxstudio.bountu.network.ConnectionMonitor
import com.chatxstudio.bountu.packages.PackageManager as BountuPackageManager
import com.chatxstudio.bountu.security.SecurityManager
import com.chatxstudio.bountu.service.BountuBackgroundService
import com.chatxstudio.bountu.sync.AutoSyncManager
import com.chatxstudio.bountu.sync.SyncResult
import com.chatxstudio.bountu.sync.SyncState
import com.chatxstudio.bountu.theme.AppTheme
import com.chatxstudio.bountu.utils.BatteryOptimizationHelper
import com.chatxstudio.bountu.theme.ThemeConfig
import com.chatxstudio.bountu.theme.ThemeManager
import com.chatxstudio.bountu.ui.GitErrorScreen
import com.chatxstudio.bountu.ui.auth.LoginScreen
import com.chatxstudio.bountu.auth.AccountManager
import com.chatxstudio.bountu.ui.MainScreen
import com.chatxstudio.bountu.ui.MaintenanceScreen
import com.chatxstudio.bountu.update.AutoUpdateInstaller
import com.chatxstudio.bountu.ui.SyncErrorScreen
import com.chatxstudio.bountu.ui.theme.BountuTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * App states for navigation
 */
enum class AppState {
    Initializing,
    Syncing,
    SyncError,
    Ready
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Start background service to keep app running
        BountuBackgroundService.start(this)
        
        // Request battery optimization exemption to keep app running
        if (!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(this)) {
            BatteryOptimizationHelper.requestIgnoreBatteryOptimizations(this)
        }
        
        setContent {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val communicationManager = remember { CommunicationManager(context) }
            val securityManager = remember { SecurityManager(context) }
            val themeManager = remember { ThemeManager(context) }
            val gitManager = remember { GitPackageManager(context) }
            val packageManager = remember { BountuPackageManager(context) }
            val connectionMonitor = remember { ConnectionMonitor(context) }
            val autoSyncManager = remember { AutoSyncManager(context, gitManager, connectionMonitor) }
            val updateInstaller = remember { AutoUpdateInstaller(context) }
            val accountManager = remember { AccountManager(context) }

            // State
            var appState by remember { mutableStateOf<AppState>(AppState.Initializing) }
            var maintenanceStatus by remember { mutableStateOf<MaintenanceStatus?>(null) }
            // Update prompt state for first-launch check
            var showUpdatePrompt by remember { mutableStateOf(false) }
            var latestTag by remember { mutableStateOf<String?>(null) }
            var rateLimited by remember { mutableStateOf(false) }
            
            // Collect states

            val syncState by autoSyncManager.syncState.collectAsState()
            val connectionState by connectionMonitor.connectionState.collectAsState()
            val pingMs by connectionMonitor.pingMs.collectAsState()
            val isLoggedIn by accountManager.isLoggedIn.collectAsState()
            
            // Permission state
            var permissionsGranted by remember { mutableStateOf(false) }
            var showPermissionDialog by remember { mutableStateOf(false) }
            
            // Permission launcher
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                permissionsGranted = permissions.values.all { it }
                if (!permissionsGranted) {
                    showPermissionDialog = true
                }
            }

            // Check permissions on start
            LaunchedEffect(Unit) {
                val requiredPermissions = mutableListOf(
                    Manifest.permission.INTERNET,
                    Manifest.permission.ACCESS_NETWORK_STATE
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                
                val allGranted = requiredPermissions.all { permission ->
                    ContextCompat.checkSelfPermission(context, permission) == AndroidPackageManager.PERMISSION_GRANTED
                }
                
                if (allGranted) {
                    permissionsGranted = true
                } else {
                    permissionLauncher.launch(requiredPermissions.toTypedArray())
                }
            }

            // Load theme config reactively
            val themeConfig by themeManager.getThemeConfigFlow().collectAsState(initial = ThemeConfig())

            // Check for app updates and force reload if updated
            LaunchedEffect(Unit) {
                updateInstaller.checkAndInstallPendingUpdates()
            }
            
            // Perform initial sync with connection monitoring and first-launch update check
            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn) return@LaunchedEffect
                try {
                    appState = AppState.Syncing
                    Log.d("MainActivity", "Starting initial sync...")

                    // Perform sync with AutoSyncManager
                    val syncResult = autoSyncManager.performInitialSync()

                        when (syncResult) {
                            is SyncResult.Success -> {
                            Log.d("MainActivity", "Sync successful: ${syncResult.packageCount} packages")

                            // Sync packages with package manager
                            packageManager.syncPackagesFromGit()

                            // Check maintenance status
                            when (val statusResult = gitManager.getMaintenanceStatus()) {
                                is GitResult.Success -> {
                                    maintenanceStatus = statusResult.data
                                    Log.d("MainActivity", "Maintenance status: ${statusResult.data}")
                                }
                                is GitResult.Error -> {
                                    maintenanceStatus = MaintenanceStatus()
                                    Log.w("MainActivity", "Using default maintenance status")
                                }
                            }

                            // First-launch update check against GitHub releases
                            val current = updateInstaller.getCurrentVersion()
                            val gh = updateInstaller.checkGithubLatest(
                                owner = "snmrdatobgstudioz9918-creator",
                                repo = "bountu-android"
                            )
                            rateLimited = gh.rateLimited
                            latestTag = gh.latest
                            if (!rateLimited && gh.latest != null && updateInstaller.compareVersions(current, gh.latest) < 0) {
                                showUpdatePrompt = true
                            }

                            appState = AppState.Ready
                        }
                        is SyncResult.Failed -> {
                            Log.e("MainActivity", "Sync failed: ${syncResult.error.message}")
                            appState = AppState.SyncError
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Sync exception: ${e.message}", e)
                    appState = AppState.SyncError
                }
            }
            
            // No need for manual loading cycle - sync manager handles it

            AppTheme(themeManager = themeManager, themeConfig = themeConfig) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(Modifier.fillMaxSize()) {
                        // If not logged in, show Login/Register first
                        val loggedIn = isLoggedIn
                        if (!loggedIn) {
                            LoginScreen(
                                accountManager = accountManager,
                                onLoginSuccess = { /* reactive via isLoggedIn flow */ }
                            )
                        } else {
                        // Render based on app state
                        when (appState) {
                            AppState.Initializing -> {
                                LoadingScreen("Initializing...")
                            }
                            
                            AppState.Syncing -> {
                                // Show sync status with connection info
                                SyncingScreen(
                                    syncState = syncState,
                                    connectionState = connectionState,
                                    pingMs = pingMs
                                )
                            }
                            
                            AppState.SyncError -> {
                                // Show sync error screen with retry
                                SyncErrorScreen(
                                    syncManager = autoSyncManager,
                                    connectionMonitor = connectionMonitor,
                                    onRetry = {
                                        scope.launch {
                                            appState = AppState.Syncing
                                            val result = autoSyncManager.retrySync()
                                            when (result) {
                                                is SyncResult.Success -> {
                                                    packageManager.syncPackagesFromGit()
                                                    appState = AppState.Ready
                                                }
                                                is SyncResult.Failed -> {
                                                    appState = AppState.SyncError
                                                }
                                            }
                                        }
                                    },
                                    onExit = { finish() }
                                )
                            }
                            
                            AppState.Ready -> {
                                // Check maintenance status
                                if (maintenanceStatus?.isEnabled == true) {
                                    MaintenanceScreen(
                                        maintenanceStatus = maintenanceStatus!!,
                                        onRetry = {
                                            scope.launch {
                                                when (val statusResult = gitManager.getMaintenanceStatus()) {
                                                    is GitResult.Success -> {
                                                        maintenanceStatus = statusResult.data
                                                    }
                                                    is GitResult.Error -> {
                                                        maintenanceStatus = MaintenanceStatus()
                                                    }
                                                }
                                            }
                                        }
                                    )
                                } else {
                                    // Show main app
                                    Box(Modifier.fillMaxSize()) {
                                        MainScreen(
                                            communicationManager = communicationManager,
                                            securityManager = securityManager,
                                            themeManager = themeManager,
                                            accountManager = accountManager
                                        )
                                        if (showUpdatePrompt && latestTag != null) {
                                            AlertDialog(
                                                onDismissRequest = { showUpdatePrompt = false },
                                                title = { Text("Update available") },
                                                text = { Text("A new version ${latestTag} is available. Download and install now?") },
                                                confirmButton = {
                                                    TextButton(onClick = {
                                                        showUpdatePrompt = false
                                                        scope.launch {
                                                            val file = updateInstaller.downloadLatestApkFromGithub(
                                                                owner = "snmrdatobgstudioz9918-creator",
                                                                repo = "bountu-android"
                                                            )
                                                            if (file != null) {
                                                                updateInstaller.installUpdate(file)
                                                            }
                                                        }
                                                    }) { Text("Install") }
                                                },
                                                dismissButton = { TextButton(onClick = { showUpdatePrompt = false }) { Text("Later") } }
                                            )
                                        }
                                        if (rateLimited) {
                                            AlertDialog(
                                                onDismissRequest = { rateLimited = false },
                                                title = { Text("GitHub traffic is high") },
                                                text = { Text("GitHub is rate limiting. Update check may be delayed; please try again later.") },
                                                confirmButton = { TextButton(onClick = { rateLimited = false }) { Text("OK") } }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Loading screen
 */
@Composable
fun LoadingScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Syncing screen with connection info
 */
@Composable
fun SyncingScreen(
    syncState: SyncState,
    connectionState: com.chatxstudio.bountu.network.ConnectionState,
    pingMs: Long?
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            CircularProgressIndicator()
            
            Text(
                text = when (syncState) {
                    is SyncState.Checking -> "Checking connection..."
                    is SyncState.Syncing -> "Syncing... (${syncState.attempt}/${syncState.maxAttempts})"
                    is SyncState.Retrying -> "Retrying... (${syncState.attempt}/${syncState.maxAttempts})"
                    else -> "Syncing with repository..."
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Connection info card
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
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Network:")
                        Text(
                            text = when (connectionState) {
                                is com.chatxstudio.bountu.network.ConnectionState.Connected -> "Connected"
                                is com.chatxstudio.bountu.network.ConnectionState.Disconnected -> "Disconnected"
                                is com.chatxstudio.bountu.network.ConnectionState.Limited -> "Limited"
                                else -> "Checking..."
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ping:")
                        Text(
                            text = if (pingMs != null) "${pingMs}ms" else "N/A",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

