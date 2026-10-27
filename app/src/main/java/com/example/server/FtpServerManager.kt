package com.example.server

import android.os.Environment
import android.util.Log
import com.example.data.ActivityLog
import com.example.data.AppSettings
import com.example.data.LogLevel
import com.example.data.ServerInfo
import com.example.data.ServerStatus
import com.example.network.NetworkHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.Authority
import org.apache.ftpserver.ftplet.DefaultFtplet
import org.apache.ftpserver.ftplet.FtpException
import org.apache.ftpserver.ftplet.FtpReply
import org.apache.ftpserver.ftplet.FtpRequest
import org.apache.ftpserver.ftplet.FtpSession
import org.apache.ftpserver.ftplet.FtpletResult
import org.apache.ftpserver.ftplet.UserManager
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission
import org.apache.ftpserver.usermanager.impl.TransferRatePermission
import org.apache.ftpserver.usermanager.impl.WritePermission
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class FtpServerManager private constructor() {

    companion object {
        private const val TAG = "FtpServerManager"

        @Volatile
        private var instance: FtpServerManager? = null

        fun getInstance(): FtpServerManager {
            return instance ?: synchronized(this) {
                instance ?: FtpServerManager().also { instance = it }
            }
        }
    }

    private var ftpServer: FtpServer? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _serverInfo = MutableStateFlow(ServerInfo())
    val serverInfo: StateFlow<ServerInfo> = _serverInfo.asStateFlow()

    private val _activityLogs = MutableStateFlow<List<ActivityLog>>(emptyList())
    val activityLogs: StateFlow<List<ActivityLog>> = _activityLogs.asStateFlow()

    private val _logEvents = MutableSharedFlow<ActivityLog>(extraBufferCapacity = 64)
    val logEvents: SharedFlow<ActivityLog> = _logEvents.asSharedFlow()

    private val activeConnectionCounter = AtomicInteger(0)

    fun log(message: String, level: LogLevel = LogLevel.INFO, tag: String? = null) {
        val entry = ActivityLog(message = message, level = level, tag = tag)
        scope.launch {
            _logEvents.emit(entry)
            _activityLogs.value = (_activityLogs.value + entry).takeLast(200)
        }
        when (level) {
            LogLevel.INFO -> Log.i(TAG, message)
            LogLevel.SUCCESS -> Log.i(TAG, "✓ $message")
            LogLevel.WARNING -> Log.w(TAG, "⚠ $message")
            LogLevel.ERROR -> Log.e(TAG, "✗ $message")
        }
    }

    fun clearLogs() {
        _activityLogs.value = emptyList()
    }

    @Synchronized
    fun start(settings: AppSettings, localIp: String? = null): Boolean {
        if (ftpServer != null && !ftpServer!!.isStopped) {
            log("FTP Server is already running on port ${_serverInfo.value.port}", LogLevel.WARNING)
            return true
        }

        _serverInfo.value = _serverInfo.value.copy(status = ServerStatus.STARTING)
        log("Initializing FTP Server on port ${settings.port}...", LogLevel.INFO)

        val ip = localIp ?: NetworkHelper.getLocalIpAddress() ?: "127.0.0.1"

        try {
            val serverFactory = FtpServerFactory()
            val listenerFactory = ListenerFactory()

            // Bind to requested port
            listenerFactory.port = settings.port
            
            // Set data connection idle time
            listenerFactory.dataConnectionConfiguration

            serverFactory.addListener("default", listenerFactory.createListener())

            // Setup root path
            val rootDir = File(settings.rootPath).let {
                if (it.exists() && it.canRead()) it else Environment.getExternalStorageDirectory()
            }
            if (!rootDir.exists()) {
                rootDir.mkdirs()
            }

            // Setup User Manager
            val userManagerFactory = PropertiesUserManagerFactory()
            val userManager: UserManager = userManagerFactory.createUserManager()

            val authorities = ArrayList<Authority>()
            if (settings.allowWrite) {
                authorities.add(WritePermission())
            }
            authorities.add(ConcurrentLoginPermission(20, 10))
            authorities.add(TransferRatePermission(0, 0))

            // Configure Anonymous user
            if (settings.allowAnonymous) {
                val anonymousUser = BaseUser().apply {
                    name = "anonymous"
                    homeDirectory = rootDir.absolutePath
                    this.authorities = authorities
                    setEnabled(true)
                }
                userManager.save(anonymousUser)
            }

            // Configure Authenticated user
            if (settings.username.isNotBlank()) {
                val authUser = BaseUser().apply {
                    name = settings.username
                    password = settings.password
                    homeDirectory = rootDir.absolutePath
                    this.authorities = authorities
                    setEnabled(true)
                }
                userManager.save(authUser)
            }

            serverFactory.userManager = userManager

            // Configure Ftplet for real-time connection and event monitoring
            val ftplets = HashMap<String, org.apache.ftpserver.ftplet.Ftplet>()
            ftplets["monitorFtplet"] = object : DefaultFtplet() {
                override fun onConnect(session: FtpSession): FtpletResult {
                    val count = activeConnectionCounter.incrementAndGet()
                    val clientIp = session.clientAddress?.address?.hostAddress ?: "Unknown"
                    log("Client connected from $clientIp (Active: $count)", LogLevel.INFO, "CONNECT")
                    updateActiveConnections(count)
                    return FtpletResult.DEFAULT
                }

                override fun onDisconnect(session: FtpSession): FtpletResult {
                    val count = activeConnectionCounter.decrementAndGet().coerceAtLeast(0)
                    val clientIp = session.clientAddress?.address?.hostAddress ?: "Unknown"
                    log("Client disconnected: $clientIp (Active: $count)", LogLevel.INFO, "DISCONNECT")
                    updateActiveConnections(count)
                    return FtpletResult.DEFAULT
                }

                override fun onLogin(session: FtpSession, request: FtpRequest): FtpletResult {
                    val user = session.user?.name ?: "anonymous"
                    log("User '$user' logged in successfully", LogLevel.SUCCESS, "AUTH")
                    return FtpletResult.DEFAULT
                }

                override fun onUploadStart(session: FtpSession, request: FtpRequest): FtpletResult {
                    log("Upload started: ${request.argument}", LogLevel.INFO, "STOR")
                    return FtpletResult.DEFAULT
                }

                override fun onUploadEnd(session: FtpSession, request: FtpRequest): FtpletResult {
                    log("Upload completed: ${request.argument}", LogLevel.SUCCESS, "STOR")
                    return FtpletResult.DEFAULT
                }

                override fun onDownloadStart(session: FtpSession, request: FtpRequest): FtpletResult {
                    log("Download started: ${request.argument}", LogLevel.INFO, "RETR")
                    return FtpletResult.DEFAULT
                }

                override fun onDownloadEnd(session: FtpSession, request: FtpRequest): FtpletResult {
                    log("Download completed: ${request.argument}", LogLevel.SUCCESS, "RETR")
                    return FtpletResult.DEFAULT
                }

                override fun onDeleteStart(session: FtpSession, request: FtpRequest): FtpletResult {
                    log("Deleted file/folder: ${request.argument}", LogLevel.WARNING, "DELE")
                    return FtpletResult.DEFAULT
                }
            }
            serverFactory.ftplets = ftplets

            val server = serverFactory.createServer()
            server.start()
            ftpServer = server

            _serverInfo.value = ServerInfo(
                status = ServerStatus.RUNNING,
                ipAddress = ip,
                port = settings.port,
                rootPath = rootDir.absolutePath,
                isAnonymousAllowed = settings.allowAnonymous,
                isWriteAllowed = settings.allowWrite,
                activeConnections = 0,
                startedAt = System.currentTimeMillis()
            )

            log("FTP Server successfully started at ftp://$ip:${settings.port}", LogLevel.SUCCESS)
            log("Root directory: ${rootDir.absolutePath}", LogLevel.INFO)
            return true
        } catch (e: Exception) {
            log("Failed to start FTP Server: ${e.localizedMessage}", LogLevel.ERROR)
            _serverInfo.value = ServerInfo(
                status = ServerStatus.ERROR,
                errorMessage = e.localizedMessage
            )
            return false
        }
    }

    private fun updateActiveConnections(count: Int) {
        _serverInfo.value = _serverInfo.value.copy(activeConnections = count)
    }

    @Synchronized
    fun stop() {
        if (ftpServer == null || ftpServer!!.isStopped) {
            _serverInfo.value = _serverInfo.value.copy(status = ServerStatus.STOPPED)
            return
        }

        try {
            log("Stopping FTP Server...", LogLevel.INFO)
            ftpServer?.stop()
            ftpServer = null
            activeConnectionCounter.set(0)
            _serverInfo.value = ServerInfo(
                status = ServerStatus.STOPPED,
                activeConnections = 0
            )
            log("FTP Server stopped", LogLevel.INFO)
        } catch (e: Exception) {
            log("Error stopping FTP Server: ${e.localizedMessage}", LogLevel.ERROR)
            _serverInfo.value = _serverInfo.value.copy(
                status = ServerStatus.STOPPED,
                errorMessage = e.localizedMessage
            )
        }
    }

    fun isRunning(): Boolean {
        return ftpServer != null && !ftpServer!!.isStopped && !ftpServer!!.isSuspended
    }
}
