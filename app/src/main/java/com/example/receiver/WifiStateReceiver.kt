package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import com.example.data.LogLevel
import com.example.data.SettingsRepository
import com.example.network.NetworkHelper
import com.example.server.FtpServerManager
import com.example.service.FtpService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WifiStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleNetworkChange(context, action)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleNetworkChange(context: Context, action: String) {
        val settingsRepo = SettingsRepository(context.applicationContext)
        val settings = settingsRepo.getSettings()
        val isWifi = NetworkHelper.isWifiConnected(context)
        val ftpManager = FtpServerManager.getInstance()

        ftpManager.log("Network state event received: $action (Wi-Fi connected: $isWifi)", LogLevel.INFO, "NETWORK")

        if (isWifi) {
            if (!settings.autoStartOnWifi) {
                ftpManager.log("Wi-Fi connected, but Auto-Start is disabled in settings.", LogLevel.INFO, "AUTO-START")
                return
            }

            val currentSsid = NetworkHelper.getWifiSsid(context)
            
            // Check trusted network constraints
            if (settings.onlyTrustedNetworks && settings.trustedNetworks.isNotEmpty()) {
                if (currentSsid == null) {
                    ftpManager.log("Wi-Fi connected, but cannot read SSID (Location permission might be needed). Checking trusted network policy.", LogLevel.WARNING, "TRUSTED")
                } else if (!settings.trustedNetworks.contains(currentSsid)) {
                    ftpManager.log("Connected to '$currentSsid', which is NOT in trusted networks list. Auto-start skipped.", LogLevel.WARNING, "TRUSTED")
                    return
                }
            }

            // Check storage access
            val hasStorageAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true
            }

            if (!hasStorageAccess) {
                ftpManager.log("Auto-start skipped: MANAGE_EXTERNAL_STORAGE permission required.", LogLevel.WARNING, "STORAGE")
                return
            }

            if (!ftpManager.isRunning()) {
                ftpManager.log("Auto-starting FTP Server on Wi-Fi connection ($currentSsid)...", LogLevel.SUCCESS, "AUTO-START")
                FtpService.startService(context)
            }
        } else {
            // Disconnected from Wi-Fi
            if (ftpManager.isRunning()) {
                ftpManager.log("Wi-Fi disconnected. Automatically stopping FTP Server...", LogLevel.INFO, "AUTO-STOP")
                FtpService.stopService(context)
            }
        }
    }
}
