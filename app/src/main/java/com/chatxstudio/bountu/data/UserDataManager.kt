package com.chatxstudio.bountu.data

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
 * User Data Manager
 * Saves and restores all user progress and data
 */
class UserDataManager(private val context: Context) {
    
    companion object {
        private const val TAG = "UserDataManager"
        private const val USER_DATA_DIR = "user_data"
        private const val PROGRESS_FILE = "progress.json"
        private const val SETTINGS_FILE = "settings.json"
        private const val TERMINAL_HISTORY_FILE = "terminal_history.json"
        private const val AI_CONVERSATIONS_FILE = "ai_conversations.json"
    }
    
    private val userDataDir: File by lazy {
        File(context.filesDir, USER_DATA_DIR).apply { mkdirs() }
    }
    
    private val _userProgress = MutableStateFlow<UserProgress?>(null)
    val userProgress: StateFlow<UserProgress?> = _userProgress
    
    private val _userSettings = MutableStateFlow<UserSettings?>(null)
    val userSettings: StateFlow<UserSettings?> = _userSettings
    
    init {
        loadUserData()
    }
    
    /**
     * Load all user data
     */
    private fun loadUserData() {
        try {
            loadProgress()
            loadSettings()
            Log.d(TAG, "User data loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load user data", e)
        }
    }
    
    /**
     * Save user progress
     */
    suspend fun saveProgress(progress: UserProgress): Boolean = withContext(Dispatchers.IO) {
        try {
            val progressFile = File(userDataDir, PROGRESS_FILE)
            val json = progressToJson(progress)
            progressFile.writeText(json.toString(2))
            
            _userProgress.value = progress
            
            Log.d(TAG, "Progress saved")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save progress", e)
            false
        }
    }
    
    /**
     * Load user progress
     */
    private fun loadProgress() {
        try {
            val progressFile = File(userDataDir, PROGRESS_FILE)
            if (!progressFile.exists()) {
                _userProgress.value = UserProgress()
                return
            }
            
            val json = JSONObject(progressFile.readText())
            val progress = jsonToProgress(json)
            _userProgress.value = progress
            
            Log.d(TAG, "Progress loaded")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load progress", e)
            _userProgress.value = UserProgress()
        }
    }
    
    /**
     * Save user settings
     */
    suspend fun saveSettings(settings: UserSettings): Boolean = withContext(Dispatchers.IO) {
        try {
            val settingsFile = File(userDataDir, SETTINGS_FILE)
            val json = settingsToJson(settings)
            settingsFile.writeText(json.toString(2))
            
            _userSettings.value = settings
            
            Log.d(TAG, "Settings saved")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save settings", e)
            false
        }
    }
    
    /**
     * Load user settings
     */
    private fun loadSettings() {
        try {
            val settingsFile = File(userDataDir, SETTINGS_FILE)
            if (!settingsFile.exists()) {
                _userSettings.value = UserSettings()
                return
            }
            
            val json = JSONObject(settingsFile.readText())
            val settings = jsonToSettings(json)
            _userSettings.value = settings
            
            Log.d(TAG, "Settings loaded")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load settings", e)
            _userSettings.value = UserSettings()
        }
    }
    
    /**
     * Save terminal history
     */
    suspend fun saveTerminalHistory(history: List<String>): Boolean = withContext(Dispatchers.IO) {
        try {
            val historyFile = File(userDataDir, TERMINAL_HISTORY_FILE)
            val json = JSONArray()
            
            history.forEach { command ->
                json.put(command)
            }
            
            historyFile.writeText(json.toString(2))
            
            Log.d(TAG, "Terminal history saved: ${history.size} commands")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save terminal history", e)
            false
        }
    }
    
