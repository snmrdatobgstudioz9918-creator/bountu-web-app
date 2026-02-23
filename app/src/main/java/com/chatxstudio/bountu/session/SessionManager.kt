package com.chatxstudio.bountu.session

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Session Manager
 * Auto-saves terminal sessions, server states, and app state
 */
class SessionManager(private val context: Context) {
    
    companion object {
        private const val TAG = "SessionManager"
        private const val SESSIONS_DIR = "sessions"
        private const val CURRENT_SESSION_FILE = "current_session.json"
        private const val AUTO_SAVE_INTERVAL = 30000L // 30 seconds
    }
    
    private val sessionsDir: File by lazy {
        File(context.filesDir, SESSIONS_DIR).apply { mkdirs() }
    }
    
    private val currentSessionFile: File by lazy {
        File(sessionsDir, CURRENT_SESSION_FILE)
    }
    
    private val _currentSession = MutableStateFlow<Session?>(null)
    val currentSession: StateFlow<Session?> = _currentSession
    
    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions
    
    init {
        loadSessions()
        loadCurrentSession()
    }
    
    /**
     * Create a new session
     */
    suspend fun createSession(name: String): Session = withContext(Dispatchers.IO) {
        val session = Session(
            id = System.currentTimeMillis().toString(),
            name = name,
            createdAt = System.currentTimeMillis(),
            lastSavedAt = System.currentTimeMillis(),
            terminalState = TerminalState(),
            serverStates = emptyList(),
            openFiles = emptyList(),
            workingDirectory = context.filesDir.absolutePath
        )
        
        _currentSession.value = session
        saveCurrentSession()
        
        val allSessions = _sessions.value.toMutableList()
        allSessions.add(session)
        _sessions.value = allSessions
        saveSessions()
        
        Log.d(TAG, "Created session: $name")
        session
    }
    
    /**
     * Load a session
     */
    suspend fun loadSession(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        val session = _sessions.value.find { it.id == sessionId }
        if (session != null) {
            _currentSession.value = session
            saveCurrentSession()
            Log.d(TAG, "Loaded session: ${session.name}")
            true
        } else {
            Log.w(TAG, "Session not found: $sessionId")
            false
        }
    }
    
    /**
     * Save current session
     */
    suspend fun saveCurrentSession(): Boolean = withContext(Dispatchers.IO) {
        val session = _currentSession.value ?: return@withContext false
        
        try {
            val updatedSession = session.copy(lastSavedAt = System.currentTimeMillis())
            _currentSession.value = updatedSession
            
            val json = updatedSession.toJson()
            currentSessionFile.writeText(json.toString(2))
            
            // Update in sessions list
            val allSessions = _sessions.value.toMutableList()
            val index = allSessions.indexOfFirst { it.id == session.id }
            if (index != -1) {
                allSessions[index] = updatedSession
                _sessions.value = allSessions
                saveSessions()
            }
            
            Log.d(TAG, "Saved current session: ${session.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save current session", e)
            false
        }
    }
    
    /**
     * Update terminal state
     */
    suspend fun updateTerminalState(
        commandHistory: List<String>,
        currentDirectory: String,
        environmentVars: Map<String, String>
    ) {
        val session = _currentSession.value ?: return
        
        val updatedTerminalState = TerminalState(
            commandHistory = commandHistory,
            currentDirectory = currentDirectory,
            environmentVars = environmentVars
        )
        
        _currentSession.value = session.copy(
            terminalState = updatedTerminalState,
            workingDirectory = currentDirectory
        )
        
        saveCurrentSession()
    }
    
    /**
     * Update server states
     */
    suspend fun updateServerStates(serverStates: List<ServerState>) {
        val session = _currentSession.value ?: return
        
        _currentSession.value = session.copy(serverStates = serverStates)
        saveCurrentSession()
    }
    
    /**
     * Update open files
     */
    suspend fun updateOpenFiles(openFiles: List<OpenFile>) {
        val session = _currentSession.value ?: return
        
        _currentSession.value = session.copy(openFiles = openFiles)
        saveCurrentSession()
    }
    
    /**
     * Delete a session
     */
    suspend fun deleteSession(sessionId: String): Boolean = withContext(Dispatchers.IO) {
        val allSessions = _sessions.value.toMutableList()
        allSessions.removeAll { it.id == sessionId }
        _sessions.value = allSessions
        
        saveSessions()
        
        // If current session was deleted, clear it
        if (_currentSession.value?.id == sessionId) {
            _currentSession.value = null
            currentSessionFile.delete()
        }
        
        Log.d(TAG, "Deleted session: $sessionId")
        true
    }
    
    /**
     * Auto-save current session periodically
     */
    suspend fun startAutoSave() {
        while (true) {
            kotlinx.coroutines.delay(AUTO_SAVE_INTERVAL)
            if (_currentSession.value != null) {
                saveCurrentSession()
                Log.d(TAG, "Auto-saved session")
            }
        }
    }
    
    // Private helper methods
    
    private fun loadCurrentSession() {
        try {
            if (!currentSessionFile.exists()) {
                _currentSession.value = null
                return
            }
            
            val json = JSONObject(currentSessionFile.readText())
            val session = Session.fromJson(json)
            _currentSession.value = session
            
            Log.d(TAG, "Loaded current session: ${session.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load current session", e)
            _currentSession.value = null
        }
    }
    
