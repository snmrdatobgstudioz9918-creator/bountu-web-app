package com.chatxstudio.bountu.auth

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.*

/**
 * Account Manager for Bountu
 * Handles user registration, login, and profile management
 */
class AccountManager(private val context: Context) {
    
    companion object {
        private const val TAG = "AccountManager"
        private const val ACCOUNTS_FILE = "accounts.json"
        private const val CURRENT_USER_FILE = "current_user.json"
        private const val PREFS_NAME = "bountu_auth"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_USER_ID = "user_id"
    }
    
    private val accountsFile: File by lazy {
        File(context.filesDir, ACCOUNTS_FILE)
    }
    
    private val currentUserFile: File by lazy {
        File(context.filesDir, CURRENT_USER_FILE)
    }
    
    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn
    
    init {
        loadCurrentUser()
    }
    
    /**
     * Register a new user
     */
    suspend fun register(
        username: String,
        email: String,
        password: String,
        fullName: String
    ): AuthResult = withContext(Dispatchers.IO) {
        try {
            // Validate input
            if (username.length < 3) {
                return@withContext AuthResult.Error("Username must be at least 3 characters")
            }
            
            if (!isValidEmail(email)) {
                return@withContext AuthResult.Error("Invalid email address")
            }
            
            if (password.length < 6) {
                return@withContext AuthResult.Error("Password must be at least 6 characters")
            }
            
            // Check if user already exists
            if (userExists(username, email)) {
                return@withContext AuthResult.Error("Username or email already exists")
            }
            
            // Create new user
            val user = User(
                id = UUID.randomUUID().toString(),
                username = username,
                email = email,
                passwordHash = hashPassword(password),
                fullName = fullName,
                createdAt = System.currentTimeMillis(),
                lastLoginAt = System.currentTimeMillis(),
                profilePicture = null,
                bio = "",
                preferences = UserPreferences()
            )
            
            // Save user
            saveUser(user)
            
            // Auto-login after registration
            login(username, password)
            
            Log.d(TAG, "User registered: $username")
            AuthResult.Success(user)
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            AuthResult.Error("Registration failed: ${e.message}")
        }
    }
    
    /**
     * Login user
     */
    suspend fun login(username: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        try {
            val user = findUser(username)
                ?: return@withContext AuthResult.Error("User not found")
            
            val passwordHash = hashPassword(password)
            if (user.passwordHash != passwordHash) {
                return@withContext AuthResult.Error("Invalid password")
            }
            
            // Update last login
            val updatedUser = user.copy(lastLoginAt = System.currentTimeMillis())
            updateUser(updatedUser)
            
            // Set current user
            _currentUser.value = updatedUser
            _isLoggedIn.value = true
            
            // Save login state
            prefs.edit()
                .putBoolean(KEY_LOGGED_IN, true)
                .putString(KEY_USER_ID, updatedUser.id)
                .apply()
            
            saveCurrentUser(updatedUser)
            
            Log.d(TAG, "User logged in: $username")
            AuthResult.Success(updatedUser)
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            AuthResult.Error("Login failed: ${e.message}")
        }
    }
    
    /**
     * Logout current user
     */
    fun logout() {
        _currentUser.value = null
        _isLoggedIn.value = false
        
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, false)
            .remove(KEY_USER_ID)
            .apply()
        
        currentUserFile.delete()
        
