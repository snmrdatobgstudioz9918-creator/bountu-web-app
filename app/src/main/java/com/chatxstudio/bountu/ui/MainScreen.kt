package com.chatxstudio.bountu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import com.chatxstudio.bountu.terminal.TerminalViewModel
import com.chatxstudio.bountu.security.SecuritySettingsScreen
import com.chatxstudio.bountu.theme.ThemeConfigurationScreen
import com.chatxstudio.bountu.communication.CommunicationManager
import com.chatxstudio.bountu.security.SecurityManager
import com.chatxstudio.bountu.theme.ThemeManager
import com.chatxstudio.bountu.theme.ThemeConfig
import com.chatxstudio.bountu.packages.PackageManager
import com.chatxstudio.bountu.packages.PackageFilter
import com.chatxstudio.bountu.packages.InstallationResult
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch

// Define the screens for navigation
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Terminal : Screen("terminal", "Terminal", Icons.Filled.Terminal)
    object Themes : Screen("themes", "Themes", Icons.Filled.Palette)
    object Security : Screen("security", "Security", Icons.Filled.Lock)
    object Profile : Screen("profile", "Profile", Icons.Filled.Person)
    object Connection : Screen("connection", "Connection", Icons.Filled.Wifi)
    object Packages : Screen("packages", "Packages", Icons.Filled.Search)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    communicationManager: CommunicationManager,
    securityManager: SecurityManager,
    themeManager: ThemeManager,
    accountManager: com.chatxstudio.bountu.auth.AccountManager? = null
) {
    val navController = rememberNavController()
    val bottomBarState = remember { mutableStateOf(true) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val updater = remember { com.chatxstudio.bountu.update.AutoUpdateInstaller(context) }

    // Update dialog state
    var updateDialog by remember { mutableStateOf<Pair<String, String>?>(null) } // Pair(current, latest)
    var updateChecking by remember { mutableStateOf(false) }

    // Get current theme config to apply to the whole app
    var currentThemeConfig by remember { 
        mutableStateOf(ThemeConfig()) 
    }
    
    LaunchedEffect(Unit) {
        currentThemeConfig = runBlocking { themeManager.getThemeConfig() }
    }
    
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    // Cooldown tracking to avoid API rate limits
    var lastUpdateCheck by remember { mutableStateOf(0L) }
    val now = System.currentTimeMillis()
    val cooldownMs = 60_000L
    val canCheck = !updateChecking && (now - lastUpdateCheck >= cooldownMs)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bountu") },
                actions = {
                    IconButton(
                        onClick = {
                            if (!canCheck) {
                                scope.launch {
                                    val wait = ((cooldownMs - (now - lastUpdateCheck)).coerceAtLeast(0L) / 1000).toInt()
                                    snackbarHostState.showSnackbar("Please wait ${wait}s before checking again")
                                }
                                return@IconButton
                            }
                            updateChecking = true
                            lastUpdateCheck = System.currentTimeMillis()
                            scope.launch {
                                val current = updater.getCurrentVersion()
                                val result = updater.checkGithubLatest(
                                    owner = "snmrdatobgstudioz9918-creator",
                                    repo = "bountu-android"
                                )
                                updateChecking = false
                                if (result.rateLimited) {
                                    updateDialog = current to (result.latest ?: "latest")
                                } else if (result.latest != null && updater.compareVersions(current, result.latest) < 0) {
                                    updateDialog = current to result.latest
                                } else {
                                    snackbarHostState.showSnackbar("You are up to date")
                                }
                            }
                        },
                        enabled = canCheck
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Check for updates")
                    }
                    if (accountManager != null) {
                        IconButton(onClick = { accountManager.logout() }) {
                            Icon(Icons.Filled.Lock, contentDescription = "Logout")
                        }
                    }
                }
            )
        },
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (bottomBarState.value) {
                BottomNavigationBar(
                    navController = navController,
                    currentThemeConfig = currentThemeConfig
                )
            }
        }
    ) { paddingValues ->
        if (updateDialog != null) {
            val (current, latest) = updateDialog!!
            // We don't know here if it's rateLimited; to keep it simple, show update dialog if latest looks like a version; else show load message
            val isVersion = latest.any { it.isDigit() }
            if (isVersion) {
                AlertDialog(
                    onDismissRequest = { updateDialog = null },
                    title = { Text("Update available") },
                    text = { Text("A new version $latest is available (you have $current). Open releases page?") },
                    confirmButton = {
                        TextButton(onClick = {
                            updateDialog = null
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(
                                "https://github.com/snmrdatobgstudioz9918-creator/bountu-android/releases/latest"
                            ))
                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }) { Text("Open") }
                    },
                    dismissButton = { TextButton(onClick = { updateDialog = null }) { Text("Later") } }
                )
            } else {
                AlertDialog(
                    onDismissRequest = { updateDialog = null },
                    title = { Text("GitHub traffic is high") },
                    text = { Text("GitHub is currently experiencing high traffic or rate limiting. Please try checking for updates again in a few minutes.") },
                    confirmButton = { TextButton(onClick = { updateDialog = null }) { Text("OK") } }
                )
            }
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Terminal.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Terminal.route) {
                TerminalView(
                    communicationManager = communicationManager,
                    securityManager = securityManager
                )
            }
            composable(Screen.Themes.route) {
                ThemeConfigurationScreen(
                    themeManager = themeManager,
                    onThemeChanged = {
                        // Refresh theme config
                        currentThemeConfig = runBlocking { themeManager.getThemeConfig() }
                    }
                )
            }
            composable(Screen.Security.route) {
                SecuritySettingsScreen(securityManager = securityManager)
            }
            composable(Screen.Profile.route) {
                accountManager?.let { ProfileCustomizationScreen(it) }
            }
            composable(Screen.Connection.route) {
                ConnectionScreen(communicationManager = communicationManager)
            }
            composable(Screen.Packages.route) {
                GitPackagesScreen()
            }
        }
    }
}

