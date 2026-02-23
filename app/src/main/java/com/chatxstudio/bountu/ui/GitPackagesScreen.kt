package com.chatxstudio.bountu.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chatxstudio.bountu.git.GitPackageManager
import com.chatxstudio.bountu.git.GitResult
import com.chatxstudio.bountu.git.PackageMetadata
import com.chatxstudio.bountu.git.RepositoryInfo
import kotlinx.coroutines.launch
import com.chatxstudio.bountu.packages.PackageManager as BountuPackageManager
import com.chatxstudio.bountu.packages.InstallationResult
import com.chatxstudio.bountu.ui.components.Badge
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitPackagesScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gitManager = remember { GitPackageManager(context) }
    val pkgManager = remember { BountuPackageManager(context) }

    var isInitialized by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Not initialized") }
    var packages by remember { mutableStateOf<List<String>>(emptyList()) }
    var repositoryInfo by remember { mutableStateOf<RepositoryInfo?>(null) }
    var selectedPackage by remember { mutableStateOf<PackageMetadata?>(null) }
    var showPackageDialog by remember { mutableStateOf(false) }
    var installing by remember { mutableStateOf(false) }
    var installMessage by remember { mutableStateOf<String?>(null) }

    // Check if repository is initialized on start
    LaunchedEffect(Unit) {
        isInitialized = gitManager.isRepositoryInitialized()
        if (isInitialized) {
            statusMessage = "Repository initialized"
            // Load repository info
            when (val result = gitManager.getRepositoryInfo()) {
                is GitResult.Success -> repositoryInfo = result.data
                is GitResult.Error -> statusMessage = "Error: ${result.message}"
            }
            // Load packages
            when (val result = gitManager.listPackages()) {
                is GitResult.Success -> packages = result.data
                is GitResult.Error -> statusMessage = "Error: ${result.message}"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Git-Based Packages") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isInitialized)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isInitialized) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = if (isInitialized)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Repository Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    repositoryInfo?.let { info ->
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Local Path: ${info.localPath}", style = MaterialTheme.typography.bodySmall)
                        Text("Commit: ${info.currentCommit.take(8)}", style = MaterialTheme.typography.bodySmall)
                        Text("Packages: ${packages.size}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            statusMessage = "Initializing repository..."

                            // Using the official Bountu packages repository
                            val repoUrl = "https://github.com/snmrdatobgstudioz9918-creator/bountu-packages-global.git"

                            when (val result = gitManager.initialize(repoUrl)) {
                                is GitResult.Success -> {
                                    isInitialized = true
                                    statusMessage = "Repository initialized successfully!"

                                    // Load packages
                                    when (val packagesResult = gitManager.listPackages()) {
                                        is GitResult.Success -> packages = packagesResult.data
                                        is GitResult.Error -> statusMessage = "Error loading packages: ${packagesResult.message}"
                                    }

                                    // Load repo info
                                    when (val infoResult = gitManager.getRepositoryInfo()) {
                                        is GitResult.Success -> repositoryInfo = infoResult.data
                                        is GitResult.Error -> {}
                                    }
                                }
                                is GitResult.Error -> {
                                    statusMessage = "Initialization failed: ${result.message}"
                                }
                            }
                            isLoading = false
                        }
                    },
                    enabled = !isLoading && !isInitialized,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Initialize")
                }

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            statusMessage = "Syncing repository..."

                            when (val result = gitManager.syncRepository()) {
                                is GitResult.Success -> {
                                    val syncStatus = result.data
                                    statusMessage = if (syncStatus.hasUpdates) {
                                        "Updated: ${syncStatus.beforeCommit.take(8)} → ${syncStatus.afterCommit.take(8)}"
                                    } else {
                                        "Already up to date"
                                    }

                                    // Reload packages
                                    when (val packagesResult = gitManager.listPackages()) {
                                        is GitResult.Success -> packages = packagesResult.data
                                        is GitResult.Error -> {}
                                    }
                                }
                                is GitResult.Error -> {
                                    statusMessage = "Sync failed: ${result.message}"
                                }
                            }
                            isLoading = false
                        }
                    },
                    enabled = !isLoading && isInitialized,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Sync, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sync")
                }

                Button(
                    onClick = {
                        scope.launch {
                            // Simple custom package creator with defaults
                            val newId = "custom-" + System.currentTimeMillis().toString().takeLast(6)
                            val result = gitManager.createCustomPackage(
                                id = newId,
                                name = "Custom Package",
                                version = "1.0.0",
                                description = "Your custom package",
                                platform = "android",
                                architecture = "aarch64",
                                downloadUrl = "",
                                checksumSha256 = ""
                            )
                            if (result is GitResult.Success) {
                                statusMessage = "Created $newId"
                                when (val packagesResult = gitManager.listPackages()) {
                                    is GitResult.Success -> packages = packagesResult.data
                                    is GitResult.Error -> {}
                                }
                            } else if (result is GitResult.Error) {
                                statusMessage = result.message
                            }
                        }
                    },
                    enabled = isInitialized,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("New Package")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            statusMessage = "Force refreshing (re-clone)..."
                            val repoUrl = "https://github.com/snmrdatobgstudioz9918-creator/bountu-packages-global.git"
                            when (val result = gitManager.initialize(repoUrl, forceRefresh = true)) {
                                is GitResult.Success -> {
                                    isInitialized = true
                                    // Reload packages & info after re-clone
                                    when (val packagesResult = gitManager.listPackages()) {
                                        is GitResult.Success -> packages = packagesResult.data
                                        is GitResult.Error -> {}
                                    }
                                    when (val infoResult = gitManager.getRepositoryInfo()) {
                                        is GitResult.Success -> repositoryInfo = infoResult.data
                                        is GitResult.Error -> {}
                                    }
                                    statusMessage = "Re-cloned latest from remote"
                                }
                                is GitResult.Error -> {
                                    statusMessage = "Force refresh failed: ${result.message}"
                                }
                            }
                            isLoading = false
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Restore, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Force Refresh")
                }
            }


            // Loading Indicator
            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Packages List
            if (packages.isNotEmpty()) {
                Text(
                    text = "Available Packages (${packages.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(packages) { packageId ->
                        var meta by remember(packageId) { mutableStateOf<PackageMetadata?>(null) }
                        LaunchedEffect(packageId) {
                            when (val md = gitManager.getPackageMetadata(packageId)) {
                                is GitResult.Success -> meta = md.data
                                is GitResult.Error -> meta = null
                            }
                        }
                        val platform = meta?.platform?.ifBlank { "android" } ?: "?"
                        val arch = meta?.architecture?.ifBlank { "?" } ?: "?"
                        val missingUrl = meta?.downloadUrl.isNullOrBlank()
                        val missingSha = meta?.checksumSha256.isNullOrBlank()
                        val installable = (platform.equals("android", true) && !(missingUrl || missingSha))
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                scope.launch {
                                    when (val result = gitManager.getPackageMetadata(packageId)) {
                                        is GitResult.Success -> {
                                            selectedPackage = result.data
                                        }
                                        is GitResult.Error -> {
                                            statusMessage = "Error loading package: ${result.message}"
                                        }
                                    }
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(packageId, style = MaterialTheme.typography.titleSmall)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (platform != "?") com.chatxstudio.bountu.ui.components.Badge(platform.uppercase(), Color(0xFF1565C0))
                                        if (arch != "?") com.chatxstudio.bountu.ui.components.Badge(arch, Color(0xFF6A1B9A))
                                        if (meta != null) {
                                            when {
                                                installable -> com.chatxstudio.bountu.ui.components.Badge("Installable", Color(0xFF2E7D32))
                                                missingUrl || missingSha -> com.chatxstudio.bountu.ui.components.Badge("Missing data", Color(0xFFF9A825))
                                                else -> com.chatxstudio.bountu.ui.components.Badge("Unsupported", Color(0xFFC62828))
                                            }
                                        }
                                    }
                                }
                                Icon(Icons.Filled.Archive, contentDescription = null)
                            }
                        }
                    }
                }
            } else if (isInitialized) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "No packages found",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }

    // Inline details panel below list when a package is selected
    selectedPackage?.let { meta ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(meta.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Version: ${meta.version}", style = MaterialTheme.typography.bodyMedium)
                Text("Category: ${meta.category}", style = MaterialTheme.typography.bodyMedium)
                Text("Size: ${maxOf(1L, meta.size) / 1024 / 1024} MB", style = MaterialTheme.typography.bodyMedium)
                if (meta.dependencies.isNotEmpty()) {
                    Text("Dependencies: ${meta.dependencies.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium)
                }
                val missingUrl = meta.downloadUrl.isBlank()
                val missingChecksum = meta.checksumSha256.isBlank()
                val platform = meta.platform.ifBlank { "android" }
                if (missingUrl || missingChecksum || !platform.equals("android", true)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    if (!platform.equals("android", true)) {
                        Text("Unsupported platform: ${platform}", color = MaterialTheme.colorScheme.error)
                    }
                    if (missingUrl || missingChecksum) {
                        Text(
                            text = buildString {
                                append("Metadata issues: ")
                                if (missingUrl) append("downloadUrl ")
                                if (missingChecksum) append("checksumSha256 ")
                            }.trim(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(meta.description, style = MaterialTheme.typography.bodySmall)
                val canInstall = meta.downloadUrl.isNotBlank() && meta.checksumSha256.isNotBlank() && platform.equals("android", true)
                Button(onClick = {
                    scope.launch {
                        installing = true
                        installMessage = "Downloading..."
                        val result = pkgManager.installPackage(meta.id)
                        installing = false
                        installMessage = when (result) {
                            is InstallationResult.Success -> {
                                statusMessage = result.message
                                "Installed"
                            }
                            is InstallationResult.Failure -> {
                                statusMessage = "Install failed: ${result.error}"
                                "Failed: ${result.error}"
                            }
                            is InstallationResult.RequiresDependencies -> {
                                val deps = result.dependencies.joinToString(", ")
                                statusMessage = "Missing dependencies: $deps"
                                "Missing dependencies: $deps"
                            }
                            is InstallationResult.HasConflicts -> {
                                val cf = result.conflicts.joinToString(", ")
                                statusMessage = "Conflicts: $cf"
                                "Conflicts: $cf"
                            }
                        }
                    }
                }, enabled = !installing && canInstall) {
                    if (installing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(installMessage ?: "Installing...")
                    } else {
                        Text(if (canInstall) "Install" else "Not installable on this device")
                    }
                }
            }
        }
    }
}

@Composable
fun PackageCard(
    packageId: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Archive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = packageId,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PackageDetailsDialog(
    packageMetadata: PackageMetadata,
    installing: Boolean,
    installMessage: String?,
    onInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Filled.Info, contentDescription = null)
        },
        title = {
            Text(packageMetadata.name)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Version: ${packageMetadata.version}", style = MaterialTheme.typography.bodyMedium)
                Text("Category: ${packageMetadata.category}", style = MaterialTheme.typography.bodyMedium)
                Text("Size: ${maxOf(1L, packageMetadata.size) / 1024 / 1024} MB", style = MaterialTheme.typography.bodyMedium)

                if (packageMetadata.dependencies.isNotEmpty()) {
                    Text("Dependencies: ${packageMetadata.dependencies.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyMedium)
                }

                // Validation & compatibility
                val missingUrl = packageMetadata.downloadUrl.isBlank()
                val missingChecksum = packageMetadata.checksumSha256.isBlank()
                val platform = packageMetadata.platform.ifBlank { "android" }
                val arch = packageMetadata.architecture.ifBlank { System.getProperty("os.arch") ?: "unknown" }
                if (missingUrl || missingChecksum || !platform.equals("android", true)) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    if (!platform.equals("android", true)) {
                        Text("Unsupported platform: ${platform}", color = MaterialTheme.colorScheme.error)
                    }
                    if (missingUrl || missingChecksum) {
                        Text(
                            text = buildString {
                                append("Metadata issues: ")
                                if (missingUrl) append("downloadUrl ")
                                if (missingChecksum) append("checksumSha256 ")
                            }.trim(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Please add these fields in packages/${packageMetadata.id}/metadata.json.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text(packageMetadata.description, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = onDismiss) { Text("Close") }
                val canInstall = packageMetadata.downloadUrl.isNotBlank() && packageMetadata.checksumSha256.isNotBlank()
                Button(onClick = onInstall, enabled = !installing && canInstall) {
                    if (installing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(installMessage ?: "Installing...")
                    } else {
                        Text(if (canInstall) "Install" else "Fix metadata to install")
                    }
                }
            }
        }
    )
}
