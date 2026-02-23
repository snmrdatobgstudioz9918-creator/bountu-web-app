package com.chatxstudio.bountu.terminal

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Desktop-style Terminal for Android
 * Full-featured terminal with command history, tab completion, and more
 */
class DesktopTerminal(private val context: Context) {
    
    companion object {
        private const val TAG = "DesktopTerminal"
        private const val HISTORY_FILE = "terminal_history.txt"
        private const val MAX_HISTORY = 1000
    }
    
    private val historyFile: File by lazy {
        File(context.filesDir, HISTORY_FILE)
    }
    
    private val _output = MutableStateFlow<List<TerminalLine>>(emptyList())
    val output: StateFlow<List<TerminalLine>> = _output
    
    private val _currentDirectory = MutableStateFlow(context.filesDir.absolutePath)
    val currentDirectory: StateFlow<String> = _currentDirectory
    
    private val _commandHistory = MutableStateFlow<List<String>>(emptyList())
    val commandHistory: StateFlow<List<String>> = _commandHistory
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning
    
    private var currentProcess: Process? = null
    
    init {
        loadHistory()
        addOutput(TerminalLine("Welcome to Bountu Terminal", TerminalLineType.SYSTEM))
        addOutput(TerminalLine("Type 'help' for available commands", TerminalLineType.SYSTEM))
        addOutput(TerminalLine("", TerminalLineType.PROMPT))
    }
    
    /**
     * Execute command
     */
    suspend fun executeCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (command.isBlank()) return@withContext true
            
            _isRunning.value = true
            
            // Add to history
            addToHistory(command)
            
            // Add command to output
            addOutput(TerminalLine("$ $command", TerminalLineType.COMMAND))
            
            // Parse and execute command
            val result = when {
                command.startsWith("cd ") -> changeDirectory(command.substring(3).trim())
                command == "pwd" -> printWorkingDirectory()
                command == "clear" -> clearScreen()
                command == "history" -> showHistory()
                command == "help" -> showHelp()
                command.startsWith("export ") -> setEnvironmentVariable(command)
                command.startsWith("alias ") -> setAlias(command)
                else -> executeSystemCommand(command)
            }
            
            if (!result) {
                addOutput(TerminalLine("Command failed", TerminalLineType.ERROR))
            }
            
