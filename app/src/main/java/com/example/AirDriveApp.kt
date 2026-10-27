package com.example

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Environment
import com.example.data.LogLevel
import com.example.data.SettingsRepository
import com.example.network.NetworkHelper
import com.example.server.FtpServerManager
import com.example.service.FtpService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AirDriveApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .build()

            connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    applicationScope.launch {
                        handleWifiConnected()
                    }
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    applicationScope.launch {
                        handleWifiLost()
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun handleWifiConnected() {
        val ftpManager = FtpServerManager.getInstance()
        val settings = settingsRepository.getSettings()

        if (!settings.autoStartOnWifi) {
            return
        }

        val ssid = NetworkHelper.getWifiSsid(this)
        
        // Trusted network check
        if (settings.onlyTrustedNetworks && settings.trustedNetworks.isNotEmpty()) {
            if (ssid != null && !settings.trustedNetworks.contains(ssid)) {
                ftpManager.log("Wi-Fi network '$ssid' is not trusted. Auto-start skipped.", LogLevel.INFO, "WIFI")
                return
            }
        }

        // Storage permission check
        val hasStorage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }

        if (!hasStorage) {
            ftpManager.log("Auto-start skipped: Storage permission not granted.", LogLevel.WARNING, "STORAGE")
            return
        }

        if (!ftpManager.isRunning()) {
            ftpManager.log("Wi-Fi connected. Auto-starting FTP service...", LogLevel.SUCCESS, "WIFI")
            FtpService.startService(this@AirDriveApp)
        }
    }

    private fun handleWifiLost() {
        val ftpManager = FtpServerManager.getInstance()
        if (ftpManager.isRunning()) {
            ftpManager.log("Wi-Fi disconnected. Stopping FTP service...", LogLevel.INFO, "WIFI")
            FtpService.stopService(this@AirDriveApp)
        }
    }
}
