package com.chatxstudio.bountu.server

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Server Manager
 * Create, run, and manage servers (HTTP, Database, etc.)
 */
class ServerManager(private val context: Context) {
    
    companion object {
        private const val TAG = "ServerManager"
        private const val SERVERS_DIR = "servers"
        private const val SERVERS_FILE = "servers.json"
    }
    
    private val serversDir: File by lazy {
        File(context.filesDir, SERVERS_DIR).apply { mkdirs() }
    }
    
    private val serversFile: File by lazy {
        File(serversDir, SERVERS_FILE)
    }
    
    private val _servers = MutableStateFlow<List<Server>>(emptyList())
    val servers: StateFlow<List<Server>> = _servers
    
    private val runningProcesses = mutableMapOf<String, Process>()
    
    init {
        loadServers()
    }
    
    /**
     * Create a new server
     */
    suspend fun createServer(
        name: String,
        type: ServerType,
        port: Int,
        autoStart: Boolean = false
    ): Server = withContext(Dispatchers.IO) {
        val server = Server(
            id = System.currentTimeMillis().toString(),
            name = name,
            type = type,
            port = port,
            status = ServerStatus.STOPPED,
            autoStart = autoStart,
            createdAt = System.currentTimeMillis(),
            config = getDefaultConfig(type, port)
        )
        
        val currentServers = _servers.value.toMutableList()
        currentServers.add(server)
        _servers.value = currentServers
        
        saveServers()
        
        Log.d(TAG, "Created server: $name (${type.name}) on port $port")
        server
    }
    