@Composable
fun TerminalView(
    communicationManager: CommunicationManager,
    securityManager: SecurityManager
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val viewModel = remember { TerminalViewModel(communicationManager, securityManager) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Display terminal output
        LazyColumn(
            modifier = Modifier
                .weight(1f)
        ) {
            item {
                Text(
                    text = viewModel.terminalOutput,
                    color = androidx.compose.ui.graphics.Color.Green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            }
        }
        
        // Command input
        TextField(
            value = viewModel.commandInput,
            onValueChange = { viewModel.updateCommandInput(it) },
            label = { Text("Enter command", color = Color.White) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = TextStyle(
                color = Color.White,
                fontFamily = FontFamily.Monospace
            ),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    viewModel.executeCommand(viewModel.commandInput)
                    viewModel.clearCommandInput()
                }
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = {
                viewModel.executeCommand(viewModel.commandInput)
                viewModel.clearCommandInput()
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Execute")
        }
    }
}

@Composable
fun BottomNavigationBar(
    navController: androidx.navigation.NavHostController,
    currentThemeConfig: ThemeConfig
) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Terminal, contentDescription = "Terminal") },
            label = { Text("Terminal") },
            selected = navController.currentDestination?.route == Screen.Terminal.route,
            onClick = {
                navController.navigate(Screen.Terminal.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            },
            alwaysShowLabel = false
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Palette, contentDescription = "Themes") },
            label = { Text("Themes") },
            selected = navController.currentDestination?.route == Screen.Themes.route,
            onClick = {
                navController.navigate(Screen.Themes.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            },
            alwaysShowLabel = false
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Lock, contentDescription = "Security") },
            label = { Text("Security") },
            selected = navController.currentDestination?.route == Screen.Security.route,
            onClick = {
                navController.navigate(Screen.Security.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            },
            alwaysShowLabel = false
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = navController.currentDestination?.route == Screen.Profile.route,
            onClick = {
                navController.navigate(Screen.Profile.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            },
            alwaysShowLabel = false
        )
        NavigationBarItem(
            icon = { Icon(Icons.Filled.Wifi, contentDescription = "Connection") },
            label = { Text("Connection") },
            selected = navController.currentDestination?.route == Screen.Connection.route,
            onClick = {
                navController.navigate(Screen.Connection.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            },
            alwaysShowLabel = false
        )
        NavigationBarItem(
            icon = { 
                Box {
                    Icon(Icons.Filled.Search, contentDescription = "Packages")
                    // Under development badge
                    Icon(
                        Icons.Filled.Build,
                        contentDescription = "Under Development",
                        modifier = Modifier
                            .size(12.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            },
            label = { Text("Packages") },
            selected = navController.currentDestination?.route == Screen.Packages.route,
            onClick = {
                navController.navigate(Screen.Packages.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            },
            alwaysShowLabel = false
        )
    }
}

@Composable
fun ConnectionScreen(communicationManager: CommunicationManager) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Connection Settings",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Connection Status",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = if (communicationManager.isConnectedToWindows) {
                        "Connected to Windows at ${communicationManager.windowsHostAddress}"
                    } else {
                        "Not connected to Windows"
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { communicationManager.discoverWindowsInstances() },
                    enabled = !communicationManager.isConnectedToWindows
                ) {
                    Text("Discover Windows Instances")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { 
                        if (communicationManager.isConnectedToWindows) {
                            communicationManager.stopCommunicationService()
                        } else {
                            communicationManager.startCommunicationService()
                            // Immediately attempt discovery to actually connect
                            communicationManager.discoverWindowsInstances()
                        }
                    }
                ) {
                    Text(
                        if (communicationManager.isConnectedToWindows) "Disconnect" else "Connect"
                    )
                }
            }
        }
    }
}

