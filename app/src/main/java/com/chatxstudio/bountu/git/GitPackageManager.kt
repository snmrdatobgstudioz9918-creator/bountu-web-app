package com.chatxstudio.bountu.git

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

/**
 * Git-based Package Manager for Bountu
 * Replaces Firebase with Git repositories for package distribution and configuration
 */
class GitPackageManager(private val context: Context) {

    companion object {
        private const val TAG = "GitPackageManager"
    private const val DEFAULT_REPO_URL = "https://github.com/snmrdatobgstudioz9918-creator/bountu-packages-global.git"
        private const val LOCAL_REPO_DIR = "bountu-repo"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    private val localRepoPath: File
        get() = File(context.filesDir, LOCAL_REPO_DIR)

    /**
     * Initialize the Git repository
     * Clones if not exists, otherwise validates
     * @param forceRefresh If true, deletes existing repo and re-clones
     */
    suspend fun initialize(repoUrl: String = DEFAULT_REPO_URL, forceRefresh: Boolean = false): GitResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            // Force refresh: delete and re-clone
            if (forceRefresh && localRepoPath.exists()) {
                Log.d(TAG, "Force refresh: deleting existing repository")
                localRepoPath.deleteRecursively()
            }
            
            if (!localRepoPath.exists()) {
                Log.d(TAG, "Repository not found, cloning from $repoUrl")
                cloneRepository(repoUrl)
            } else {
                Log.d(TAG, "Repository exists at ${localRepoPath.absolutePath}")
                // Verify it's a valid git repo
                if (!File(localRepoPath, ".git").exists()) {
                    Log.w(TAG, "Invalid repository, re-cloning")
                    localRepoPath.deleteRecursively()
                    cloneRepository(repoUrl)
                } else {
                    GitResult.Success(true)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize repository", e)
            GitResult.Error("Initialization failed: ${e.message}")
        }
    }

    /**
     * Clone a Git repository using JGit
     */
    private suspend fun cloneRepository(repoUrl: String): GitResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Cloning repository from $repoUrl to ${localRepoPath.absolutePath}")
            
            // Ensure parent directory exists
            localRepoPath.parentFile?.mkdirs()
            
            // Clone using JGit
            Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(localRepoPath)
                .setCloneAllBranches(false)
                .setBranch("main") // or "master"
                .setProgressMonitor(object : ProgressMonitor {
                    override fun start(totalTasks: Int) {
                        Log.d(TAG, "Clone started: $totalTasks tasks")
                    }
                    override fun beginTask(title: String?, totalWork: Int) {
                        Log.d(TAG, "Task: $title ($totalWork)")
                    }
                    override fun update(completed: Int) {}
                    override fun endTask() {}
                    override fun isCancelled(): Boolean = false
                    override fun showDuration(enabled: Boolean) {}
                })
                .call()
                .close()
            
            Log.d(TAG, "Repository cloned successfully")
            GitResult.Success(true)
        } catch (e: GitAPIException) {
            Log.e(TAG, "Git API exception during clone", e)
            GitResult.Error("Clone failed: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Clone exception", e)
            GitResult.Error("Clone exception: ${e.message}")
        }
    }

    /**
     * Sync repository (fetch and pull latest changes) using JGit
     */
    suspend fun syncRepository(): GitResult<SyncStatus> = withContext(Dispatchers.IO) {
        try {
            if (!isRepositoryInitialized()) {
                return@withContext GitResult.Error("Repository not initialized")
            }

            // Get current commit hash
            val beforeCommit = getCurrentCommit()

            // Open repository and pull
            Git.open(localRepoPath).use { git ->
                Log.d(TAG, "Fetching latest changes...")
                git.fetch()
                    .setProgressMonitor(object : ProgressMonitor {
                        override fun start(totalTasks: Int) {}
                        override fun beginTask(title: String?, totalWork: Int) {}
                        override fun update(completed: Int) {}
                        override fun endTask() {}
                        override fun isCancelled(): Boolean = false
                        override fun showDuration(enabled: Boolean) {}
                    })
                    .call()
                
                Log.d(TAG, "Pulling changes...")
                git.pull()
                    .setProgressMonitor(object : ProgressMonitor {
                        override fun start(totalTasks: Int) {}
                        override fun beginTask(title: String?, totalWork: Int) {}
                        override fun update(completed: Int) {}
                        override fun endTask() {}
                        override fun isCancelled(): Boolean = false
                        override fun showDuration(enabled: Boolean) {}
                    })
                    .call()
            }

            // Get new commit hash
            val afterCommit = getCurrentCommit()

            val hasUpdates = beforeCommit != afterCommit
            Log.d(TAG, "Sync complete. Updates: $hasUpdates")

            GitResult.Success(SyncStatus(
                hasUpdates = hasUpdates,
                beforeCommit = beforeCommit ?: "",
                afterCommit = afterCommit ?: "",
                message = if (hasUpdates) "Repository updated" else "Already up to date"
            ))
        } catch (e: GitAPIException) {
            Log.e(TAG, "Git API exception during sync", e)
            GitResult.Error("Sync failed: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            GitResult.Error("Sync failed: ${e.message}")
        }
    }

    /**
     * Get current Git commit hash using JGit
     */
    private suspend fun getCurrentCommit(): String? = withContext(Dispatchers.IO) {
        try {
            Git.open(localRepoPath).use { git ->
                val head = git.repository.resolve("HEAD")
                head?.name
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get current commit", e)
            null
        }
    }

    /**
     * Check if repository is initialized
     */
    fun isRepositoryInitialized(): Boolean {
        return localRepoPath.exists() && File(localRepoPath, ".git").exists()
    }

    /**
     * Get maintenance status from Git repository
     */
    suspend fun getMaintenanceStatus(): GitResult<MaintenanceStatus> = withContext(Dispatchers.IO) {
        try {
            val configFile = File(localRepoPath, "config/maintenance.json")

            if (!configFile.exists()) {
                Log.w(TAG, "Maintenance config not found, using defaults")
                return@withContext GitResult.Success(MaintenanceStatus())
            }

            val jsonContent = configFile.readText()
            val status = json.decodeFromString<MaintenanceStatus>(jsonContent)

            Log.d(TAG, "Maintenance status loaded: $status")
            GitResult.Success(status)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load maintenance status", e)
            GitResult.Error("Failed to load maintenance status: ${e.message}")
        }
    }

    /**
     * Get app configuration from Git repository
     */
    suspend fun getAppConfig(): GitResult<AppConfig> = withContext(Dispatchers.IO) {
        try {
            val configFile = File(localRepoPath, "config/app_config.json")

            if (!configFile.exists()) {
                Log.w(TAG, "App config not found, using defaults")
                return@withContext GitResult.Success(AppConfig())
            }

            val jsonContent = configFile.readText()
            val config = json.decodeFromString<AppConfig>(jsonContent)

            Log.d(TAG, "App config loaded: $config")
            GitResult.Success(config)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load app config", e)
            GitResult.Error("Failed to load app config: ${e.message}")
        }
    }

    /**
     * Get package metadata from repository
     */
    suspend fun getPackageMetadata(packageId: String): GitResult<PackageMetadata> = withContext(Dispatchers.IO) {
        try {
            val metadataFile = File(localRepoPath, "packages/$packageId/metadata.json")

            if (!metadataFile.exists()) {
                return@withContext GitResult.Error("Package metadata not found: $packageId")
            }

            var jsonContent = metadataFile.readText()
            // Strip UTF-8 BOM if present and trim whitespace
            if (jsonContent.isNotEmpty() && jsonContent[0] == '\uFEFF') {
                jsonContent = jsonContent.substring(1)
            }
            jsonContent = jsonContent.trim()

            if (jsonContent.isEmpty()) {
                return@withContext GitResult.Error("Empty metadata.json for package: $packageId")
            }

            // Sanitize accidentally escaped JSON (e.g., contains \" and is stored as a string literal)
            var normalized = jsonContent
            if (normalized.contains("\\\"")) {
                // Remove wrapping quotes if present
                if (normalized.length >= 2 && normalized.first() == '"' && normalized.last() == '"') {
                    normalized = normalized.substring(1, normalized.length - 1)
                }
                normalized = normalized
                    .replace("\\\"", "\"")    // unescape quotes
                    .replace("\\/", "/")        // unescape forward slashes
            }

            val metadata = json.decodeFromString<PackageMetadata>(normalized)

            GitResult.Success(metadata)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load package metadata", e)
            GitResult.Error("Failed to load package metadata: ${e.message}")
        }
    }

    /**
     * List all available packages
     */
    suspend fun listPackages(): GitResult<List<String>> = withContext(Dispatchers.IO) {
        try {
            val packagesDir = File(localRepoPath, "packages")

            if (!packagesDir.exists() || !packagesDir.isDirectory) {
                return@withContext GitResult.Success(emptyList())
            }

            val packages = packagesDir.listFiles()
                ?.filter { it.isDirectory }
                ?.map { it.name }
                ?: emptyList()

            GitResult.Success(packages)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to list packages", e)
            GitResult.Error("Failed to list packages: ${e.message}")
        }
    }

    /** Create or update a custom package under packages/<id> and commit */
    suspend fun createCustomPackage(
        id: String,
        name: String,
        version: String,
        description: String,
        platform: String = "android",
        architecture: String = "aarch64",
        downloadUrl: String = "",
        checksumSha256: String = ""
    ): GitResult<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!isRepositoryInitialized()) return@withContext GitResult.Error("Repository not initialized")
            val pkgDir = File(localRepoPath, "packages/$id")
            pkgDir.mkdirs()
            val metadata = PackageMetadata(
                id = id,
                name = name,
                version = version,
                description = description,
                category = "custom",
                size = 0,
                dependencies = emptyList(),
                downloadUrl = downloadUrl,
                checksumSha256 = checksumSha256,
                platform = platform,
                architecture = architecture
            )
            val metaFile = File(pkgDir, "metadata.json")
            metaFile.writeText(json.encodeToString(PackageMetadata.serializer(), metadata))

            // Commit the change
            Git.open(localRepoPath).use { git ->
                git.add().addFilepattern("packages/$id/metadata.json").call()
                git.commit().setMessage("Add/Update custom package: $id $version").call()
            }
            GitResult.Success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create custom package", e)
            GitResult.Error("Failed to create custom package: ${e.message}")
        }
    }




    /**
     * Get repository information using JGit
     */
    suspend fun getRepositoryInfo(): GitResult<RepositoryInfo> = withContext(Dispatchers.IO) {
        try {
            if (!isRepositoryInitialized()) {
                return@withContext GitResult.Error("Repository not initialized")
            }

            val commit = getCurrentCommit()
            
            val remoteUrl = Git.open(localRepoPath).use { git ->
                git.repository.config.getString("remote", "origin", "url") ?: "unknown"
            }

            val lastUpdate = File(localRepoPath, ".git/FETCH_HEAD").let {
                if (it.exists()) it.lastModified() else 0L
            }

            GitResult.Success(RepositoryInfo(
                localPath = localRepoPath.absolutePath,
                remoteUrl = remoteUrl,
                currentCommit = commit ?: "unknown",
                lastUpdate = lastUpdate,
                isInitialized = true
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get repository info", e)
            GitResult.Error("Failed to get repository info: ${e.message}")
        }
    }
}

/**
 * Git operation result
 */
sealed class GitResult<out T> {
    data class Success<T>(val data: T) : GitResult<T>()
    data class Error(val message: String) : GitResult<Nothing>()
}



/**
 * Sync status
 */
@Serializable
data class SyncStatus(
    val hasUpdates: Boolean,
    val beforeCommit: String,
    val afterCommit: String,
    val message: String
)

/**
 * Maintenance status (from Git repo)
 */
@Serializable
data class MaintenanceStatus(
    val isEnabled: Boolean = false,
    val title: String = "Maintenance Mode",
    val message: String = "The app is currently under maintenance. Please try again later.",
    val estimatedTime: String = "Unknown",
    val allowedVersions: List<String> = emptyList()
)

/**
 * App configuration (from Git repo)
 */
@Serializable
data class AppConfig(
    val minVersion: String = "1.0",
    val latestVersion: String = "1.0",
    val forceUpdate: Boolean = false,
    val updateMessage: String = "A new version is available. Please update.",
    val enabledFeatures: List<String> = emptyList()
)

/**
 * Package metadata
 */
@Serializable
data class PackageMetadata(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val category: String,
    val size: Long,
    val dependencies: List<String> = emptyList(),
    val downloadUrl: String = "",
    val checksumSha256: String = "",
    // Optional fields present in repo (ignored earlier)
    val platform: String = "",           // e.g., "android", "windows", "both"
    val architecture: String = ""         // e.g., "aarch64", "arm", "x86_64"
)

/**
 * Repository information
 */
data class RepositoryInfo(
    val localPath: String,
    val remoteUrl: String,
    val currentCommit: String,
    val lastUpdate: Long,
    val isInitialized: Boolean
)