    /**
     * Start a server
     */
    suspend fun startServer(serverId: String): Boolean = withContext(Dispatchers.IO) {
        val server = _servers.value.find { it.id == serverId } ?: return@withContext false
        
        if (server.status == ServerStatus.RUNNING) {
            Log.w(TAG, "Server ${server.name} is already running")
            return@withContext true
        }
        
        try {
            val process = when (server.type) {
                ServerType.HTTP -> startHttpServer(server)
                ServerType.NGINX -> startNginxServer(server)
                ServerType.APACHE -> startApacheServer(server)
                ServerType.MYSQL -> startMySQLServer(server)
                ServerType.POSTGRESQL -> startPostgreSQLServer(server)
                ServerType.REDIS -> startRedisServer(server)
                ServerType.MONGODB -> startMongoDBServer(server)
                ServerType.CUSTOM -> startCustomServer(server)
            }
            
            if (process != null) {
                runningProcesses[serverId] = process
                updateServerStatus(serverId, ServerStatus.RUNNING)
                Log.d(TAG, "Started server: ${server.name}")
                true
            } else {
                Log.e(TAG, "Failed to start server: ${server.name}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting server: ${server.name}", e)
            updateServerStatus(serverId, ServerStatus.ERROR)
            false
        }
    }
    
    /**
     * Stop a server
     */
    suspend fun stopServer(serverId: String): Boolean = withContext(Dispatchers.IO) {
        val server = _servers.value.find { it.id == serverId } ?: return@withContext false
        
        try {
            val process = runningProcesses[serverId]
            if (process != null) {
                process.destroy()
                process.waitFor()
                runningProcesses.remove(serverId)
            }
            
            updateServerStatus(serverId, ServerStatus.STOPPED)
            Log.d(TAG, "Stopped server: ${server.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server: ${server.name}", e)
            false
        }
    }
    
    /**
     * Restart a server
     */
    suspend fun restartServer(serverId: String): Boolean {
        stopServer(serverId)
        kotlinx.coroutines.delay(1000)
        return startServer(serverId)
    }
    
    /**
     * Delete a server
     */
    suspend fun deleteServer(serverId: String): Boolean = withContext(Dispatchers.IO) {
        stopServer(serverId)
        
        val currentServers = _servers.value.toMutableList()
        currentServers.removeAll { it.id == serverId }
        _servers.value = currentServers
        
        saveServers()
        Log.d(TAG, "Deleted server: $serverId")
        true
    }
    
    /**
     * Get server logs
     */
    suspend fun getServerLogs(serverId: String): List<String> = withContext(Dispatchers.IO) {
        val logFile = File(serversDir, "$serverId.log")
        if (logFile.exists()) {
            logFile.readLines().takeLast(100)
        } else {
            emptyList()
        }
    }
    
    /**
     * Auto-start servers marked for auto-start
     */
    suspend fun autoStartServers() {
        _servers.value.filter { it.autoStart }.forEach { server ->
            startServer(server.id)
        }
    }
    
    // Private helper methods
    
    private fun startHttpServer(server: Server): Process? {
        val command = listOf(
            "python3", "-m", "http.server", server.port.toString()
        )
        return ProcessBuilder(command)
            .directory(File(server.config["root"] ?: context.filesDir.absolutePath))
            .redirectOutput(File(serversDir, "${server.id}.log"))
            .redirectError(File(serversDir, "${server.id}.error.log"))
            .start()
    }
    
    private fun startNginxServer(server: Server): Process? {
        val configFile = File(serversDir, "${server.id}_nginx.conf")
        createNginxConfig(server, configFile)
        
        val command = listOf("nginx", "-c", configFile.absolutePath)
        return ProcessBuilder(command)
            .redirectOutput(File(serversDir, "${server.id}.log"))
            .redirectError(File(serversDir, "${server.id}.error.log"))
            .start()
    }
    
    private fun startApacheServer(server: Server): Process? {
        val configFile = File(serversDir, "${server.id}_apache.conf")
        createApacheConfig(server, configFile)
        
        val command = listOf("httpd", "-f", configFile.absolutePath)
        return ProcessBuilder(command)
            .redirectOutput(File(serversDir, "${server.id}.log"))
            .redirectError(File(serversDir, "${server.id}.error.log"))
            .start()
    }
    
    private fun startMySQLServer(server: Server): Process? {
        val dataDir = File(serversDir, "${server.id}_mysql_data").apply { mkdirs() }
        
        val command = listOf(
            "mysqld",
            "--datadir=${dataDir.absolutePath}",
            "--port=${server.port}",
            "--socket=${File(serversDir, "${server.id}.sock").absolutePath}"
        )
        return ProcessBuilder(command)
            .redirectOutput(File(serversDir, "${server.id}.log"))
            .redirectError(File(serversDir, "${server.id}.error.log"))
            .start()
    }
    
    private fun startPostgreSQLServer(server: Server): Process? {
        val dataDir = File(serversDir, "${server.id}_postgres_data").apply { mkdirs() }
        
        val command = listOf(
            "postgres",
            "-D", dataDir.absolutePath,
            "-p", server.port.toString()
        )
        return ProcessBuilder(command)
            .redirectOutput(File(serversDir, "${server.id}.log"))
            .redirectError(File(serversDir, "${server.id}.error.log"))
            .start()
    }
    
    private fun startRedisServer(server: Server): Process? {
        val configFile = File(serversDir, "${server.id}_redis.conf")
        createRedisConfig(server, configFile)
        
        val command = listOf("redis-server", configFile.absolutePath)
        return ProcessBuilder(command)
            .redirectOutput(File(serversDir, "${server.id}.log"))
            .redirectError(File(serversDir, "${server.id}.error.log"))
            .start()
    }
    
    private fun startMongoDBServer(server: Server): Process? {
        val dataDir = File(serversDir, "${server.id}_mongo_data").apply { mkdirs() }
        
        val command = listOf(
            "mongod",
            "--dbpath", dataDir.absolutePath,
            "--port", server.port.toString()
        )
        return ProcessBuilder(command)
            .redirectOutput(File(serversDir, "${server.id}.log"))
            .redirectError(File(serversDir, "${server.id}.error.log"))
            .start()
    }
    
    private fun startCustomServer(server: Server): Process? {
        val command = server.config["command"]?.split(" ") ?: return null
        return ProcessBuilder(command)
            .redirectOutput(File(serversDir, "${server.id}.log"))
            .redirectError(File(serversDir, "${server.id}.error.log"))
            .start()
    }
    
    private fun createNginxConfig(server: Server, configFile: File) {
        val config = """
            worker_processes 1;
            events {
                worker_connections 1024;
            }
            http {
                server {
                    listen ${server.port};
                    server_name localhost;
                    root ${server.config["root"] ?: context.filesDir.absolutePath};
                    index index.html index.htm;
                }
            }
        """.trimIndent()
        configFile.writeText(config)
    }
    
    private fun createApacheConfig(server: Server, configFile: File) {
        val config = """
            Listen ${server.port}
            ServerRoot "${context.filesDir.absolutePath}"
            DocumentRoot "${server.config["root"] ?: context.filesDir.absolutePath}"
            <Directory "${server.config["root"] ?: context.filesDir.absolutePath}">
                Options Indexes FollowSymLinks
                AllowOverride All
                Require all granted
            </Directory>
        """.trimIndent()
        configFile.writeText(config)
    }
    
    private fun createRedisConfig(server: Server, configFile: File) {
        val config = """
            port ${server.port}
            bind 127.0.0.1
            dir ${serversDir.absolutePath}
        """.trimIndent()
        configFile.writeText(config)
    }
    
    private fun getDefaultConfig(type: ServerType, port: Int): Map<String, String> {
        return when (type) {
            ServerType.HTTP -> mapOf("root" to context.filesDir.absolutePath)
            ServerType.NGINX -> mapOf("root" to context.filesDir.absolutePath)
            ServerType.APACHE -> mapOf("root" to context.filesDir.absolutePath)
            ServerType.MYSQL -> mapOf("socket" to File(serversDir, "mysql.sock").absolutePath)
            ServerType.POSTGRESQL -> mapOf("data" to File(serversDir, "postgres_data").absolutePath)
            ServerType.REDIS -> mapOf("dir" to serversDir.absolutePath)
            ServerType.MONGODB -> mapOf("dbpath" to File(serversDir, "mongo_data").absolutePath)
            ServerType.CUSTOM -> emptyMap()
        }
    }
    
    private fun updateServerStatus(serverId: String, status: ServerStatus) {
        val currentServers = _servers.value.toMutableList()
        val index = currentServers.indexOfFirst { it.id == serverId }
        if (index != -1) {
            currentServers[index] = currentServers[index].copy(status = status)
            _servers.value = currentServers
            saveServers()
        }
    }
    
    private fun loadServers() {
        try {
            if (!serversFile.exists()) {
                _servers.value = emptyList()
                return
            }
            
            val json = JSONArray(serversFile.readText())
            val servers = mutableListOf<Server>()
            
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                servers.add(Server.fromJson(obj))
            }
            
            _servers.value = servers
            Log.d(TAG, "Loaded ${servers.size} servers")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load servers", e)
            _servers.value = emptyList()
        }
    }
    