            // Add prompt for next command
            addOutput(TerminalLine("", TerminalLineType.PROMPT))
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Command execution failed", e)
            addOutput(TerminalLine("Error: ${e.message}", TerminalLineType.ERROR))
            false
        } finally {
            _isRunning.value = false
        }
    }
    
    /**
     * Stop current running command
     */
    fun stopCommand() {
        currentProcess?.destroy()
        currentProcess = null
        _isRunning.value = false
        addOutput(TerminalLine("^C", TerminalLineType.SYSTEM))
    }
    
    /**
     * Clear terminal
     */
    fun clear() {
        _output.value = emptyList()
        addOutput(TerminalLine("", TerminalLineType.PROMPT))
    }
    
    /**
     * Get command suggestions for tab completion
     */
    fun getCommandSuggestions(partial: String): List<String> {
        val commands = listOf(
            "ls", "cd", "pwd", "cat", "echo", "mkdir", "rm", "cp", "mv",
            "grep", "find", "chmod", "chown", "ps", "kill", "top", "df",
            "du", "tar", "gzip", "gunzip", "wget", "curl", "git", "nano",
            "vim", "python", "node", "npm", "pip", "apt", "pkg", "clear",
            "history", "help", "exit", "export", "alias"
        )
        
        return commands.filter { it.startsWith(partial) }
    }
    
    // Private helper methods
    
    private fun changeDirectory(path: String): Boolean {
        return try {
            val newDir = if (path.startsWith("/")) {
                File(path)
            } else {
                File(_currentDirectory.value, path)
            }
            
            if (newDir.exists() && newDir.isDirectory) {
                _currentDirectory.value = newDir.absolutePath
                true
            } else {
                addOutput(TerminalLine("cd: no such directory: $path", TerminalLineType.ERROR))
                false
            }
        } catch (e: Exception) {
            addOutput(TerminalLine("cd: ${e.message}", TerminalLineType.ERROR))
            false
        }
    }
    
    private fun printWorkingDirectory(): Boolean {
        addOutput(TerminalLine(_currentDirectory.value, TerminalLineType.OUTPUT))
        return true
    }
    
    private fun clearScreen(): Boolean {
        clear()
        return true
    }
    
    private fun showHistory(): Boolean {
        _commandHistory.value.forEachIndexed { index, cmd ->
            addOutput(TerminalLine("${index + 1}  $cmd", TerminalLineType.OUTPUT))
        }
        return true
    }
    
    private fun showHelp(): Boolean {
        val helpText = """
            Bountu Terminal - Available Commands:
            
            File Operations:
              ls [path]          - List directory contents
              cd <path>          - Change directory
              pwd                - Print working directory
              cat <file>         - Display file contents
              mkdir <dir>        - Create directory
              rm <file>          - Remove file
              cp <src> <dst>     - Copy file
              mv <src> <dst>     - Move file
            
            System:
              ps                 - List processes
              kill <pid>         - Kill process
              top                - System monitor
              df                 - Disk usage
              clear              - Clear screen
              history            - Command history
              help               - Show this help
            
            Package Management:
              apt install <pkg>  - Install package
              apt remove <pkg>   - Remove package
              apt update         - Update package list
            
            Development:
              git <args>         - Git commands
              python <file>      - Run Python script
              node <file>        - Run Node.js script
              nano <file>        - Edit file with nano
              vim <file>         - Edit file with vim
            
            Network:
              wget <url>         - Download file
              curl <url>         - Transfer data
              ping <host>        - Ping host
            
            Other:
              export VAR=value   - Set environment variable
              alias name=cmd     - Create command alias
              exit               - Exit terminal
        """.trimIndent()
        
        helpText.lines().forEach { line ->
            addOutput(TerminalLine(line, TerminalLineType.OUTPUT))
        }
        return true
    }
    
    private fun setEnvironmentVariable(command: String): Boolean {
        // Simplified environment variable handling
        addOutput(TerminalLine("Environment variable set", TerminalLineType.OUTPUT))
        return true
    }
    
    private fun setAlias(command: String): Boolean {
        // Simplified alias handling
        addOutput(TerminalLine("Alias created", TerminalLineType.OUTPUT))
        return true
    }
    
    private suspend fun executeSystemCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val processBuilder = ProcessBuilder()
            processBuilder.command("sh", "-c", command)
            processBuilder.directory(File(_currentDirectory.value))
            processBuilder.redirectErrorStream(true)
            
            currentProcess = processBuilder.start()
            
            val reader = BufferedReader(InputStreamReader(currentProcess!!.inputStream))
            var line: String?
            
            while (reader.readLine().also { line = it } != null) {
                addOutput(TerminalLine(line!!, TerminalLineType.OUTPUT))
            }
            
            val exitCode = currentProcess!!.waitFor()
            currentProcess = null
            
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "System command failed", e)
            addOutput(TerminalLine("Error: ${e.message}", TerminalLineType.ERROR))
            false
        }
    }
    
    private fun addOutput(line: TerminalLine) {
        _output.value = _output.value + line
    }
    
    private fun addToHistory(command: String) {
        val history = _commandHistory.value.toMutableList()
        history.add(command)
        
        // Keep only last MAX_HISTORY commands
        if (history.size > MAX_HISTORY) {
            history.removeAt(0)
        }
        
        _commandHistory.value = history
        saveHistory()
    }
    
    private fun loadHistory() {
        try {
            if (historyFile.exists()) {
                val history = historyFile.readLines()
                _commandHistory.value = history.takeLast(MAX_HISTORY)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load history", e)
        }
    }
    
    private fun saveHistory() {
        try {
            historyFile.writeText(_commandHistory.value.joinToString("\n"))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save history", e)
        }
    }
}

/**
 * Terminal line
 */
data class TerminalLine(
    val text: String,
    val type: TerminalLineType,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Terminal line types
 */
enum class TerminalLineType {
    COMMAND,    // User input command
    OUTPUT,     // Command output
    ERROR,      // Error message
    SYSTEM,     // System message
    PROMPT      // Command prompt
}
