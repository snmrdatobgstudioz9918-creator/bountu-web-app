package com.chatxstudio.bountu.terminal

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chatxstudio.bountu.communication.CommunicationManager
import com.chatxstudio.bountu.security.CommandValidationResult
import com.chatxstudio.bountu.security.SecurityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TerminalViewModel(
    private val communicationManager: CommunicationManager? = null,
    private val securityManager: SecurityManager? = null
) {
    private val TAG = "TerminalViewModel"
    
    var terminalOutput by mutableStateOf("")
        private set
    
    var commandInput by mutableStateOf("")
        private set
    
    init {
        initializeTerminal()
    }
    
    private fun initializeTerminal() {
        terminalOutput = "Welcome to Bountu - Linux-like environment for Android\n"
        terminalOutput += "Type 'help' to see available commands\n"
        terminalOutput += "\$ "
    }
    
    fun executeCommand(input: String) {
        if (input.isBlank()) {
            terminalOutput += "\$ "
            return
        }
        
        // Validate the command for security
        if (securityManager != null) {
            val validationResult = securityManager.validateCommand(input)
            
            when (validationResult) {
                is CommandValidationResult.Invalid -> {
                    terminalOutput += "SECURITY ERROR: ${validationResult.reason}\n"
                    terminalOutput += "\$ "
                    securityManager.logSecurityEvent("Blocked invalid command: $input")
                    return
                }
                is CommandValidationResult.RequiresConfirmation -> {
                    // In a real implementation, we would prompt for confirmation
                    // For now, we'll allow it but log it
                    securityManager.logSecurityEvent("Dangerous command executed: $input")
                    terminalOutput += "WARNING: ${validationResult.message}\n"
                }
                is CommandValidationResult.Valid -> {
                    // Command is valid, proceed
                }
            }
        }
        
        terminalOutput += "$input\n"
        
        // Parse the command
        val parts = input.trim().split("\\s+".toRegex())
        val command = parts[0].lowercase()
        
        when (command) {
            "help" -> showHelp()
            "clear" -> clearTerminal()
            "echo" -> echoCommand(parts.drop(1).joinToString(" "))
            "pwd" -> showWorkingDirectory()
            "ls" -> listDirectory(parts.getOrNull(1))
            "whoami" -> showCurrentUser()
            "date" -> showDate()
            "uname" -> showSystemInfo()
            "cat" -> catFile(parts.drop(1))
            // SSH helpers
            "sshinfo" -> showSshInfo()
            "sshuri" -> showSshUri()
            // Cross-platform
            "connect" -> connectToWindows(parts.drop(1).firstOrNull())
            "disconnect" -> disconnectFromWindows()
            "wexec" -> executeOnWindows(parts.drop(1).joinToString(" "))
            "discover" -> discoverWindows()
            else -> {
                terminalOutput += "Command not found: $command. Type 'help' for available commands.\n"
                terminalOutput += "\$ "
            }
        }

    }
    
    private fun showHelp() {
        terminalOutput += """
            Available commands:
            help       - Show this help message
            clear      - Clear the terminal screen
            echo       - Display a line of text
            pwd        - Print working directory
            ls         - List directory contents
            whoami     - Print current user
            date       - Show current date and time
            uname      - Print system information
            cat        - Concatenate and display file content
            sshinfo    - Show SSH CLI for Termius (ssh user@<ip> -p 22)
            sshuri     - Show ssh:// URI for Termius deep link
            exit       - Exit the terminal
            
        """.trimIndent()
        terminalOutput += "\$ "
    }

    
    private fun clearTerminal() {
        terminalOutput = "\$ "
    }
    
    private fun echoCommand(text: String) {
        terminalOutput += "$text\n"
        terminalOutput += "\$ "
    }
    
    private fun showWorkingDirectory() {
        terminalOutput += "/home/user\n"  // Simplified path for demo
        terminalOutput += "\$ "
    }
    
    private fun listDirectory(option: String?) {
        if (option == "-la" || option == "-a") {
            terminalOutput += ". .. .config .bashrc Documents Downloads\n"
        } else {
            terminalOutput += "Documents Downloads Music Pictures Videos\n"
        }
        terminalOutput += "\$ "
    }
    
    private fun showCurrentUser() {
        terminalOutput += "user\n"
        terminalOutput += "\$ "
    }
    
    private fun showDate() {
        val currentTime = java.time.LocalDateTime.now().toString()
        terminalOutput += "$currentTime\n"
        terminalOutput += "\$ "
    }
    
    private fun showSystemInfo() {
        terminalOutput += "Bountu Linux 1.0.0 (Android)\n"
        terminalOutput += "\$ "
    }
    
    private fun catFile(args: List<String>) {
        if (args.isEmpty()) {
            terminalOutput += "cat: missing file operand\n"
        } else {
            terminalOutput += "Content of ${args.joinToString(" ")} would appear here\n"
        }
        terminalOutput += "\$ "
    }

    private fun showSshInfo() {
        val details = com.chatxstudio.bountu.ssh.SshInfo.currentDeviceAsServer()
        if (details == null) {
            terminalOutput += "No network IP found. Connect to Wi‑Fi or hotspot.\n"
        } else {
            terminalOutput += details.toCli() + "\n"
        }
        terminalOutput += "\$ "
    }

    private fun showSshUri() {
        val details = com.chatxstudio.bountu.ssh.SshInfo.currentDeviceAsServer()
        if (details == null) {
            terminalOutput += "No network IP found. Connect to Wi‑Fi or hotspot.\n"
        } else {
            terminalOutput += details.toUri().toString() + "\n"
        }
        terminalOutput += "\$ "
    }
    
    private fun connectToWindows(host: String?) {

        if (communicationManager == null) {
            terminalOutput += "Cross-platform communication not available\n"
            terminalOutput += "\$ "
            return
        }
        
        if (host != null) {
            // In a real implementation, we would store the host address
            terminalOutput += "Connecting to Windows at $host...\n"
            terminalOutput += "Connection established to Windows Bountu instance\n"
        } else {
            terminalOutput += "Usage: connect <host_address>\n"
        }
        terminalOutput += "\$ "
    }
    
    private fun disconnectFromWindows() {
        if (communicationManager == null) {
            terminalOutput += "Cross-platform communication not available\n"
            terminalOutput += "\$ "
            return
        }
        
        terminalOutput += "Disconnecting from Windows...\n"
        terminalOutput += "Disconnected from Windows Bountu instance\n"
        terminalOutput += "\$ "
    }
    
    private fun discoverWindows() {
        if (communicationManager == null) {
            terminalOutput += "Cross-platform communication not available\n"
            terminalOutput += "\$ "
            return
        }
        
        terminalOutput += "Discovering Windows Bountu instances...\n"
        communicationManager.discoverWindowsInstances()
        terminalOutput += "Discovery initiated. Check connection status.\n"
        terminalOutput += "\$ "
    }
    
    private fun executeOnWindows(command: String) {
        if (communicationManager == null) {
            terminalOutput += "Cross-platform communication not available\n"
            terminalOutput += "\$ "
            return
        }
        
        if (command.isEmpty()) {
            terminalOutput += "Usage: wexec <command>\n"
            terminalOutput += "\$ "
            return
        }
        
        terminalOutput += "Executing '$command' on Windows...\n"
        
        // Execute command asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            val result = communicationManager.sendCommandToWindows(command)
            withContext(Dispatchers.Main) {
                if (result != null) {
                    terminalOutput += "Result from Windows:\n$result\n"
                } else {
                    terminalOutput += "Failed to execute command on Windows\n"
                }
                terminalOutput += "\$ "
            }
        }
    }
    
    fun updateCommandInput(newInput: String) {
        commandInput = newInput
    }
    
    fun clearCommandInput() {
        commandInput = ""
    }
}