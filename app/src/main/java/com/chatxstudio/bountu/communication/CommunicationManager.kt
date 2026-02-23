package com.chatxstudio.bountu.communication

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Manages cross-platform communication functionality
 * Provides methods to interact with the communication service and Windows counterparts
 */
class CommunicationManager(private val context: Context) {
    
    var isConnectedToWindows by mutableStateOf(false)
        private set
    
    var windowsHostAddress by mutableStateOf("")
        private set
    
    var connectionStatus by mutableStateOf("Disconnected")
        private set
    
    private val communicationModule = CrossPlatformCommunication()
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    
    /**
     * Starts the cross-platform communication service
     */
    fun startCommunicationService() {
        val intent = Intent(context, CrossPlatformService::class.java).apply {
            action = CrossPlatformService.ACTION_START
        }
        context.startService(intent)
        connectionStatus = "Service started"
    }
    
    /**
     * Stops the cross-platform communication service
     */
    fun stopCommunicationService() {
        val intent = Intent(context, CrossPlatformService::class.java).apply {
            action = CrossPlatformService.ACTION_STOP
        }
        context.startService(intent)
        connectionStatus = "Service stopped"
        isConnectedToWindows = false
    }
    
    /**
     * Attempts to discover Windows Bountu instances on the local network
     */
    fun discoverWindowsInstances() {
        coroutineScope.launch {
            connectionStatus = "Discovering Windows instances..."
            val hosts = communicationModule.discoverWindowsInstances()
            
            if (hosts.isNotEmpty()) {
                windowsHostAddress = hosts.first() // Use the first discovered host
                connectionStatus = "Found Windows instance at ${hosts.first()}"
                isConnectedToWindows = true
            } else {
                connectionStatus = "No Windows instances found"
                isConnectedToWindows = false
            }
        }
    }
    
    /**
     * Sends a command to the Windows Bountu instance
     */
    suspend fun sendCommandToWindows(command: String): String? {
        if (!isConnectedToWindows || windowsHostAddress.isEmpty()) {
            return "Not connected to Windows instance"
        }
        
        val response = communicationModule.sendCommandToWindows(windowsHostAddress, command)
        return response?.output
    }
    
    /**
     * Gets the local IP address of the Android device
     */
    fun getLocalIpAddress(): String {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipAddress = wifiManager.connectionInfo.ipAddress
        
        // Convert integer IP to dotted decimal notation
        return String.format(
            "%d.%d.%d.%d",
            ipAddress and 0xff,
            ipAddress shr 8 and 0xff,
            ipAddress shr 16 and 0xff,
            ipAddress shr 24 and 0xff
        )
    }
    
    /**
     * Checks if the device is connected to WiFi
     */
    fun isWifiConnected(): Boolean {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }
}