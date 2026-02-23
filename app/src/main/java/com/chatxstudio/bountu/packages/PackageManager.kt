package com.chatxstudio.bountu.packages

import android.content.Context
import android.util.Log
import com.chatxstudio.bountu.git.GitPackageManager
import com.chatxstudio.bountu.git.GitResult
import com.chatxstudio.bountu.git.PackageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Package Manager for Bountu
 * Handles package installation, removal, updates, and queries
 * Similar to APT (Advanced Package Tool) from Ubuntu/Debian
 */
class PackageManager(private val context: Context) {
    
    companion object {
        private const val TAG = "PackageManager"
        private const val PACKAGES_DIR = "packages"
        private const val INSTALLED_PACKAGES_FILE = "installed_packages.json"
        private const val USE_REAL_INSTALLATION = true // Toggle real vs mock installation
    }
    
    private val packageInstaller = PackageInstaller(context)
    private val gitManager = GitPackageManager(context)
    
    // State flows for reactive UI
    private val _installedPackages = MutableStateFlow<List<Package>>(emptyList())
    val installedPackages: StateFlow<List<Package>> = _installedPackages
    
    private val _availablePackages = MutableStateFlow<List<Package>>(emptyList())
    val availablePackages: StateFlow<List<Package>> = _availablePackages
    
    private val _installationProgress = MutableStateFlow<Pair<String, Float>?>(null)
    val installationProgress: StateFlow<Pair<String, Float>?> = _installationProgress
    
    private val _isLoadingPackages = MutableStateFlow(false)
    val isLoadingPackages: StateFlow<Boolean> = _isLoadingPackages
    
    private val packagesDir: File by lazy {
        File(context.filesDir, PACKAGES_DIR).apply { mkdirs() }
    }
    
    init {
        loadPackages()
    }
    
    /**
     * Load all packages from Git repository ONLY
     * No mock packages - all packages must come from GitHub
     */
    private fun loadPackages() {
        // Start with empty list - will ONLY be populated by Git sync
        // NO MOCK PACKAGES - all packages come from GitHub repository
        _availablePackages.value = emptyList()
        
        // Load installed packages from storage
        val installed = loadInstalledPackages()
        _installedPackages.value = installed
        
        Log.d(TAG, "Package manager initialized - waiting for Git sync")
        Log.d(TAG, "All packages will be fetched from: https://github.com/snmrdatobgstudioz9918-creator/bountu-packages-global")
    }
    
