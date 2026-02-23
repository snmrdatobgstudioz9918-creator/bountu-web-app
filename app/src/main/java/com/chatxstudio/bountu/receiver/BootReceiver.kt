package com.chatxstudio.bountu.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.chatxstudio.bountu.service.BountuBackgroundService

/**
 * Broadcast receiver to restart service on device boot
 */
class BootReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "BootReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "Device booted or app updated, starting background service")
                BountuBackgroundService.start(context)
            }
        }
    }
}
