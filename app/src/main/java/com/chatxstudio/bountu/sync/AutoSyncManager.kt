package com.chatxstudio.bountu.sync

import android.content.Context
import android.util.Log
import com.chatxstudio.bountu.git.GitPackageManager
import com.chatxstudio.bountu.git.GitResult
import com.chatxstudio.bountu.network.ConnectionMonitor
import com.chatxstudio.bountu.network.ConnectionResult
import com.chatxstudio.bountu.network.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Auto Sync Manager
 * Automatically syncs with Git repository
 * Blocks app loading if sync fails
 */
class AutoSyncManager(
    private val context: Context,
    private val gitManager: GitPackageManager,
    private val connectionMonitor: ConnectionMonitor
) {
    
    companion object {
        private const val TAG = "AutoSyncManager"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 2000L
        private const val SYNC_TIMEOUT_MS = 30000L
    }
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState
    
    private val _retryCount = MutableStateFlow(0)
    val retryCount: StateFlow<Int> = _retryCount
    
    /**
     * Perform initial sync (required for app to load)
     */
    suspend fun performInitialSync(): SyncResult = withContext(Dispatchers.IO) {
        try {
            _syncState.value = SyncState.Checking
            _retryCount.value = 0
            
            Log.d(TAG, "Starting initial sync...")
            
            // Check connection first
            val connectionResult = connectionMonitor.checkConnection()
            
            when (connectionResult) {
                is ConnectionResult.NoNetwork -> {
                    _syncState.value = SyncState.Failed(SyncError.NoNetwork)
                    return@withContext SyncResult.Failed(SyncError.NoNetwork)
                }
                is ConnectionResult.GitHubUnreachable -> {
                    _syncState.value = SyncState.Failed(SyncError.GitHubUnreachable)
                    return@withContext SyncResult.Failed(SyncError.GitHubUnreachable)
                }
                is ConnectionResult.Limited -> {
                    _syncState.value = SyncState.Failed(SyncError.LimitedConnectivity)
                    return@withContext SyncResult.Failed(SyncError.LimitedConnectivity)
                }
                is ConnectionResult.Error -> {
                    _syncState.value = SyncState.Failed(SyncError.ConnectionError(connectionResult.message))
                    return@withContext SyncResult.Failed(SyncError.ConnectionError(connectionResult.message))
                }
                is ConnectionResult.Success -> {
                    // Connection OK, proceed with sync
                }
            }
            
            // Attempt sync with retries
            var attempt = 0
            var lastError: SyncError? = null
            
            while (attempt < MAX_RETRY_ATTEMPTS) {
                attempt++
                _retryCount.value = attempt
                _syncState.value = SyncState.Syncing(attempt, MAX_RETRY_ATTEMPTS)
                
                Log.d(TAG, "Sync attempt $attempt/$MAX_RETRY_ATTEMPTS")
                
                val syncResult = performSync()
                
                when (syncResult) {
                    is SyncResult.Success -> {
                        _syncState.value = SyncState.Success
                        connectionMonitor.updateSyncStatus(SyncStatus.Synced(System.currentTimeMillis()))
                        Log.d(TAG, "Sync successful")
                        return@withContext syncResult
                    }
                    is SyncResult.Failed -> {
                        lastError = syncResult.error
                        Log.w(TAG, "Sync attempt $attempt failed: ${syncResult.error}")
                        
                        if (attempt < MAX_RETRY_ATTEMPTS) {
                            _syncState.value = SyncState.Retrying(attempt, MAX_RETRY_ATTEMPTS)
                            delay(RETRY_DELAY_MS)
                        }
                    }
                }
            }
            
            // All attempts failed
            val finalError = lastError ?: SyncError.Unknown
            _syncState.value = SyncState.Failed(finalError)
            connectionMonitor.updateSyncStatus(SyncStatus.Failed(finalError.message))
            
            Log.e(TAG, "Sync failed after $MAX_RETRY_ATTEMPTS attempts")
            SyncResult.Failed(finalError)
            
        } catch (e: Exception) {
            Log.e(TAG, "Sync exception", e)
            val error = SyncError.Exception(e.message ?: "Unknown error")
            _syncState.value = SyncState.Failed(error)
            SyncResult.Failed(error)
        }
    }
    
    /**
     * Perform sync operation
     */
    private suspend fun performSync(): SyncResult = withContext(Dispatchers.IO) {
        try {
            // Initialize/sync Git repository
            val initResult = gitManager.initialize(forceRefresh = true)
            
            when (initResult) {
                is GitResult.Error -> {
                    return@withContext SyncResult.Failed(SyncError.GitInitFailed)
                }
                is GitResult.Success -> {
                    // Continue
                }
            }
            
            // Verify repository is accessible
            val repoInfo = gitManager.getRepositoryInfo()
            if (repoInfo == null) {
                return@withContext SyncResult.Failed(SyncError.RepositoryNotAccessible)
            }
            
            // List packages to verify sync
            val packagesResult = gitManager.listPackages()
            val packageCount = when (packagesResult) {
                is GitResult.Success -> packagesResult.data.size
                is GitResult.Error -> 0
            }
            
            if (packageCount == 0) {
                return@withContext SyncResult.Failed(SyncError.NoPackagesFound)
            }
            
            Log.d(TAG, "Sync completed: $packageCount packages found")
            SyncResult.Success(packageCount)
            
        } catch (e: Exception) {
            Log.e(TAG, "Sync operation failed", e)
            SyncResult.Failed(SyncError.Exception(e.message ?: "Sync failed"))
        }
    }
    
    /**
     * Retry sync
     */
    suspend fun retrySync(): SyncResult {
        return performInitialSync()
    }
    
    /**
     * Check if sync is required
     */
    fun isSyncRequired(): Boolean {
        return connectionMonitor.isSyncNeeded()
    }
    
    /**
     * Get sync age in milliseconds
     */
    fun getSyncAge(): Long {
        return connectionMonitor.getTimeSinceLastSync()
    }
}

/**
 * Sync state
 */
sealed class SyncState {
    object Idle : SyncState()
    object Checking : SyncState()
    data class Syncing(val attempt: Int, val maxAttempts: Int) : SyncState()
    data class Retrying(val attempt: Int, val maxAttempts: Int) : SyncState()
    object Success : SyncState()
    data class Failed(val error: SyncError) : SyncState()
}

/**
 * Sync result
 */
sealed class SyncResult {
    data class Success(val packageCount: Int) : SyncResult()
    data class Failed(val error: SyncError) : SyncResult()
}

/**
 * Sync error
 */
sealed class SyncError {
    object NoNetwork : SyncError()
    object GitHubUnreachable : SyncError()
    object LimitedConnectivity : SyncError()
    data class ConnectionError(val msg: String) : SyncError()
    object GitInitFailed : SyncError()
    object RepositoryNotAccessible : SyncError()
    object NoPackagesFound : SyncError()
    data class Exception(val msg: String) : SyncError()
    object Unknown : SyncError()
    
    val message: String
        get() = when (this) {
            is NoNetwork -> "No network connection"
            is GitHubUnreachable -> "Cannot reach GitHub"
            is LimitedConnectivity -> "Limited connectivity"
            is ConnectionError -> msg
            is GitInitFailed -> "Failed to initialize Git repository"
            is RepositoryNotAccessible -> "Repository not accessible"
            is NoPackagesFound -> "No packages found in repository"
            is Exception -> msg
            is Unknown -> "Unknown error"
        }
}
