package com.example.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActivityLog
import com.example.data.AppSettings
import com.example.data.LogLevel
import com.example.data.ServerInfo
import com.example.data.ServerStatus
import com.example.data.SettingsRepository
import com.example.data.WifiConnectionState
import com.example.network.NetworkHelper
import com.example.server.FtpServerManager
import com.example.service.FtpService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FtpViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    private val ftpServerManager = FtpServerManager.getInstance()

    val settings: StateFlow<AppSettings> = settingsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    val serverInfo: StateFlow<ServerInfo> = ftpServerManager.serverInfo

    val activityLogs: StateFlow<List<ActivityLog>> = ftpServerManager.activityLogs

    private val _wifiState = MutableStateFlow(WifiConnectionState())
    val wifiState: StateFlow<WifiConnectionState> = _wifiState.asStateFlow()

    private val _hasStoragePermission = MutableStateFlow(checkStoragePermission())
    val hasStoragePermission: StateFlow<Boolean> = _hasStoragePermission.asStateFlow()

    private val _hasLocationPermission = MutableStateFlow(checkLocationPermission())
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission.asStateFlow()

    init {
        refreshState()
        viewModelScope.launch {
            while (true) {
                refreshState()
                delay(3000)
            }
        }
    }

    fun refreshState() {
        val context = getApplication<Application>()
        _hasStoragePermission.value = checkStoragePermission()
        _hasLocationPermission.value = checkLocationPermission()

        val isWifi = NetworkHelper.isWifiConnected(context)
        val ssid = NetworkHelper.getWifiSsid(context)
        val ip = NetworkHelper.getLocalIpAddress()
        val currentSettings = settings.value
        val isTrusted = ssid != null && currentSettings.trustedNetworks.contains(ssid)

        _wifiState.value = WifiConnectionState(
            isConnected = isWifi,
            ssid = ssid,
            ipAddress = ip,
            isTrusted = isTrusted
        )
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val context = getApplication<Application>()
            val readPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            val writePerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            readPerm && writePerm
        }
    }

    private fun checkLocationPermission(): Boolean {
        val context = getApplication<Application>()
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun toggleServer() {
        val currentStatus = serverInfo.value.status
        if (currentStatus == ServerStatus.RUNNING || currentStatus == ServerStatus.STARTING) {
            stopServer()
        } else {
            startServer()
        }
    }

    fun startServer() {
        val context = getApplication<Application>()
        if (!_hasStoragePermission.value) {
            ftpServerManager.log("Cannot start: Storage permission not granted. Please grant storage access.", LogLevel.ERROR)
            return
        }
        FtpService.startService(context)
        refreshState()
    }

    fun stopServer() {
        val context = getApplication<Application>()
        FtpService.stopService(context)
        refreshState()
    }

    fun setAutoStartOnWifi(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setAutoStartOnWifi(enabled)
            ftpServerManager.log("Auto-start on Wi-Fi set to $enabled", LogLevel.INFO)
        }
    }

    fun setOnlyTrustedNetworks(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setOnlyTrustedNetworks(enabled)
            ftpServerManager.log("Require Trusted Networks set to $enabled", LogLevel.INFO)
        }
    }

    fun addCurrentNetworkToTrusted() {
        val ssid = _wifiState.value.ssid ?: return
        viewModelScope.launch {
            settingsRepo.addTrustedNetwork(ssid)
            ftpServerManager.log("Added '$ssid' to trusted Wi-Fi networks", LogLevel.SUCCESS)
            refreshState()
        }
    }

    fun addTrustedNetwork(ssid: String) {
        if (ssid.isBlank()) return
        viewModelScope.launch {
            settingsRepo.addTrustedNetwork(ssid)
            ftpServerManager.log("Added '$ssid' to trusted Wi-Fi networks", LogLevel.SUCCESS)
            refreshState()
        }
    }

    fun removeTrustedNetwork(ssid: String) {
        viewModelScope.launch {
            settingsRepo.removeTrustedNetwork(ssid)
            ftpServerManager.log("Removed '$ssid' from trusted Wi-Fi networks", LogLevel.INFO)
            refreshState()
        }
    }

    fun setPort(port: Int) {
        if (port in 1024..65535) {
            viewModelScope.launch {
                settingsRepo.setPort(port)
                ftpServerManager.log("Port configured to $port (requires server restart to apply)", LogLevel.INFO)
            }
        }
    }

    fun setAllowAnonymous(allow: Boolean) {
        viewModelScope.launch {
            settingsRepo.setAllowAnonymous(allow)
        }
    }

    fun setAllowWrite(allow: Boolean) {
        viewModelScope.launch {
            settingsRepo.setAllowWrite(allow)
        }
    }

    fun setCredentials(username: String, pass: String) {
        viewModelScope.launch {
            settingsRepo.setCredentials(username, pass)
            ftpServerManager.log("User credentials updated", LogLevel.INFO)
        }
    }

    fun setEnableWakeLock(enable: Boolean) {
        viewModelScope.launch {
            settingsRepo.setEnableWakeLock(enable)
        }
    }

    fun setAdvertiseMdns(advertise: Boolean) {
        viewModelScope.launch {
            settingsRepo.setAdvertiseMdns(advertise)
        }
    }

    fun clearLogs() {
        ftpServerManager.clearLogs()
    }
}
