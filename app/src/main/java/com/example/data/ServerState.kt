package com.example.data

enum class ServerStatus {
    STOPPED,
    STARTING,
    RUNNING,
    ERROR
}

data class ServerInfo(
    val status: ServerStatus = ServerStatus.STOPPED,
    val ipAddress: String? = null,
    val port: Int = 2121,
    val rootPath: String = "/storage/emulated/0",
    val isAnonymousAllowed: Boolean = true,
    val isWriteAllowed: Boolean = true,
    val isMdnsBroadcasting: Boolean = false,
    val activeConnections: Int = 0,
    val errorMessage: String? = null,
    val startedAt: Long? = null
) {
    val ftpUrl: String?
        get() = if (ipAddress != null && status == ServerStatus.RUNNING) "ftp://$ipAddress:$port" else null
}

data class WifiConnectionState(
    val isConnected: Boolean = false,
    val ssid: String? = null,
    val bssid: String? = null,
    val ipAddress: String? = null,
    val isTrusted: Boolean = false
)

enum class LogLevel {
    INFO, SUCCESS, WARNING, ERROR
}

data class ActivityLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val message: String,
    val level: LogLevel = LogLevel.INFO,
    val tag: String? = null
)
