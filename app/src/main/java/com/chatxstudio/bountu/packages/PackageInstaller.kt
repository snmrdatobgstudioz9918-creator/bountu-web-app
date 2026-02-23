package com.chatxstudio.bountu.packages

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import java.util.zip.GZIPInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream

/**
 * Real package installer that downloads and installs actual binaries
 * Optimized for Android (no root required)
 */
class PackageInstaller(private val context: Context) {
    
    companion object {
        private const val TAG = "PackageInstaller"
        private const val PACKAGES_DIR = "packages"
        private const val BIN_DIR = "bin"
        private const val LIB_DIR = "lib"
        private const val SHARE_DIR = "share"
        
        // Base URLs for package downloads
        private const val GITHUB_RELEASES = "https://github.com/termux/termux-packages/releases/download"
        private const val BACKUP_CDN = "https://packages-cf.termux.dev/apt/termux-main"
    }
    
    private val packagesDir: File by lazy {
        File(context.filesDir, PACKAGES_DIR).apply { mkdirs() }
    }
    
    private val binDir: File by lazy {
        File(packagesDir, BIN_DIR).apply { mkdirs() }
    }
    
    private val libDir: File by lazy {
        File(packagesDir, LIB_DIR).apply { mkdirs() }
    }
    
    private val shareDir: File by lazy {
        File(packagesDir, SHARE_DIR).apply { mkdirs() }
    }
    
    /**
     * Install a package from URL
     */
    suspend fun installPackage(
        pkg: Package,
        onProgress: (Float) -> Unit
    ): InstallationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting installation of ${pkg.name}")
            
            // Step 1: Download package (0-40%)
            val downloadedFile = downloadPackage(pkg) { progress ->
                onProgress(progress * 0.4f)
            } ?: return@withContext InstallationResult.Failure("Download failed")
            
            // Step 2: Verify checksum (40-50%)
            onProgress(0.45f)
            if (!verifyChecksum(downloadedFile, pkg.checksumSHA256)) {
                downloadedFile.delete()
                return@withContext InstallationResult.Failure("Checksum verification failed")
            }
            
            // Step 3: Extract package (50-80%)
            onProgress(0.5f)
            if (!extractPackage(downloadedFile, pkg) { progress ->
                onProgress(0.5f + progress * 0.3f)
            }) {
                downloadedFile.delete()
                return@withContext InstallationResult.Failure("Extraction failed")
            }
            
            // Step 4: Set permissions (80-90%)
            onProgress(0.8f)
            if (!setExecutablePermissions(pkg)) {
                return@withContext InstallationResult.Failure("Failed to set permissions")
            }
            
            // Step 5: Run post-install script (90-95%)
            onProgress(0.9f)
            if (pkg.installScript.isNotEmpty()) {
                runInstallScript(pkg)
            }
            
            // Step 6: Create symlinks and update PATH (95-100%)
            onProgress(0.95f)
            createSymlinks(pkg)
            updateEnvironment(pkg)
            
            // Cleanup
            downloadedFile.delete()
            onProgress(1.0f)
            