    /**
     * Load terminal history
     */
    suspend fun loadTerminalHistory(): List<String> = withContext(Dispatchers.IO) {
        try {
            val historyFile = File(userDataDir, TERMINAL_HISTORY_FILE)
            if (!historyFile.exists()) {
                return@withContext emptyList()
            }
            
            val json = JSONArray(historyFile.readText())
            val history = mutableListOf<String>()
            
            for (i in 0 until json.length()) {
                history.add(json.getString(i))
            }
            
            Log.d(TAG, "Terminal history loaded: ${history.size} commands")
            history
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load terminal history", e)
            emptyList()
        }
    }
    
    /**
     * Save AI conversations
     */
    suspend fun saveAIConversations(conversations: Map<String, List<AIMessage>>): Boolean = withContext(Dispatchers.IO) {
        try {
            val conversationsFile = File(userDataDir, AI_CONVERSATIONS_FILE)
            val json = JSONObject()
            
            conversations.forEach { (botId, messages) ->
                val messagesArray = JSONArray()
                messages.forEach { message ->
                    messagesArray.put(aiMessageToJson(message))
                }
                json.put(botId, messagesArray)
            }
            
            conversationsFile.writeText(json.toString(2))
            
            Log.d(TAG, "AI conversations saved")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save AI conversations", e)
            false
        }
    }
    
    /**
     * Load AI conversations
     */
    suspend fun loadAIConversations(): Map<String, List<AIMessage>> = withContext(Dispatchers.IO) {
        try {
            val conversationsFile = File(userDataDir, AI_CONVERSATIONS_FILE)
            if (!conversationsFile.exists()) {
                return@withContext emptyMap()
            }
            
            val json = JSONObject(conversationsFile.readText())
            val conversations = mutableMapOf<String, List<AIMessage>>()
            
            json.keys().forEach { botId ->
                val messagesArray = json.getJSONArray(botId)
                val messages = mutableListOf<AIMessage>()
                
                for (i in 0 until messagesArray.length()) {
                    messages.add(jsonToAIMessage(messagesArray.getJSONObject(i)))
                }
                
                conversations[botId] = messages
            }
            
            Log.d(TAG, "AI conversations loaded")
            conversations
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load AI conversations", e)
            emptyMap()
        }
    }
    
    /**
     * Update package installation status
     */
    suspend fun updateInstalledPackage(packageId: String, installed: Boolean): Boolean {
        val progress = _userProgress.value ?: UserProgress()
        
        val updatedPackages = progress.installedPackages.toMutableSet()
        if (installed) {
            updatedPackages.add(packageId)
        } else {
            updatedPackages.remove(packageId)
        }
        
        val updatedProgress = progress.copy(
            installedPackages = updatedPackages,
            lastUpdated = System.currentTimeMillis()
        )
        
        return saveProgress(updatedProgress)
    }
    
    /**
     * Update AI bot training level
     */
    suspend fun updateBotTrainingLevel(botId: String, level: Int): Boolean {
        val progress = _userProgress.value ?: UserProgress()
        
        val updatedBots = progress.aiBotLevels.toMutableMap()
        updatedBots[botId] = level
        
        val updatedProgress = progress.copy(
            aiBotLevels = updatedBots,
            lastUpdated = System.currentTimeMillis()
        )
        
        return saveProgress(updatedProgress)
    }
    
    /**
     * Update terminal usage stats
     */
    suspend fun updateTerminalStats(commandsExecuted: Int): Boolean {
        val progress = _userProgress.value ?: UserProgress()
        
        val updatedProgress = progress.copy(
            terminalCommandsExecuted = progress.terminalCommandsExecuted + commandsExecuted,
            lastUpdated = System.currentTimeMillis()
        )
        
        return saveProgress(updatedProgress)
    }
    
    /**
     * Clear all user data
     */
    suspend fun clearAllData(): Boolean = withContext(Dispatchers.IO) {
        try {
            userDataDir.listFiles()?.forEach { it.delete() }
            
            _userProgress.value = UserProgress()
            _userSettings.value = UserSettings()
            
            Log.d(TAG, "All user data cleared")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear user data", e)
            false
        }
    }
    
