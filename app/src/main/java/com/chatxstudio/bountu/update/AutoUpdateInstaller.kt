package com.chatxstudio.bountu.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

/**
 * Auto Update Installer
 * Automatically installs app updates when APK is downloaded
 */
class AutoUpdateInstaller(private val context: Context) {
    
    companion object {
        private const val TAG = "AutoUpdateInstaller"
        private const val UPDATE_DIR = "updates"
        private const val AUTHORITY = "com.chatxstudio.bountu.fileprovider"
    }
    
    private val updateDir: File by lazy {
        File(context.filesDir, UPDATE_DIR).apply { mkdirs() }
    }
    
    private val _updateStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus
    
    /**
     * Check for pending updates and install them
     */
    suspend fun checkAndInstallPendingUpdates(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking for pending updates...")
            
            // Check if app was just updated
            val prefs = context.getSharedPreferences("app_updates", Context.MODE_PRIVATE)
            val lastVersion = prefs.getInt("last_version", 0)
            val currentVersion = getCurrentVersionCode()
            
            if (currentVersion > lastVersion) {
                Log.d(TAG, "App was updated from version $lastVersion to $currentVersion")
                prefs.edit().putInt("last_version", currentVersion).apply()
                
                // Clear app cache to force reload
                clearAppCache()
                
                // Trigger full app restart
                triggerAppRestart()
                
                return@withContext true
            }
            
            val updateFiles = updateDir.listFiles { file ->
                file.extension == "apk"
            }
            
            if (updateFiles.isNullOrEmpty()) {
                Log.d(TAG, "No pending updates found")
                return@withContext false
            }
            
            // Get the latest update file
            val latestUpdate = updateFiles.maxByOrNull { it.lastModified() }
                ?: return@withContext false
            
            Log.d(TAG, "Found update: ${latestUpdate.name}")
            
            // Install the update
            installUpdate(latestUpdate)
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates", e)
            _updateStatus.value = UpdateStatus.Error(e.message ?: "Unknown error")
            false
        }
    }
    
    /**
     * Install update APK
     */
    suspend fun installUpdate(apkFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            _updateStatus.value = UpdateStatus.Installing
            
            Log.d(TAG, "Installing update: ${apkFile.absolutePath}")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // Use FileProvider for Android N and above
                val apkUri = FileProvider.getUriForFile(
                    context,
                    AUTHORITY,
                    apkFile
                )
                
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                
                context.startActivity(intent)
            } else {
                // Legacy method for older Android versions
                val apkUri = Uri.fromFile(apkFile)
                
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(apkUri, "application/vnd.android.package-archive")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                
                context.startActivity(intent)
            }
            
            _updateStatus.value = UpdateStatus.Success
            
            // Clean up old update files after installation
            cleanupOldUpdates(apkFile)
            
            Log.d(TAG, "Update installation initiated")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install update", e)
            _updateStatus.value = UpdateStatus.Error(e.message ?: "Installation failed")
            false
        }
    }
    
    /**
     * Download update from URL
     */
    suspend fun downloadUpdate(url: String): File? = withContext(Dispatchers.IO) {
        try {
            _updateStatus.value = UpdateStatus.Downloading(0)
            
            Log.d(TAG, "Downloading update from: $url")
            
            val fileName = "bountu-update-${System.currentTimeMillis()}.apk"
            val outputFile = File(updateDir, fileName)
            
            // Download logic would go here
            // For now, this is a placeholder
            
            _updateStatus.value = UpdateStatus.Downloaded
            
            Log.d(TAG, "Update downloaded: ${outputFile.absolutePath}")
            outputFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download update", e)
            _updateStatus.value = UpdateStatus.Error(e.message ?: "Download failed")
            null
        }
    }
    
    data class GithubLatestResult(val latest: String?, val status: Int, val rateLimited: Boolean)


    /**
     * Check GitHub latest release tag for desktop/mobile app updates.
     * Returns status and tag (e.g., "v1.2.3"). rateLimited=true when API returns 403 with X-RateLimit-Remaining=0 or 429.
     */
    suspend fun checkGithubLatest(owner: String, repo: String): GithubLatestResult = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL("https://api.github.com/repos/$owner/$repo/releases/latest")
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Bountu-Android")
                connectTimeout = 7000
                readTimeout = 7000
            }
            val status = conn.responseCode
            val remaining = conn.getHeaderField("X-RateLimit-Remaining")
            val rateLimited = (status == 403 && remaining == "0") || status == 429
            val latest = if (status in 200..299) conn.inputStream.bufferedReader().use { r ->
                val text = r.readText()
                val obj = org.json.JSONObject(text)
                obj.optString("tag_name", null)
            } else null
            GithubLatestResult(latest = latest, status = status, rateLimited = rateLimited)
        } catch (e: Exception) {
            Log.w(TAG, "GitHub latest check failed: ${e.message}")
            GithubLatestResult(latest = null, status = -1, rateLimited = false)
        }
    }

    /**
     * Download latest APK asset from GitHub Releases (latest) and save into updates/.
     * Returns the downloaded File or null.
     */
    suspend fun downloadLatestApkFromGithub(owner: String, repo: String): File? = withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL("https://api.github.com/repos/$owner/$repo/releases/latest")
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Bountu-Android")
                connectTimeout = 10000
                readTimeout = 10000
            }
            if (conn.responseCode !in 200..299) return@withContext null
            val json = conn.inputStream.bufferedReader().use { it.readText() }
            val obj = org.json.JSONObject(json)
            val assets = obj.optJSONArray("assets") ?: return@withContext null
            var downloadUrl: String? = null
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.optString("name", "")
                val ctype = a.optString("content_type", "")
                val urlCandidate = a.optString("browser_download_url", null)
                if (name.endsWith(".apk", ignoreCase = true) || ctype.contains("android.package-archive")) {
                    downloadUrl = urlCandidate
                    break
                }
            }
            if (downloadUrl == null) return@withContext null

            val outFile = File(updateDir, "bountu-update-${System.currentTimeMillis()}.apk")
            val dconn = (java.net.URL(downloadUrl).openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Bountu-Android")
                connectTimeout = 20000
                readTimeout = 20000
            }
            dconn.inputStream.use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            outFile
        } catch (e: Exception) {
            Log.w(TAG, "GitHub APK download failed: ${e.message}")
            null
        }
    }

    /** Simple semver-ish compare: returns -1 if a<b, 0 if equal, 1 if a>b */
    fun compareVersions(a: String?, b: String?): Int {

        return try {
            val va = (a ?: "0").replace(Regex("[^0-9.]"), "")
            val vb = (b ?: "0").replace(Regex("[^0-9.]"), "")
            val pa = va.split('.').map { it.toIntOrNull() ?: 0 }
            val pb = vb.split('.').map { it.toIntOrNull() ?: 0 }
            val max = maxOf(pa.size, pb.size)
            for (i in 0 until max) {
                val ai = if (i < pa.size) pa[i] else 0
                val bi = if (i < pb.size) pb[i] else 0
                if (ai < bi) return -1
                if (ai > bi) return 1
            }
            0
        } catch (_: Exception) { 0 }
    }


    
    /**
     * Get current app version
     */
    fun getCurrentVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get current version", e)
            "1.0"
        }
    }
    
    /**
     * Clean up old update files
     */
    private fun cleanupOldUpdates(keepFile: File? = null) {
        try {
            updateDir.listFiles()?.forEach { file ->
                if (file != keepFile && file.extension == "apk") {
                    file.delete()
                    Log.d(TAG, "Deleted old update: ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old updates", e)
        }
    }
    
    /**
     * Check if app has REQUEST_INSTALL_PACKAGES permission
     */
    fun hasInstallPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }
    
    /**
     * Request install permission
     */
    fun requestInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
    
    /**
     * Get current app version code
     */
    private fun getCurrentVersionCode(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionCode
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get version code", e)
            0
        }
    }
    
    /**
     * Clear app cache to force reload
     */
    private fun clearAppCache() {
        try {
            val cacheDir = context.cacheDir
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            
            // Clear internal cache
            context.filesDir.listFiles()?.forEach { file ->
                if (file.name.endsWith(".cache") || file.name.endsWith(".tmp")) {
                    file.delete()
                }
            }
            
            Log.d(TAG, "App cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
        }
    }
    
    /**
     * Trigger full app restart
     */
    private fun triggerAppRestart() {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            
            if (intent != null) {
                context.startActivity(intent)
                
                // Kill current process to force full restart
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restart app", e)
        }
    }
}

/**
 * Update status
 */
sealed class UpdateStatus {
    object Idle : UpdateStatus()
    data class Downloading(val progress: Int) : UpdateStatus()
    object Downloaded : UpdateStatus()
    object Installing : UpdateStatus()
    object Success : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

/**
 * Update info
 */
data class UpdateInfo(
    val version: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val fileSize: Long,
    val isRequired: Boolean
)
