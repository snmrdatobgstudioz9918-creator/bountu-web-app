package com.chatxstudio.bountu.communication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Background service for handling cross-platform communication
 * Runs in the background to listen for and send commands to Windows counterpart
 */
class CrossPlatformService : Service() {
    
    companion object {
        const val CHANNEL_ID = "BountuCrossPlatformChannel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.chatxstudio.bountu.START_CROSS_PLATFORM_SERVICE"
        const val ACTION_STOP = "com.chatxstudio.bountu.STOP_CROSS_PLATFORM_SERVICE"
    }
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var communicationModule: CrossPlatformCommunication? = null
    
    override fun onCreate() {
        super.onCreate()
        communicationModule = CrossPlatformCommunication()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, createNotification())
                // Start listening for connections
                serviceScope.launch {
                    communicationModule?.startServer()
                }
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }
        
        // Keep service running
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't provide binding for this service
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bountu Cross-Platform Communication",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Handles communication between Bountu Android and Windows apps"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bountu Cross-Platform Service")
            .setContentText("Connected to Windows Bountu instance")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Placeholder icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}