    private fun saveServers() {
        try {
            val json = JSONArray()
            _servers.value.forEach { server ->
                json.put(server.toJson())
            }
            serversFile.writeText(json.toString(2))
            Log.d(TAG, "Saved ${_servers.value.size} servers")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save servers", e)
        }
    }
}

/**
 * Server data class
 */
data class Server(
    val id: String,
    val name: String,
    val type: ServerType,
    val port: Int,
    val status: ServerStatus,
    val autoStart: Boolean,
    val createdAt: Long,
    val config: Map<String, String>
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("type", type.name)
        json.put("port", port)
        json.put("status", status.name)
        json.put("autoStart", autoStart)
        json.put("createdAt", createdAt)
        
        val configJson = JSONObject()
        config.forEach { (key, value) ->
            configJson.put(key, value)
        }
        json.put("config", configJson)
        
        return json
    }
    
    companion object {
        fun fromJson(json: JSONObject): Server {
            val configJson = json.getJSONObject("config")
            val config = mutableMapOf<String, String>()
            configJson.keys().forEach { key ->
                config[key] = configJson.getString(key)
            }
            
            return Server(
                id = json.getString("id"),
                name = json.getString("name"),
                type = ServerType.valueOf(json.getString("type")),
                port = json.getInt("port"),
                status = ServerStatus.valueOf(json.getString("status")),
                autoStart = json.getBoolean("autoStart"),
                createdAt = json.getLong("createdAt"),
                config = config
            )
        }
    }
}

/**
 * Server types
 */
enum class ServerType {
    HTTP,
    NGINX,
    APACHE,
    MYSQL,
    POSTGRESQL,
    REDIS,
    MONGODB,
    CUSTOM
}

/**
 * Server status
 */
enum class ServerStatus {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING,
    ERROR
}