    private fun loadSessions() {
        try {
            val sessionsFile = File(sessionsDir, "sessions.json")
            if (!sessionsFile.exists()) {
                _sessions.value = emptyList()
                return
            }
            
            val json = JSONArray(sessionsFile.readText())
            val sessions = mutableListOf<Session>()
            
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                sessions.add(Session.fromJson(obj))
            }
            
            _sessions.value = sessions
            Log.d(TAG, "Loaded ${sessions.size} sessions")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load sessions", e)
            _sessions.value = emptyList()
        }
    }
    
    private fun saveSessions() {
        try {
            val json = JSONArray()
            _sessions.value.forEach { session ->
                json.put(session.toJson())
            }
            
            val sessionsFile = File(sessionsDir, "sessions.json")
            sessionsFile.writeText(json.toString(2))
            
            Log.d(TAG, "Saved ${_sessions.value.size} sessions")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save sessions", e)
        }
    }
}

/**
 * Session data class
 */
data class Session(
    val id: String,
    val name: String,
    val createdAt: Long,
    val lastSavedAt: Long,
    val terminalState: TerminalState,
    val serverStates: List<ServerState>,
    val openFiles: List<OpenFile>,
    val workingDirectory: String
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("createdAt", createdAt)
        json.put("lastSavedAt", lastSavedAt)
        json.put("terminalState", terminalState.toJson())
        
        val serverStatesArray = JSONArray()
        serverStates.forEach { serverStatesArray.put(it.toJson()) }
        json.put("serverStates", serverStatesArray)
        
        val openFilesArray = JSONArray()
        openFiles.forEach { openFilesArray.put(it.toJson()) }
        json.put("openFiles", openFilesArray)
        
        json.put("workingDirectory", workingDirectory)
        
        return json
    }
    
    companion object {
        fun fromJson(json: JSONObject): Session {
            val terminalState = TerminalState.fromJson(json.getJSONObject("terminalState"))
            
            val serverStatesArray = json.getJSONArray("serverStates")
            val serverStates = mutableListOf<ServerState>()
            for (i in 0 until serverStatesArray.length()) {
                serverStates.add(ServerState.fromJson(serverStatesArray.getJSONObject(i)))
            }
            
            val openFilesArray = json.getJSONArray("openFiles")
            val openFiles = mutableListOf<OpenFile>()
            for (i in 0 until openFilesArray.length()) {
                openFiles.add(OpenFile.fromJson(openFilesArray.getJSONObject(i)))
            }
            
            return Session(
                id = json.getString("id"),
                name = json.getString("name"),
                createdAt = json.getLong("createdAt"),
                lastSavedAt = json.getLong("lastSavedAt"),
                terminalState = terminalState,
                serverStates = serverStates,
                openFiles = openFiles,
                workingDirectory = json.getString("workingDirectory")
            )
        }
    }
}

/**
 * Terminal state
 */
data class TerminalState(
    val commandHistory: List<String> = emptyList(),
    val currentDirectory: String = "",
    val environmentVars: Map<String, String> = emptyMap()
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        
        val historyArray = JSONArray()
        commandHistory.forEach { historyArray.put(it) }
        json.put("commandHistory", historyArray)
        
        json.put("currentDirectory", currentDirectory)
        
        val envVarsJson = JSONObject()
        environmentVars.forEach { (key, value) ->
            envVarsJson.put(key, value)
        }
        json.put("environmentVars", envVarsJson)
        
        return json
    }
    
    companion object {
        fun fromJson(json: JSONObject): TerminalState {
            val historyArray = json.getJSONArray("commandHistory")
            val commandHistory = mutableListOf<String>()
            for (i in 0 until historyArray.length()) {
                commandHistory.add(historyArray.getString(i))
            }
            
            val envVarsJson = json.getJSONObject("environmentVars")
            val environmentVars = mutableMapOf<String, String>()
            envVarsJson.keys().forEach { key ->
                environmentVars[key] = envVarsJson.getString(key)
            }
            
            return TerminalState(
                commandHistory = commandHistory,
                currentDirectory = json.getString("currentDirectory"),
                environmentVars = environmentVars
            )
        }
    }
}

/**
 * Server state
 */
data class ServerState(
    val serverId: String,
    val isRunning: Boolean,
    val port: Int
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("serverId", serverId)
        json.put("isRunning", isRunning)
        json.put("port", port)
        return json
    }
    
    companion object {
        fun fromJson(json: JSONObject): ServerState {
            return ServerState(
                serverId = json.getString("serverId"),
                isRunning = json.getBoolean("isRunning"),
                port = json.getInt("port")
            )
        }
    }
}

/**
 * Open file
 */
data class OpenFile(
    val path: String,
    val cursorPosition: Int,
    val scrollPosition: Int
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("path", path)
        json.put("cursorPosition", cursorPosition)
        json.put("scrollPosition", scrollPosition)
        return json
    }
    
    companion object {
        fun fromJson(json: JSONObject): OpenFile {
            return OpenFile(
                path = json.getString("path"),
                cursorPosition = json.getInt("cursorPosition"),
                scrollPosition = json.getInt("scrollPosition")
            )
        }
    }
}