        Log.d(TAG, "User logged out")
    }
    
    /**
     * Update user profile
     */
    suspend fun updateProfile(
        fullName: String? = null,
        bio: String? = null,
        profilePicture: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value ?: return@withContext false
            
            val updatedUser = user.copy(
                fullName = fullName ?: user.fullName,
                bio = bio ?: user.bio,
                profilePicture = profilePicture ?: user.profilePicture
            )
            
            updateUser(updatedUser)
            _currentUser.value = updatedUser
            saveCurrentUser(updatedUser)
            
            Log.d(TAG, "Profile updated")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Profile update failed", e)
            false
        }
    }
    
    /**
     * Change password
     */
    suspend fun changePassword(
        oldPassword: String,
        newPassword: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value ?: return@withContext false
            
            val oldPasswordHash = hashPassword(oldPassword)
            if (user.passwordHash != oldPasswordHash) {
                return@withContext false
            }
            
            if (newPassword.length < 6) {
                return@withContext false
            }
            
            val updatedUser = user.copy(passwordHash = hashPassword(newPassword))
            updateUser(updatedUser)
            _currentUser.value = updatedUser
            
            Log.d(TAG, "Password changed")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Password change failed", e)
            false
        }
    }
    
    /**
     * Update user preferences
     */
    suspend fun updatePreferences(preferences: UserPreferences): Boolean = withContext(Dispatchers.IO) {
        try {
            val user = _currentUser.value ?: return@withContext false
            
            val updatedUser = user.copy(preferences = preferences)
            updateUser(updatedUser)
            _currentUser.value = updatedUser
            saveCurrentUser(updatedUser)
            
            Log.d(TAG, "Preferences updated")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Preferences update failed", e)
            false
        }
    }
    
    // Private helper methods
    
    private fun loadCurrentUser() {
        try {
            if (!prefs.getBoolean(KEY_LOGGED_IN, false)) {
                return
            }
            
            val userId = prefs.getString(KEY_USER_ID, null) ?: return
            
            if (currentUserFile.exists()) {
                val json = JSONObject(currentUserFile.readText())
                val user = parseUser(json)
                _currentUser.value = user
                _isLoggedIn.value = true
                Log.d(TAG, "Current user loaded: ${user.username}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load current user", e)
        }
    }
    
    private fun saveCurrentUser(user: User) {
        try {
            val json = userToJson(user)
            currentUserFile.writeText(json.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save current user", e)
        }
    }
    
    private fun userExists(username: String, email: String): Boolean {
        return findUser(username) != null || findUserByEmail(email) != null
    }
    
    private fun findUser(username: String): User? {
        try {
            if (!accountsFile.exists()) return null
            
            val json = JSONObject(accountsFile.readText())
            val users = json.optJSONArray("users") ?: return null
            
            for (i in 0 until users.length()) {
                val userJson = users.getJSONObject(i)
                if (userJson.getString("username") == username) {
                    return parseUser(userJson)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find user", e)
        }
        return null
    }
    
    private fun findUserByEmail(email: String): User? {
        try {
            if (!accountsFile.exists()) return null
            
            val json = JSONObject(accountsFile.readText())
            val users = json.optJSONArray("users") ?: return null
            
            for (i in 0 until users.length()) {
                val userJson = users.getJSONObject(i)
                if (userJson.getString("email") == email) {
                    return parseUser(userJson)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to find user by email", e)
        }
        return null
    }
    
    private fun saveUser(user: User) {
        try {
            val json = if (accountsFile.exists()) {
                JSONObject(accountsFile.readText())
            } else {
                JSONObject().apply {
                    put("users", org.json.JSONArray())
                }
            }
            
            val users = json.getJSONArray("users")
            users.put(userToJson(user))
            
            accountsFile.writeText(json.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user", e)
        }
    }
    
    private fun updateUser(user: User) {
        try {
            if (!accountsFile.exists()) return
            
            val json = JSONObject(accountsFile.readText())
            val users = json.getJSONArray("users")
            
            for (i in 0 until users.length()) {
                val userJson = users.getJSONObject(i)
                if (userJson.getString("id") == user.id) {
                    users.put(i, userToJson(user))
                    break
                }
            }
            
            accountsFile.writeText(json.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update user", e)
        }
    }
    
    private fun parseUser(json: JSONObject): User {
        val prefsJson = json.optJSONObject("preferences")
        val preferences = if (prefsJson != null) {
            UserPreferences(
                theme = prefsJson.optString("theme", "dark"),
                terminalFont = prefsJson.optString("terminalFont", "monospace"),
                terminalFontSize = prefsJson.optInt("terminalFontSize", 14),
                enableNotifications = prefsJson.optBoolean("enableNotifications", true),
                enableAutoTrain = prefsJson.optBoolean("enableAutoTrain", false)
            )
        } else {
            UserPreferences()
        }
        
        return User(
            id = json.getString("id"),
            username = json.getString("username"),
            email = json.getString("email"),
            passwordHash = json.getString("passwordHash"),
            fullName = json.getString("fullName"),
            createdAt = json.getLong("createdAt"),
            lastLoginAt = json.getLong("lastLoginAt"),
            profilePicture = json.optString("profilePicture", null),
            bio = json.optString("bio", ""),
            preferences = preferences
        )
    }
    
    private fun userToJson(user: User): JSONObject {
        return JSONObject().apply {
            put("id", user.id)
            put("username", user.username)
            put("email", user.email)
            put("passwordHash", user.passwordHash)
            put("fullName", user.fullName)
            put("createdAt", user.createdAt)
            put("lastLoginAt", user.lastLoginAt)
            put("profilePicture", user.profilePicture)
            put("bio", user.bio)
            put("preferences", JSONObject().apply {
                put("theme", user.preferences.theme)
                put("terminalFont", user.preferences.terminalFont)
                put("terminalFontSize", user.preferences.terminalFontSize)
                put("enableNotifications", user.preferences.enableNotifications)
                put("enableAutoTrain", user.preferences.enableAutoTrain)
            })
        }
    }
    
    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}

/**
 * User data class
 */
data class User(
    val id: String,
    val username: String,
    val email: String,
    val passwordHash: String,
    val fullName: String,
    val createdAt: Long,
    val lastLoginAt: Long,
    val profilePicture: String?,
    val bio: String,
    val preferences: UserPreferences
)

/**
 * User preferences
 */
data class UserPreferences(
    val theme: String = "dark",
    val terminalFont: String = "monospace",
    val terminalFontSize: Int = 14,
    val enableNotifications: Boolean = true,
    val enableAutoTrain: Boolean = false
)

/**
 * Authentication result
 */
sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Error(val message: String) : AuthResult()
}
