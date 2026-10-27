package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.LogLevel
import com.example.data.ServerStatus
import com.example.data.SettingsRepository
import com.example.network.NetworkHelper
import com.example.server.FtpServerManager
import com.example.server.MdnsAdvertiser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FtpService : Service() {

    companion object {
        const val ACTION_START = "com.example.action.START_FTP"
        const val ACTION_STOP = "com.example.action.STOP_FTP"
        private const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "airdrive_ftp_channel"

        fun startService(context: Context) {
            val intent = Intent(context, FtpService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FtpService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var settingsRepository: SettingsRepository
    private val ftpServerManager = FtpServerManager.getInstance()
    private lateinit var mdnsAdvertiser: MdnsAdvertiser

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(applicationContext)
        mdnsAdvertiser = MdnsAdvertiser(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                ftpServerManager.log("Stop requested from system/notification", LogLevel.INFO)
                stopFtpServer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                startFtpServer()
            }
        }

        return START_STICKY
    }

    private fun startFtpServer() {
        serviceScope.launch(Dispatchers.IO) {
            val settings = settingsRepository.getSettings()
            val ipAddress = NetworkHelper.getLocalIpAddress() ?: "127.0.0.1"
            val ssid = NetworkHelper.getWifiSsid(applicationContext) ?: "Wi-Fi"

            // Acquire locks if enabled
            if (settings.enableWakeLock) {
                acquireLocks()
            }

            // Start foreground notification immediately
            val initialNotification = buildNotification(
                url = "ftp://$ipAddress:${settings.port}",
                ssid = ssid,
                clients = 0
            )

            launch(Dispatchers.Main) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        initialNotification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(NOTIFICATION_ID, initialNotification)
                }
            }

            // Start FTP Server
            val success = ftpServerManager.start(settings, ipAddress)
            if (success) {
                // Advertise via mDNS
                if (settings.advertiseMdns) {
                    mdnsAdvertiser.startAdvertising(settings.port)
                }

                // Monitor connection changes to update notification
                launch {
                    ftpServerManager.serverInfo.collectLatest { info ->
                        if (info.status == ServerStatus.RUNNING) {
                            val updatedNotification = buildNotification(
                                url = "ftp://${info.ipAddress}:${info.port}",
                                ssid = NetworkHelper.getWifiSsid(applicationContext) ?: "Wi-Fi",
                                clients = info.activeConnections
                            )
                            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.notify(NOTIFICATION_ID, updatedNotification)
                        }
                    }
                }
            } else {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun stopFtpServer() {
        mdnsAdvertiser.stopAdvertising()
        ftpServerManager.stop()
        releaseLocks()
    }

    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AirDriveFtp::WakeLock"
            )?.apply {
                setReferenceCounted(false)
                acquire(12 * 60 * 60 * 1000L) // 12 hours max
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val lockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            } else {
                @Suppress("DEPRECATION")
                WifiManager.WIFI_MODE_FULL
            }
            wifiLock = wifiManager?.createWifiLock(lockMode, "AirDriveFtp::WifiLock")?.apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun releaseLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null

            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
            wifiLock = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AirDrive FTP Server Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live FTP Server status and quick stop controls"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(url: String, ssid: String, clients: Int): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FtpService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val clientText = if (clients > 0) " • $clients client${if (clients > 1) "s" else ""}" else ""

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FTP Server Running ($ssid)")
            .setContentText("$url$clientText")
            .setSubText("AirDrive FTP")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Server",
                stopPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    override fun onDestroy() {
        stopFtpServer()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