    /**
     * Sync packages from Git repository
     * @param forceRefresh If true, deletes local repo and re-clones
     */
    suspend fun syncPackagesFromGit(forceRefresh: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        try {
            _isLoadingPackages.value = true
            Log.d(TAG, "Syncing packages from Git repository... (forceRefresh=$forceRefresh)")
            
            // Initialize Git repository if not already done
            if (!gitManager.isRepositoryInitialized() || forceRefresh) {
                when (val result = gitManager.initialize(forceRefresh = forceRefresh)) {
                    is GitResult.Success -> Log.d(TAG, "Git repository initialized")
                    is GitResult.Error -> {
                        Log.w(TAG, "Git init failed: ${result.message}")
                        _isLoadingPackages.value = false
                        return@withContext false
                    }
                }
            } else {
                // Sync to get latest changes
                when (val syncResult = gitManager.syncRepository()) {
                    is GitResult.Success -> {
                        Log.d(TAG, "Git sync: ${syncResult.data.message}")
                    }
                    is GitResult.Error -> {
                        Log.w(TAG, "Git sync failed: ${syncResult.message}")
                    }
                }
            }
            
            // List all packages from Git
            when (val packagesResult = gitManager.listPackages()) {
                is GitResult.Success -> {
                    val packageIds = packagesResult.data
                    Log.d(TAG, "Found ${packageIds.size} packages in Git repository")
                    
                    // Load metadata for each package
                    val gitPackages = mutableListOf<Package>()
                    for (packageId in packageIds) {
                        when (val metadataResult = gitManager.getPackageMetadata(packageId)) {
                            is GitResult.Success -> {
                                val metadata = metadataResult.data
                                gitPackages.add(convertMetadataToPackage(metadata))
                            }
                            is GitResult.Error -> {
                                Log.w(TAG, "Failed to load metadata for $packageId: ${metadataResult.message}")
                            }
                        }
                    }
                    
                    // Use ONLY Git packages - no merging with mock packages
                    _availablePackages.value = gitPackages
                    updatePackageStates()
                    
                    Log.d(TAG, "Successfully loaded ${gitPackages.size} packages from Git")
                    _isLoadingPackages.value = false
                    return@withContext true
                }
                is GitResult.Error -> {
                    Log.e(TAG, "Failed to list packages: ${packagesResult.message}")
                    _isLoadingPackages.value = false
                    return@withContext false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing packages from Git", e)
            _isLoadingPackages.value = false
            return@withContext false
        }
    }
    
    /**
     * Convert Git PackageMetadata to Package
     */
    private fun convertMetadataToPackage(metadata: PackageMetadata): Package {
        val arch = System.getProperty("os.arch")?.lowercase() ?: "arm64"
        val isAndroid = true // app runs on Android
        val supportedPlatform = metadata.platform.isBlank() || metadata.platform.equals("android", true) || metadata.platform.equals("both", true)
        val supportedArch = metadata.architecture.isBlank() ||
            (arch.contains("aarch64") || arch.contains("arm64") && metadata.architecture.equals("aarch64", true)) ||
            (arch.contains("arm") && metadata.architecture.equals("arm", true)) ||
            (arch.contains("x86_64") && metadata.architecture.equals("x86_64", true)) ||
            (arch.contains("x86") && metadata.architecture.equals("x86", true))

        val canInstall = isAndroid && supportedPlatform && supportedArch && metadata.downloadUrl.isNotBlank()
        val reason = when {
            !supportedPlatform -> "Unsupported platform: ${metadata.platform}"
            !supportedArch -> "Unsupported arch: ${metadata.architecture}"
            metadata.downloadUrl.isBlank() -> "Missing downloadUrl"
            else -> ""
        }

        return Package(
            id = metadata.id,
            name = metadata.name,
            version = metadata.version,
            description = metadata.description,
            longDescription = metadata.description,
            category = when (metadata.category.lowercase()) {
                "utilities" -> PackageCategory.UTILITIES
                "development" -> PackageCategory.DEVELOPMENT
                "network", "networking" -> PackageCategory.NETWORK
                "system" -> PackageCategory.SYSTEM
                "multimedia" -> PackageCategory.MULTIMEDIA
                "security" -> PackageCategory.SECURITY
                "editors" -> PackageCategory.EDITORS
                "shells" -> PackageCategory.SHELLS
                "compression" -> PackageCategory.COMPRESSION
                "database" -> PackageCategory.DATABASE
                "web" -> PackageCategory.WEB
                "programming" -> PackageCategory.PROGRAMMING
                "version_control" -> PackageCategory.VERSION_CONTROL
                "documentation" -> PackageCategory.DOCUMENTATION
                "libraries" -> PackageCategory.LIBRARIES
                "games" -> PackageCategory.GAMES
                "education" -> PackageCategory.EDUCATION
                "science" -> PackageCategory.SCIENCE
                else -> PackageCategory.UTILITIES
            },
            platform = if (supportedPlatform) Platform.ANDROID else Platform.WINDOWS, // mark unsupported as Windows for visibility
            size = metadata.size,
            binaryUrl = metadata.downloadUrl,
            checksumSHA256 = metadata.checksumSha256,
            dependencies = metadata.dependencies,
            tags = listOf(metadata.category),
            installScript = "",
            uninstallScript = "",
            isInstalled = false,
            needsUpdate = false,
            needsMaintenance = !canInstall,
            maintenanceReason = reason
        )
    }
    
    /**
     * Search packages by query and filters
     */
    fun searchPackages(filter: PackageFilter): List<Package> {
        var packages = _availablePackages.value
        
        // Apply query filter
        if (filter.query.isNotEmpty()) {
            packages = packages.filter { pkg ->
                pkg.name.contains(filter.query, ignoreCase = true) ||
                pkg.description.contains(filter.query, ignoreCase = true) ||
                pkg.longDescription.contains(filter.query, ignoreCase = true) ||
                pkg.tags.any { it.contains(filter.query, ignoreCase = true) }
            }
        }
        
        // Apply category filter
        if (filter.category != null) {
            packages = packages.filter { it.category == filter.category }
        }
        
        // Apply platform filter
        if (filter.platform != null) {
            packages = packages.filter { 
                it.platform == filter.platform || it.platform == Platform.BOTH 
            }
        }
        
        // Apply installed filter
        if (filter.installedOnly) {
            packages = packages.filter { it.isInstalled }
        }
        
        // Apply updates filter
        if (filter.updatesOnly) {
            packages = packages.filter { it.needsUpdate }
        }
        
        // Apply maintenance filter
        if (filter.maintenanceOnly) {
            packages = packages.filter { it.needsMaintenance }
        }
        
        return packages
    }
    
    /**
     * Get package by ID
     */
    fun getPackage(packageId: String): Package? {
        return _availablePackages.value.find { it.id == packageId }
    }
    
    /**
     * Install a package
     */
    suspend fun installPackage(packageId: String): InstallationResult = withContext(Dispatchers.IO) {
        try {
            val pkg = getPackage(packageId) 
                ?: return@withContext InstallationResult.Failure("Package not found")
            
            Log.d(TAG, "Installing package: ${pkg.name}")
            
            // Check if already installed
            if (pkg.isInstalled) {
                return@withContext InstallationResult.Failure("Package already installed")
            }
            
            // Check dependencies
            val missingDeps = checkDependencies(pkg)
            if (missingDeps.isNotEmpty()) {
                return@withContext InstallationResult.RequiresDependencies(missingDeps)
            }
            
            // Check conflicts
            val conflicts = checkConflicts(pkg)
            if (conflicts.isNotEmpty()) {
                return@withContext InstallationResult.HasConflicts(conflicts)
            }
            
            // Real or simulated installation
            if (USE_REAL_INSTALLATION) {
                // Real installation using PackageInstaller
                val result = packageInstaller.installPackage(pkg) { progress ->
                    _installationProgress.value = Pair(pkg.name, progress)
                }
                
                if (result is InstallationResult.Failure) {
                    _installationProgress.value = null
                    return@withContext result
                }
            } else {
                // Simulate installation process (for testing)
                _installationProgress.value = Pair(pkg.name, 0f)
                
                // Download phase
                for (i in 0..30) {
                    _installationProgress.value = Pair(pkg.name, i / 100f)
                    kotlinx.coroutines.delay(50)
                }
                
                // Extract phase
                for (i in 31..60) {
                    _installationProgress.value = Pair(pkg.name, i / 100f)
                    kotlinx.coroutines.delay(30)
                }
                
                // Configure phase
                for (i in 61..90) {
                    _installationProgress.value = Pair(pkg.name, i / 100f)
                    kotlinx.coroutines.delay(20)
                }
                
                // Finalize
                for (i in 91..100) {
                    _installationProgress.value = Pair(pkg.name, i / 100f)
                    kotlinx.coroutines.delay(10)
                }
            }
            
            // Mark as installed
            val installedPkg = pkg.copy(
                isInstalled = true,
                installedVersion = pkg.version
            )
            
            saveInstalledPackage(installedPkg)
            updatePackageStates()
            
            _installationProgress.value = null
            
            Log.d(TAG, "Package installed successfully: ${pkg.name}")
            InstallationResult.Success("Package ${pkg.name} installed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Installation failed", e)
            _installationProgress.value = null
            InstallationResult.Failure("Installation failed: ${e.message}")
        }
    }
    
    /**
     * Uninstall a package
     */
    suspend fun uninstallPackage(packageId: String): InstallationResult = withContext(Dispatchers.IO) {
        try {
            val pkg = getPackage(packageId) 
                ?: return@withContext InstallationResult.Failure("Package not found")
            
            Log.d(TAG, "Uninstalling package: ${pkg.name}")
            
            if (!pkg.isInstalled) {
                return@withContext InstallationResult.Failure("Package not installed")
            }
            
            // Real or simulated uninstallation
            if (USE_REAL_INSTALLATION) {
                // Real uninstallation
                val result = packageInstaller.uninstallPackage(pkg)
                if (result is InstallationResult.Failure) {
                    _installationProgress.value = null
                    return@withContext result
                }
            } else {
                // Simulate uninstallation
                _installationProgress.value = Pair(pkg.name, 0f)
                
                for (i in 0..100 step 10) {
                    _installationProgress.value = Pair(pkg.name, i / 100f)
                    kotlinx.coroutines.delay(50)
                }
            }
            
            // Remove from installed packages
            removeInstalledPackage(packageId)
            updatePackageStates()
            
            _installationProgress.value = null
            
            Log.d(TAG, "Package uninstalled successfully: ${pkg.name}")
            InstallationResult.Success("Package ${pkg.name} uninstalled successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Uninstallation failed", e)
            _installationProgress.value = null
            InstallationResult.Failure("Uninstallation failed: ${e.message}")
        }
    }
    
    /**
     * Update a package
     */
    suspend fun updatePackage(packageId: String): InstallationResult = withContext(Dispatchers.IO) {
        try {
            val pkg = getPackage(packageId) 
                ?: return@withContext InstallationResult.Failure("Package not found")
            
            Log.d(TAG, "Updating package: ${pkg.name}")
            
            if (!pkg.isInstalled) {
                return@withContext InstallationResult.Failure("Package not installed")
            }
            
            if (!pkg.needsUpdate) {
                return@withContext InstallationResult.Failure("Package is up to date")
            }
            
            // Simulate update process
            _installationProgress.value = Pair(pkg.name, 0f)
            
            for (i in 0..100 step 5) {
                _installationProgress.value = Pair(pkg.name, i / 100f)
                kotlinx.coroutines.delay(30)
            }
            
            // Update package
            val updatedPkg = pkg.copy(
                installedVersion = pkg.version,
                needsUpdate = false,
                needsMaintenance = false,
                maintenanceReason = ""
            )
            
            saveInstalledPackage(updatedPkg)
            updatePackageStates()
            
            _installationProgress.value = null
            
            Log.d(TAG, "Package updated successfully: ${pkg.name}")
            InstallationResult.Success("Package ${pkg.name} updated to version ${pkg.version}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Update failed", e)
            _installationProgress.value = null
            InstallationResult.Failure("Update failed: ${e.message}")
        }
    }
    
    /**
     * Fix maintenance issues for a package
     */
    suspend fun fixMaintenance(packageId: String): InstallationResult {
        return updatePackage(packageId)
    }
    
    /**
     * Update all packages
     */
    suspend fun updateAllPackages(): List<InstallationResult> {
        val packagesNeedingUpdate = _availablePackages.value.filter { it.needsUpdate }
        val results = mutableListOf<InstallationResult>()
        
        for (pkg in packagesNeedingUpdate) {
            results.add(updatePackage(pkg.id))
        }
        
        return results
    }
    
    /**
     * Get statistics about packages
     */
    fun getPackageStats(): PackageStats {
        val all = _availablePackages.value
        val installed = all.filter { it.isInstalled }
        val needsUpdate = all.filter { it.needsUpdate }
        val needsMaintenance = all.filter { it.needsMaintenance }
        
        return PackageStats(
            totalPackages = all.size,
            installedPackages = installed.size,
            availableUpdates = needsUpdate.size,
            needsMaintenance = needsMaintenance.size,
            totalSize = installed.sumOf { it.size }
        )
    }
    
    /**
     * Get packages by category
     */
    fun getPackagesByCategory(category: PackageCategory): List<Package> {
        return _availablePackages.value.filter { it.category == category }
    }
    
    /**
     * Check package dependencies
     */
    private fun checkDependencies(pkg: Package): List<String> {
        val installed = _installedPackages.value.map { it.id }
        return pkg.dependencies.filter { it !in installed }
    }
    
    /**
     * Check package conflicts
     */
    private fun checkConflicts(pkg: Package): List<String> {
        val installed = _installedPackages.value.map { it.id }
        return pkg.conflicts.filter { it in installed }
    }
    
    /**
     * Load installed packages from storage
     */
    private fun loadInstalledPackages(): List<Package> {
        // In a real implementation, this would load from a JSON file or database
        // For now, return packages marked as installed in the repository
        return PackageRepository.getAllPackages().filter { it.isInstalled }
    }
    
    /**
     * Save installed package
     */
    private fun saveInstalledPackage(pkg: Package) {
        val current = _installedPackages.value.toMutableList()
        val index = current.indexOfFirst { it.id == pkg.id }
        
        if (index >= 0) {
            current[index] = pkg
        } else {
            current.add(pkg)
        }
        
        _installedPackages.value = current
    }
    
    /**
     * Remove installed package
     */
    private fun removeInstalledPackage(packageId: String) {
        val current = _installedPackages.value.toMutableList()
        current.removeAll { it.id == packageId }
        _installedPackages.value = current
    }
    
    /**
     * Update package states based on installed packages
     */
    private fun updatePackageStates() {
        val installed = _installedPackages.value
        val installedIds = installed.map { it.id }.toSet()
        
        val updated = _availablePackages.value.map { pkg ->
            if (pkg.id in installedIds) {
                val installedPkg = installed.find { it.id == pkg.id }
                pkg.copy(
                    isInstalled = true,
                    installedVersion = installedPkg?.installedVersion,
                    needsUpdate = installedPkg?.needsUpdate ?: false,
                    needsMaintenance = installedPkg?.needsMaintenance ?: false,
                    maintenanceReason = installedPkg?.maintenanceReason ?: ""
                )
            } else {
                pkg.copy(
                    isInstalled = false,
                    installedVersion = null,
                    needsUpdate = false,
                    needsMaintenance = false,
                    maintenanceReason = ""
                )
            }
        }
        
        _availablePackages.value = updated
    }
}

/**
 * Package statistics
 */
data class PackageStats(
    val totalPackages: Int,
    val installedPackages: Int,
    val availableUpdates: Int,
    val needsMaintenance: Int,
    val totalSize: Long
)