            Log.d(TAG, "Successfully installed ${pkg.name}")
            InstallationResult.Success("${pkg.name} installed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Installation failed", e)
            InstallationResult.Failure("Installation failed: ${e.message}")
        }
    }
    
    /**
     * Download package from URL
     */
    private suspend fun downloadPackage(
        pkg: Package,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        // Try primary URL, then fallbacks/mirrors with simple retry logic
        val tried = mutableListOf<String>()
        val urls = buildList {
            val primary = getPackageUrl(pkg)
            if (primary.isNotEmpty()) add(primary) else return@buildList
            // If Package has binaryUrl, prefer that first
            if (pkg.binaryUrl.isNotEmpty() && pkg.binaryUrl != primary) add(0, pkg.binaryUrl)
            // Add mirrors
            val arch = getArchitecture()
            try {
                addAll(PackageUrls.getMirrorUrl(pkg.id, pkg.version, arch))
            } catch (_: Exception) {}
        }

        if (urls.isEmpty()) {
            Log.e(TAG, "No compatible download URL for ${pkg.id}; reason: ${pkg.maintenanceReason}")
            return@withContext null
        }
        for (u in urls.distinct()) {
            try {
                Log.d(TAG, "Downloading from: $u")
                tried += u

                val outputFile = File(context.cacheDir, "${pkg.id}.pkg")
                if (downloadToFile(u, outputFile, onProgress)) {
                    return@withContext outputFile
                } else {
                    Log.w(TAG, "Download attempt failed for $u")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed from $u", e)
            }
        }

        Log.e(TAG, "All download attempts failed. Tried: ${tried.joinToString()}")
        null
    }

    // Robust HTTP downloader with redirects, headers and progress
    private fun downloadToFile(urlString: String, outputFile: File, onProgress: (Float) -> Unit): Boolean {
        var connection: HttpURLConnection? = null
        var currentUrl = urlString
        val maxRedirects = 5
        var redirects = 0
        try {
            // Follow redirects manually to preserve headers
            while (redirects <= maxRedirects) {
                connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 30000
                    readTimeout = 30000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "Bountu/1.0 (Android)")
                    setRequestProperty("Accept", "*/*")
                }
                val code = connection.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == 307 || code == 308) {
                    val loc = connection.getHeaderField("Location") ?: break
                    currentUrl = loc
                    redirects++
                    connection.disconnect()
                    continue
                }
                if (code !in 200..299) {
                    Log.w(TAG, "HTTP error $code for $currentUrl")
                    return false
                }
                val totalSize = connection.contentLengthLong.takeIf { it > 0 } ?: -1L
                connection.inputStream.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytesRead = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            if (totalSize > 0) {
                                onProgress(totalBytesRead.toFloat() / totalSize)
                            }
                        }
                    }
                }
                return true
            }
            Log.w(TAG, "Too many redirects for $urlString")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "downloadToFile failed for $urlString", e)
            return false
        } finally {
            try { connection?.disconnect() } catch (_: Exception) {}
        }
    }
    
    /**
     * Get package download URL
     */
    private fun getPackageUrl(pkg: Package): String {
        // If package has custom URL, use it
        if (pkg.binaryUrl.isNotEmpty()) {
            return pkg.binaryUrl
        }
        // If package was marked unsupported earlier, refuse silently
        if (pkg.needsMaintenance && pkg.maintenanceReason.startsWith("Unsupported")) return ""
        
        // Otherwise, construct Termux-compatible URL
        val arch = getArchitecture()
        return "$BACKUP_CDN/binary-$arch/${pkg.id}_${pkg.version}_$arch.deb"
    }
    
    /**
     * Get device architecture
     */
    private fun getArchitecture(): String {
        val arch = System.getProperty("os.arch") ?: "arm"
        return when {
            arch.contains("aarch64") || arch.contains("arm64") -> "aarch64"
            arch.contains("arm") -> "arm"
            arch.contains("x86_64") || arch.contains("amd64") -> "x86_64"
            arch.contains("x86") || arch.contains("i686") -> "i686"
            else -> "aarch64" // Default to ARM64
        }
    }
    
    /**
     * Verify package checksum
     */
    private fun verifyChecksum(file: File, expectedChecksum: String): Boolean {
        if (expectedChecksum.isEmpty()) {
            Log.w(TAG, "No checksum provided, skipping verification")
            return true
        }
        
        try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            
            val checksum = digest.digest().joinToString("") { "%02x".format(it) }
            return checksum.equals(expectedChecksum, ignoreCase = true)
        } catch (e: Exception) {
            Log.e(TAG, "Checksum verification failed", e)
            return false
        }
    }
    
    /**
     * Extract package contents
     */
    private suspend fun extractPackage(
        file: File,
        pkg: Package,
        onProgress: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val pkgDir = File(packagesDir, pkg.id).apply { mkdirs() }
            
            // Handle different archive formats
            when {
                file.name.endsWith(".zip") -> extractZip(file, pkgDir, onProgress)
                file.name.endsWith(".tar.gz") || file.name.endsWith(".tgz") -> {
                    extractTarGz(file, pkgDir, onProgress)
                }
                file.name.endsWith(".deb") -> extractDeb(file, pkgDir, onProgress)
                else -> {
                    Log.e(TAG, "Unsupported archive format: ${file.name}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Extraction failed", e)
            false
        }
    }
    
    /**
     * Extract ZIP archive
     */
    private fun extractZip(file: File, destDir: File, onProgress: (Float) -> Unit): Boolean {
        try {
            ZipInputStream(file.inputStream()).use { zip ->
                var entry = zip.nextEntry
                var count = 0
                
                while (entry != null) {
                    val outFile = File(destDir, entry.name)
                    
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { output ->
                            zip.copyTo(output)
                        }
                    }
                    
                    count++
                    if (count % 10 == 0) {
                        onProgress(count / 100f) // Approximate progress
                    }
                    
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "ZIP extraction failed", e)
            return false
        }
    }
    
    /**
     * Extract TAR.GZ archive
     */
    private fun extractTarGz(file: File, destDir: File, onProgress: (Float) -> Unit): Boolean {
        try {
            val fileSize = file.length()
            var bytesProcessed = 0L
            
            BufferedInputStream(file.inputStream()).use { fileInput ->
                GZIPInputStream(fileInput).use { gzipInput ->
                    TarArchiveInputStream(gzipInput).use { tarInput ->
                        var entry = tarInput.nextTarEntry
                        
                        while (entry != null) {
                            val outputFile = File(destDir, entry.name)
                            
                            if (entry.isDirectory) {
                                outputFile.mkdirs()
                            } else {
                                outputFile.parentFile?.mkdirs()
                                BufferedOutputStream(FileOutputStream(outputFile)).use { output ->
                                    val buffer = ByteArray(8192)
                                    var bytesRead: Int
                                    while (tarInput.read(buffer).also { bytesRead = it } != -1) {
                                        output.write(buffer, 0, bytesRead)
                                        bytesProcessed += bytesRead
                                        onProgress((bytesProcessed.toFloat() / fileSize))
                                    }
                                }
                                
                                // Preserve file permissions
                                if (entry.mode and 0x49 != 0) { // Check if executable bit is set
                                    outputFile.setExecutable(true, false)
                                }
                            }
                            
                            entry = tarInput.nextTarEntry
                        }
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "TAR.GZ extraction failed", e)
            return false
        }
    }
    
    /**
     * Extract DEB package
     * DEB files are AR archives containing control.tar.* and data.tar.* files
     * We extract the data.tar.* which contains the actual package files
     */
    private fun extractDeb(file: File, destDir: File, onProgress: (Float) -> Unit): Boolean {
        try {
            val tempDir = File(destDir.parentFile, "${destDir.name}_temp").apply { mkdirs() }
            
            // Step 1: Extract AR archive (DEB is an AR archive)
            onProgress(0.1f)
            BufferedInputStream(file.inputStream()).use { input ->
                ArArchiveInputStream(input).use { arInput ->
                    var entry = arInput.nextArEntry
                    
                    while (entry != null) {
                        val entryName = entry.name
                        
                        // We're only interested in data.tar.* files
                        if (entryName.startsWith("data.tar")) {
                            val dataTarFile = File(tempDir, entryName)
                            FileOutputStream(dataTarFile).use { output ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                while (arInput.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                }
                            }
                            
                            // Step 2: Extract the data.tar.* file
                            onProgress(0.3f)
                            val extractSuccess = when {
                                entryName.endsWith(".gz") || entryName.endsWith(".tgz") -> {
                                    extractTarGz(dataTarFile, destDir) { progress ->
                                        onProgress(0.3f + progress * 0.7f)
                                    }
                                }
                                entryName.endsWith(".xz") -> {
                                    Log.w(TAG, "XZ compression not yet supported")
                                    false
                                }
                                else -> {
                                    // Uncompressed tar
                                    extractTar(dataTarFile, destDir) { progress ->
                                        onProgress(0.3f + progress * 0.7f)
                                    }
                                }
                            }
                            
                            // Cleanup temp file
                            dataTarFile.delete()
                            tempDir.deleteRecursively()
                            
                            return extractSuccess
                        }
                        
                        entry = arInput.nextArEntry
                    }
                }
            }
            
            tempDir.deleteRecursively()
            Log.w(TAG, "No data.tar.* found in DEB package")
            return false
        } catch (e: Exception) {
            Log.e(TAG, "DEB extraction failed", e)
            return false
        }
    }
    
    /**
     * Extract uncompressed TAR archive
     */
    private fun extractTar(file: File, destDir: File, onProgress: (Float) -> Unit): Boolean {
        try {
            val fileSize = file.length()
            var bytesProcessed = 0L
            
            BufferedInputStream(file.inputStream()).use { input ->
                TarArchiveInputStream(input).use { tarInput ->
                    var entry = tarInput.nextTarEntry
                    
                    while (entry != null) {
                        val outputFile = File(destDir, entry.name)
                        
                        if (entry.isDirectory) {
                            outputFile.mkdirs()
                        } else {
                            outputFile.parentFile?.mkdirs()
                            BufferedOutputStream(FileOutputStream(outputFile)).use { output ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                while (tarInput.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    bytesProcessed += bytesRead
                                    onProgress((bytesProcessed.toFloat() / fileSize))
                                }
                            }
                            
                            // Preserve file permissions
                            if (entry.mode and 0x49 != 0) { // Check if executable bit is set
                                outputFile.setExecutable(true, false)
                            }
                        }
                        
                        entry = tarInput.nextTarEntry
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "TAR extraction failed", e)
            return false
        }
    }
    
    /**
     * Set executable permissions on binaries
     */
    private fun setExecutablePermissions(pkg: Package): Boolean {
        try {
            val pkgBinDir = File(packagesDir, "${pkg.id}/bin")
            if (pkgBinDir.exists()) {
                pkgBinDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.setExecutable(true, false)
                        file.setReadable(true, false)
                        Log.d(TAG, "Set executable: ${file.name}")
                    }
                }
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set permissions", e)
            return false
        }
    }
    
    /**
     * Run post-install script
     */
    private fun runInstallScript(pkg: Package) {
        try {
            if (pkg.installScript.isEmpty()) return
            
            val scriptFile = File(context.cacheDir, "install_${pkg.id}.sh")
            scriptFile.writeText(pkg.installScript)
            scriptFile.setExecutable(true)
            
            val process = ProcessBuilder()
                .command("sh", scriptFile.absolutePath)
                .directory(File(packagesDir, pkg.id))
                .redirectErrorStream(true)
                .start()
            
            val exitCode = process.waitFor()
            Log.d(TAG, "Install script exit code: $exitCode")
            
            scriptFile.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to run install script", e)
        }
    }
    
    /**
     * Create symlinks in bin directory
     */
    private fun createSymlinks(pkg: Package) {
        try {
            val pkgBinDir = File(packagesDir, "${pkg.id}/bin")
            if (pkgBinDir.exists()) {
                pkgBinDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        val symlink = File(binDir, file.name)
                        if (!symlink.exists()) {
                            // Create a wrapper script instead of symlink (Android doesn't support symlinks well)
                            symlink.writeText("#!/system/bin/sh\nexec ${file.absolutePath} \"$@\"\n")
                            symlink.setExecutable(true)
                            Log.d(TAG, "Created wrapper: ${file.name}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create symlinks", e)
        }
    }
    
    /**
     * Update environment variables
     */
    private fun updateEnvironment(pkg: Package) {
        try {
            val envFile = File(packagesDir, "environment.sh")
            val pathEntry = "export PATH=\"${binDir.absolutePath}:\$PATH\"\n"
            val ldPathEntry = "export LD_LIBRARY_PATH=\"${libDir.absolutePath}:\$LD_LIBRARY_PATH\"\n"
            
            if (!envFile.exists()) {
                envFile.writeText("#!/system/bin/sh\n")
            }
            
            val content = envFile.readText()
            if (!content.contains(pathEntry)) {
                envFile.appendText(pathEntry)
            }
            if (!content.contains(ldPathEntry)) {
                envFile.appendText(ldPathEntry)
            }
            
            Log.d(TAG, "Updated environment")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update environment", e)
        }
    }
    
    /**
     * Uninstall a package
     */
    suspend fun uninstallPackage(pkg: Package): InstallationResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Uninstalling ${pkg.name}")
            
            // Run uninstall script if exists
            if (pkg.uninstallScript.isNotEmpty()) {
                runUninstallScript(pkg)
            }
            
            // Remove package directory
            val pkgDir = File(packagesDir, pkg.id)
            if (pkgDir.exists()) {
                pkgDir.deleteRecursively()
            }
            
            // Remove symlinks
            binDir.listFiles()?.forEach { file ->
                if (file.readText().contains(pkg.id)) {
                    file.delete()
                }
            }
            
            Log.d(TAG, "Successfully uninstalled ${pkg.name}")
            InstallationResult.Success("${pkg.name} uninstalled successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Uninstallation failed", e)
            InstallationResult.Failure("Uninstallation failed: ${e.message}")
        }
    }
    
    /**
     * Run uninstall script
     */
    private fun runUninstallScript(pkg: Package) {
        try {
            if (pkg.uninstallScript.isEmpty()) return
            
            val scriptFile = File(context.cacheDir, "uninstall_${pkg.id}.sh")
            scriptFile.writeText(pkg.uninstallScript)
            scriptFile.setExecutable(true)
            
            val process = ProcessBuilder()
                .command("sh", scriptFile.absolutePath)
                .directory(File(packagesDir, pkg.id))
                .redirectErrorStream(true)
                .start()
            
            process.waitFor()
            scriptFile.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to run uninstall script", e)
        }
    }
    
    /**
     * Check if package is installed
     */
    fun isPackageInstalled(pkg: Package): Boolean {
        val pkgDir = File(packagesDir, pkg.id)
        return pkgDir.exists() && pkgDir.isDirectory
    }
    
    /**
     * Get installed package version
     */
    fun getInstalledVersion(pkg: Package): String? {
        val versionFile = File(packagesDir, "${pkg.id}/VERSION")
        return if (versionFile.exists()) {
            versionFile.readText().trim()
        } else {
            null
        }
    }
    
    /**
     * Get PATH for terminal
     */
    fun getPath(): String {
        return binDir.absolutePath
    }
    
    /**
     * Get LD_LIBRARY_PATH for terminal
     */
    fun getLibraryPath(): String {
        return libDir.absolutePath
    }
}
