package com.example.network

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkHelper {

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            // First check wlan interfaces
            for (intf in interfaces) {
                if (intf.name.startsWith("wlan", ignoreCase = true) || intf.name.startsWith("ap", ignoreCase = true)) {
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val hostAddress = addr.hostAddress
                            if (!hostAddress.isNullOrEmpty() && hostAddress != "127.0.0.1") {
                                return hostAddress
                            }
                        }
                    }
                }
            }
            // Fallback to any active non-loopback IPv4 interface
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val hostAddress = addr.hostAddress
                        if (!hostAddress.isNullOrEmpty() && hostAddress != "127.0.0.1") {
                            return hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun getWifiSsid(context: Context): String? {
        if (!isWifiConnected(context)) return null
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        
        try {
            // Check if location permission is granted (needed on Android 8.1+ for SSID)
            val hasLocationPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val wifiInfo: WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val network = cm?.activeNetwork
                val caps = cm?.getNetworkCapabilities(network)
                (caps?.transportInfo as? WifiInfo) ?: wifiManager.connectionInfo
            } else {
                wifiManager.connectionInfo
            }

            val rawSsid = wifiInfo?.ssid
            if (rawSsid != null && rawSsid != "<unknown ssid>" && rawSsid != "0x") {
                return rawSsid.removeSurrounding("\"").trim()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