// Package data class
data class PackageInfo(
    val name: String,
    val version: String,
    val description: String,
    val isInstalled: Boolean = false,
    val needsMaintenance: Boolean = false,
    val maintenanceReason: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackagesSearchScreen(communicationManager: CommunicationManager) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val packageManager = remember { PackageManager(context) }
    val scope = rememberCoroutineScope()
    
    var searchQuery by remember { mutableStateOf("") }
    var installationMessage by remember { mutableStateOf("") }
    var showInstallDialog by remember { mutableStateOf(false) }
    
    val availablePackages by packageManager.availablePackages.collectAsState()
    val installationProgress by packageManager.installationProgress.collectAsState()
    val isLoadingPackages by packageManager.isLoadingPackages.collectAsState()
    
    // Auto-sync packages from Git on first load
    LaunchedEffect(Unit) {
        packageManager.syncPackagesFromGit()
    }
    
    val filteredPackages = remember(searchQuery, availablePackages) {
        if (searchQuery.isEmpty()) {
            availablePackages
        } else {
            packageManager.searchPackages(
                PackageFilter(query = searchQuery)
            )
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Under Development Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Build,
                    contentDescription = "Under Development",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "🚧 Under Development",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "This feature is currently being developed. Some functionality may be limited.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Package Searcher",
                style = MaterialTheme.typography.headlineMedium
            )
            
            // Refresh button
            IconButton(
                onClick = {
                    scope.launch {
                        // Force refresh to get latest from GitHub
                        packageManager.syncPackagesFromGit(forceRefresh = true)
                    }
                },
                enabled = !isLoadingPackages
            ) {
                if (isLoadingPackages) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Refresh,
                        contentDescription = "Refresh packages",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search packages...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            singleLine = true
        )
        
        // Installation progress
        installationProgress?.let { (pkgName, progress) ->
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            Text(
                text = "Installing $pkgName... ${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        
        // Package list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredPackages) { pkg ->
                PackageCard(
                    packageInfo = PackageInfo(
                        name = pkg.name,
                        version = pkg.version,
                        description = pkg.description,
                        isInstalled = pkg.isInstalled,
                        needsMaintenance = pkg.needsMaintenance,
                        maintenanceReason = pkg.maintenanceReason
                    ),
                    onInstallClick = {
                        scope.launch {
                            val result = packageManager.installPackage(pkg.id)
                            when (result) {
                                is InstallationResult.Success -> {
                                    installationMessage = result.message
                                    showInstallDialog = true
                                }
                                is InstallationResult.Failure -> {
                                    installationMessage = result.error
                                    showInstallDialog = true
                                }
                                else -> {}
                            }
                        }
                    },
                    onMaintenanceClick = {
                        scope.launch {
                            val result = packageManager.fixMaintenance(pkg.id)
                            when (result) {
                                is InstallationResult.Success -> {
                                    installationMessage = result.message
                                    showInstallDialog = true
                                }
                                is InstallationResult.Failure -> {
                                    installationMessage = result.error
                                    showInstallDialog = true
                                }
                                else -> {}
                            }
                        }
                    }
                )
            }
            
            if (filteredPackages.isEmpty() && searchQuery.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No packages found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
    
    // Installation result dialog
    if (showInstallDialog) {
        AlertDialog(
            onDismissRequest = { showInstallDialog = false },
            title = { Text("Package Manager") },
            text = { Text(installationMessage) },
            confirmButton = {
                Button(onClick = { showInstallDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun PackageCard(
    packageInfo: PackageInfo,
    onInstallClick: () -> Unit,
    onMaintenanceClick: () -> Unit
) {
    var showDetailsDialog by remember { mutableStateOf(false) }
    
    // Pulsing animation for maintenance warning
    val infiniteTransition = rememberInfiniteTransition(label = "maintenance_pulse")
    val maintenanceAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDetailsDialog = true },
        colors = CardDefaults.cardColors(
            containerColor = if (packageInfo.needsMaintenance) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = packageInfo.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (packageInfo.isInstalled) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "Installed",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                    Text(
                        text = "v${packageInfo.version}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                if (packageInfo.needsMaintenance) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = "Needs Maintenance",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = maintenanceAlpha),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = packageInfo.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            // Maintenance warning
            AnimatedVisibility(visible = packageInfo.needsMaintenance) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = packageInfo.maintenanceReason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (packageInfo.needsMaintenance) {
                    Button(
                        onClick = onMaintenanceClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Filled.Build,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fix")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Button(
                    onClick = onInstallClick,
                    enabled = !packageInfo.isInstalled
                ) {
                    Text(if (packageInfo.isInstalled) "Installed" else "Install")
                }
            }
        }
    }
    
    // Package details dialog
    if (showDetailsDialog) {
        PackageDetailsDialog(
            packageInfo = packageInfo,
            onDismiss = { showDetailsDialog = false },
            onInstallClick = {
                showDetailsDialog = false
                onInstallClick()
            },
            onMaintenanceClick = {
                showDetailsDialog = false
                onMaintenanceClick()
            }
        )
    }
}

@Composable
fun PackageDetailsDialog(
    packageInfo: PackageInfo,
    onDismiss: () -> Unit,
    onInstallClick: () -> Unit,
    onMaintenanceClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Column {
                Text(
                    text = packageInfo.name,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Version ${packageInfo.version}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Installation status
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status:",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.width(100.dp)
                    )
                    Surface(
                        color = if (packageInfo.isInstalled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = if (packageInfo.isInstalled) "Installed" else "Not Installed",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = if (packageInfo.isInstalled) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                
                HorizontalDivider()
                
                // Description
                Column {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = packageInfo.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
                
                // Maintenance warning if applicable
                if (packageInfo.needsMaintenance) {
                    HorizontalDivider()
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Maintenance Required",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = packageInfo.maintenanceReason,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (packageInfo.needsMaintenance) {
                    Button(
                        onClick = onMaintenanceClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            Icons.Filled.Build,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Fix")
                    }
                }
                
                Button(
                    onClick = onInstallClick,
                    enabled = !packageInfo.isInstalled
                ) {
                    Text(if (packageInfo.isInstalled) "Installed" else "Install")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