    /**
     * Export user data
     */
    suspend fun exportUserData(): String? = withContext(Dispatchers.IO) {
        try {
            val exportData = JSONObject().apply {
                put("progress", progressToJson(_userProgress.value ?: UserProgress()))
                put("settings", settingsToJson(_userSettings.value ?: UserSettings()))
                put("exportedAt", System.currentTimeMillis())
                put("version", "1.0")
            }
            
            exportData.toString(2)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export user data", e)
            null
        }
    }
    
    /**
     * Import user data
     */
    suspend fun importUserData(data: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(data)
            
            val progress = jsonToProgress(json.getJSONObject("progress"))
            val settings = jsonToSettings(json.getJSONObject("settings"))
            
            saveProgress(progress)
            saveSettings(settings)
            
            Log.d(TAG, "User data imported")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import user data", e)
            false
        }
    }
    
    // Helper methods for JSON conversion
    
    private fun progressToJson(progress: UserProgress): JSONObject {
        return JSONObject().apply {
            put("installedPackages", JSONArray(progress.installedPackages.toList()))
            put("aiBotLevels", JSONObject(progress.aiBotLevels))
            put("terminalCommandsExecuted", progress.terminalCommandsExecuted)
            put("lastSyncTime", progress.lastSyncTime)
            put("lastUpdated", progress.lastUpdated)
        }
    }
    
    private fun jsonToProgress(json: JSONObject): UserProgress {
        val installedPackages = mutableSetOf<String>()
        val packagesArray = json.getJSONArray("installedPackages")
        for (i in 0 until packagesArray.length()) {
            installedPackages.add(packagesArray.getString(i))
        }
        
        val aiBotLevels = mutableMapOf<String, Int>()
        val botsJson = json.getJSONObject("aiBotLevels")
        botsJson.keys().forEach { key ->
            aiBotLevels[key] = botsJson.getInt(key)
        }
        
        return UserProgress(
            installedPackages = installedPackages,
            aiBotLevels = aiBotLevels,
            terminalCommandsExecuted = json.getInt("terminalCommandsExecuted"),
            lastSyncTime = json.getLong("lastSyncTime"),
            lastUpdated = json.getLong("lastUpdated")
        )
    }
    
    private fun settingsToJson(settings: UserSettings): JSONObject {
        return JSONObject().apply {
            put("theme", settings.theme)
            put("terminalFontSize", settings.terminalFontSize)
            put("terminalFont", settings.terminalFont)
            put("autoSync", settings.autoSync)
            put("notifications", settings.notifications)
            put("autoTrainAI", settings.autoTrainAI)
        }
    }
    
    private fun jsonToSettings(json: JSONObject): UserSettings {
        return UserSettings(
            theme = json.getString("theme"),
            terminalFontSize = json.getInt("terminalFontSize"),
            terminalFont = json.getString("terminalFont"),
            autoSync = json.getBoolean("autoSync"),
            notifications = json.getBoolean("notifications"),
            autoTrainAI = json.getBoolean("autoTrainAI")
        )
    }
    
    private fun aiMessageToJson(message: AIMessage): JSONObject {
        return JSONObject().apply {
            put("sender", message.sender)
            put("content", message.content)
            put("timestamp", message.timestamp)
        }
    }
    
    private fun jsonToAIMessage(json: JSONObject): AIMessage {
        return AIMessage(
            sender = json.getString("sender"),
            content = json.getString("content"),
            timestamp = json.getLong("timestamp")
        )
    }
}

/**
 * User Progress data class
 */
data class UserProgress(
    val installedPackages: Set<String> = emptySet(),
    val aiBotLevels: Map<String, Int> = emptyMap(),
    val terminalCommandsExecuted: Int = 0,
    val lastSyncTime: Long = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * User Settings data class
 */
data class UserSettings(
    val theme: String = "dark",
    val terminalFontSize: Int = 14,
    val terminalFont: String = "monospace",
    val autoSync: Boolean = true,
    val notifications: Boolean = true,
    val autoTrainAI: Boolean = true
)

/**
 * AI Message data class
 */
data class AIMessage(
    val sender: String,
    val content: String,
    val timestamp: Long
)
