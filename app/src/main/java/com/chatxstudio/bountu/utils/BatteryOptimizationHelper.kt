package com.chatxstudio.bountu.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

/**
 * Helper class to manage battery optimization settings
 * Helps keep the app running in background
 */
object BatteryOptimizationHelper {
    
    private const val TAG = "BatteryOptimization"
    
    /**
     * Check if battery optimization is disabled for the app
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = context.packageName
            return powerManager.isIgnoringBatteryOptimizations(packageName)
        }
        return true
    }
    
    /**
     * Request to disable battery optimization for the app
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isIgnoringBatteryOptimizations(context)) {
                try {
                    val intent = Intent().apply {
                        action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    Log.d(TAG, "Requested battery optimization exemption")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to request battery optimization exemption", e)
                    // Fallback: open battery optimization settings
                    openBatteryOptimizationSettings(context)
                }
            }
        }
    }
    
    /**
     * Open battery optimization settings page
     */
    fun openBatteryOptimizationSettings(context: Context) {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            Log.d(TAG, "Opened battery optimization settings")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open battery optimization settings", e)
        }
    }
}
