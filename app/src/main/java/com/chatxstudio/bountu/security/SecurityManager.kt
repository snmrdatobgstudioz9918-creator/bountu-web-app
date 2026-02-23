package com.chatxstudio.bountu.security

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.chatxstudio.bountu.terminal.TerminalViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import java.util.*

/**
 * Security manager for Bountu app
 * Handles permissions, secure command execution, and access control
 */
class SecurityManager(private val context: Context) {
    
    // State flows for security status
    private val _permissionsGranted = MutableStateFlow(false)
    val permissionsGranted: StateFlow<Boolean> = _permissionsGranted
    
    private val _isSecureMode = MutableStateFlow(true) // Start in secure mode
    val isSecureMode: StateFlow<Boolean> = _isSecureMode
    
    private val _securityLog = mutableListOf<String>()
    val securityLog: List<String> = _securityLog
    
    // Dangerous commands that require extra confirmation
    private val dangerousCommands = setOf(
        "rm", "rmdir", "mkfs", "dd", "format", "chmod", "chown", 
        "mount", "umount", "kill", "pkill", "killall", "reboot", 
        "shutdown", "halt", "poweroff", "passwd", "su", "sudo"
    )
    
    /**
     * Check if all required permissions are granted
     */
    fun checkPermissions(): Boolean {
        val permissions = getRequiredPermissions()
        var allGranted = true
        
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false
                break
            }
        }
        
        _permissionsGranted.value = allGranted
        return allGranted
    }
    
    /**
     * Get list of required permissions for the app
     */
    private fun getRequiredPermissions(): Array<String> {
        val base = mutableListOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            base.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return base.toTypedArray()
    }
    
    /**
     * Request necessary permissions from the user
     */
    fun requestPermissions(): Array<String> {
        val permissions = mutableListOf<String>()
        
        for (permission in getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(permission)
            }
        }
        
        return permissions.toTypedArray()
    }
    
    /**
     * Validates if a command is safe to execute
     */
    fun validateCommand(command: String): CommandValidationResult {
        val trimmedCmd = command.trim()
        if (trimmedCmd.isEmpty()) {
            return CommandValidationResult.Valid
        }
        
        // Split command into parts
        val parts = trimmedCmd.split("\\s+".toRegex())
        val cmd = parts[0].lowercase()
        
        // Check if command is in dangerous list
        if (cmd in dangerousCommands) {
            return CommandValidationResult.RequiresConfirmation(
                "Command '$cmd' is potentially dangerous. Are you sure you want to execute it?",
                command
            )
        }
        
        // Check for path traversal attempts
        if (containsPathTraversal(command)) {
            return CommandValidationResult.Invalid("Command contains potential path traversal attempts")
        }
        
        // Check for command injection attempts
        if (containsCommandInjection(command)) {
            return CommandValidationResult.Invalid("Command contains potential injection attempts")
        }
        
        return CommandValidationResult.Valid
    }
    
    /**
     * Checks if command contains path traversal attempts
     */
    private fun containsPathTraversal(command: String): Boolean {
        return command.contains("../") || command.contains("..\\") || 
               command.contains("/../") || command.contains("\\..\\")
    }
    
    /**
     * Checks if command contains potential command injection
     */
    private fun containsCommandInjection(command: String): Boolean {
        val injectionPatterns = listOf(
            ";", // Command separator
            "&&", // Logical AND
            "||", // Logical OR
            "|", // Pipe
            "`", // Command substitution
            "\$", // Variable expansion
            "\\(", "\\)" // Subshell
        )
        
        return injectionPatterns.any { command.contains(it) }
    }
    
    /**
     * Logs security events
     */
    fun logSecurityEvent(event: String) {
        val timestamp = Date().toString()
        val logEntry = "[$timestamp] $event"
        _securityLog.add(logEntry)
        
        // Keep log size reasonable
        if (_securityLog.size > 100) {
            _securityLog.removeAt(0)
        }
    }
    
    /**
     * Toggle secure mode
     */
    fun toggleSecureMode() {
        _isSecureMode.value = !_isSecureMode.value
        logSecurityEvent("Secure mode toggled to ${_isSecureMode.value}")
    }
    
    /**
     * Hash a string using SHA-256
     */
    fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Validate network communication security
     */
    fun validateNetworkAccess(host: String): Boolean {
        // Prevent localhost loopback attacks
        if (host.startsWith("127.") || host.equals("localhost", ignoreCase = true)) {
            logSecurityEvent("Blocked connection attempt to localhost: $host")
            return false
        }
        
        // Additional validation can be added here
        return true
    }
}

/**
 * Sealed class for command validation results
 */
sealed class CommandValidationResult {
    object Valid : CommandValidationResult()
    class Invalid(val reason: String) : CommandValidationResult()
    class RequiresConfirmation(val message: String, val command: String) : CommandValidationResult()
}