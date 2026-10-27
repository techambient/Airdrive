package com.example.server

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.example.data.LogLevel

class MdnsAdvertiser(private val context: Context) {

    companion object {
        private const val TAG = "MdnsAdvertiser"
        const val SERVICE_TYPE = "_airdrive._tcp."
        const val STANDARD_FTP_TYPE = "_ftp._tcp."
        const val DEFAULT_SERVICE_NAME = "AirDrive-FTP"
    }

    private val nsdManager: NsdManager? by lazy {
        context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    }

    private var multicastLock: WifiManager.MulticastLock? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var isRegistered = false

    fun startAdvertising(port: Int, customName: String? = null) {
        if (isRegistered) {
            stopAdvertising()
        }

        try {
            // Acquire MulticastLock
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("AirDriveMdnsLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }

            val serviceName = customName ?: "${Build.MODEL}-$DEFAULT_SERVICE_NAME"
            val serviceInfo = NsdServiceInfo().apply {
                this.serviceName = serviceName
                this.serviceType = SERVICE_TYPE
                this.port = port
            }

            registrationListener = object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(registeredServiceInfo: NsdServiceInfo) {
                    isRegistered = true
                    val name = registeredServiceInfo.serviceName
                    Log.i(TAG, "mDNS Service successfully registered: $name on port $port ($SERVICE_TYPE)")
                    FtpServerManager.getInstance().log(
                        "mDNS advertised as '$name' on $SERVICE_TYPE:$port",
                        LogLevel.SUCCESS,
                        "mDNS"
                    )
                }

                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    isRegistered = false
                    Log.e(TAG, "mDNS Service registration failed: error code $errorCode")
                    FtpServerManager.getInstance().log(
                        "mDNS registration failed (error $errorCode)",
                        LogLevel.WARNING,
                        "mDNS"
                    )
                }

                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                    isRegistered = false
                    Log.i(TAG, "mDNS Service unregistered successfully")
                    FtpServerManager.getInstance().log("mDNS service unregistered", LogLevel.INFO, "mDNS")
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    isRegistered = false
                    Log.e(TAG, "mDNS Service unregistration failed: error code $errorCode")
                }
            }

            nsdManager?.registerService(
                serviceInfo,
                NsdManager.PROTOCOL_DNS_SD,
                registrationListener
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error starting mDNS advertising", e)
            FtpServerManager.getInstance().log("Failed to start mDNS: ${e.localizedMessage}", LogLevel.ERROR, "mDNS")
        }
    }

    fun stopAdvertising() {
        if (!isRegistered && registrationListener == null) return

        try {
            registrationListener?.let { listener ->
                nsdManager?.unregisterService(listener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during mDNS unregistration", e)
        } finally {
            registrationListener = null
            isRegistered = false
            try {
                if (multicastLock?.isHeld == true) {
                    multicastLock?.release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            multicastLock = null
        }
    }
}
