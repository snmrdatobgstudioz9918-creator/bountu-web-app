package com.chatxstudio.bountu.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.URL
import kotlin.system.measureTimeMillis

/**
 * Connection Monitor
 * Monitors network connectivity, measures ping, and checks sync status
 */
class ConnectionMonitor(private val context: Context) {
    
    companion object {
        private const val TAG = "ConnectionMonitor"
        private const val PING_HOST = "8.8.8.8" // Google DNS
        private const val GITHUB_HOST = "github.com"
        private const val TIMEOUT_MS = 5000
    }
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Checking)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    
    private val _pingMs = MutableStateFlow<Long?>(null)
    val pingMs: StateFlow<Long?> = _pingMs
    
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.NotSynced)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus
    
    private val _lastSyncTime = MutableStateFlow<Long>(0)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime
    
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    init {
        startMonitoring()
    }
    
    /**
     * Start monitoring network connectivity
     */
    fun startMonitoring() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "Network available")
                _connectionState.value = ConnectionState.Connected
                scope.launch { checkConnection() }
            }
            
            override fun onLost(network: Network) {
                Log.d(TAG, "Network lost")
                _connectionState.value = ConnectionState.Disconnected
                _pingMs.value = null
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val hasValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                
                if (hasInternet && hasValidated) {
                    _connectionState.value = ConnectionState.Connected
                } else {
                    _connectionState.value = ConnectionState.Limited
                }
            }
        }
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback!!)
        
        // Initial check
        scope.launch { checkConnection() }
    }
    
    /**
     * Stop monitoring
     */
    fun stopMonitoring() {
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
        }
        networkCallback = null
    }
    
    /**
     * Check connection status
     */
    suspend fun checkConnection(): ConnectionResult = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.Checking
            
            // Check if network is available
            if (!isNetworkAvailable()) {
                _connectionState.value = ConnectionState.Disconnected
                return@withContext ConnectionResult.NoNetwork
            }
            
            // Measure ping
            val ping = measurePing()
            _pingMs.value = ping
            
            if (ping == null) {
                _connectionState.value = ConnectionState.Limited
                return@withContext ConnectionResult.Limited
            }
            
            // Check GitHub connectivity
            val githubReachable = checkGitHubConnectivity()
            
            if (!githubReachable) {
                _connectionState.value = ConnectionState.Limited
                return@withContext ConnectionResult.GitHubUnreachable
            }
            
            _connectionState.value = ConnectionState.Connected
            ConnectionResult.Success(ping)
        } catch (e: Exception) {
            Log.e(TAG, "Connection check failed", e)
            _connectionState.value = ConnectionState.Error
            ConnectionResult.Error(e.message ?: "Unknown error")
        }
    }
    
    /**
     * Measure ping to host
     */
    suspend fun measurePing(host: String = PING_HOST): Long? = withContext(Dispatchers.IO) {
        try {
            val pingTime = measureTimeMillis {
                val address = InetAddress.getByName(host)
                address.isReachable(TIMEOUT_MS)
            }
            
            Log.d(TAG, "Ping to $host: ${pingTime}ms")
            pingTime
        } catch (e: Exception) {
            Log.e(TAG, "Ping failed", e)
            null
        }
    }
    
    /**
     * Check GitHub connectivity
     */
    suspend fun checkGitHubConnectivity(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://$GITHUB_HOST")
            val connection = url.openConnection()
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.connect()
            
            Log.d(TAG, "GitHub is reachable")
            true
        } catch (e: Exception) {
            Log.e(TAG, "GitHub unreachable", e)
            false
        }
    }
    
    /**
     * Check if network is available
     */
    fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    /**
     * Get network type
     */
    fun getNetworkType(): NetworkType {
        val network = connectivityManager.activeNetwork ?: return NetworkType.None
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.None
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WiFi
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.Cellular
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.Ethernet
            else -> NetworkType.Other
        }
    }
    
    /**
     * Update sync status
     */
    fun updateSyncStatus(status: SyncStatus) {
        _syncStatus.value = status
        if (status is SyncStatus.Synced) {
            _lastSyncTime.value = System.currentTimeMillis()
        }
    }
    
    /**
     * Get connection quality
     */
    fun getConnectionQuality(): ConnectionQuality {
        val ping = _pingMs.value ?: return ConnectionQuality.Unknown
        
        return when {
            ping < 50 -> ConnectionQuality.Excellent
            ping < 100 -> ConnectionQuality.Good
            ping < 200 -> ConnectionQuality.Fair
            ping < 500 -> ConnectionQuality.Poor
            else -> ConnectionQuality.VeryPoor
        }
    }
    
    /**
     * Get time since last sync
     */
    fun getTimeSinceLastSync(): Long {
        val lastSync = _lastSyncTime.value
        if (lastSync == 0L) return -1
        return System.currentTimeMillis() - lastSync
    }
    
    /**
     * Check if sync is needed
     */
    fun isSyncNeeded(maxAgeMs: Long = 3600000): Boolean { // Default 1 hour
        val timeSinceSync = getTimeSinceLastSync()
        return timeSinceSync < 0 || timeSinceSync > maxAgeMs
    }
}

/**
 * Connection state
 */
sealed class ConnectionState {
    object Checking : ConnectionState()
    object Connected : ConnectionState()
    object Disconnected : ConnectionState()
    object Limited : ConnectionState()
    object Error : ConnectionState()
}

/**
 * Connection result
 */
sealed class ConnectionResult {
    data class Success(val pingMs: Long) : ConnectionResult()
    object NoNetwork : ConnectionResult()
    object Limited : ConnectionResult()
    object GitHubUnreachable : ConnectionResult()
    data class Error(val message: String) : ConnectionResult()
}

/**
 * Sync status
 */
sealed class SyncStatus {
    object NotSynced : SyncStatus()
    object Syncing : SyncStatus()
    data class Synced(val timestamp: Long) : SyncStatus()
    data class Failed(val error: String) : SyncStatus()
}

/**
 * Network type
 */
enum class NetworkType {
    None, WiFi, Cellular, Ethernet, Other
}

/**
 * Connection quality
 */
enum class ConnectionQuality {
    Unknown, Excellent, Good, Fair, Poor, VeryPoor
}
