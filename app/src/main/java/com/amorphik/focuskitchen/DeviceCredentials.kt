package com.amorphik.focuskitchen

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import java.net.NetworkInterface
import java.util.*

class DeviceCredentials (val prefs: SharedPreferences, val context: Context) {
    // Device specific info
    var macAddress = ""
    var ipAddress = ""
    var venueKey = ""
    var deviceName = ""
    var licenseKey = ""
    var licenseSecret = ""
    var mode = ""
    var baseApiUrl = "https://focuslink.focuspos.com/v2/"
    var baseWsUrl = "https://ws.focuslink.focuspos.com/"
    var printerNum = ""
    var urgentTime = Int.MAX_VALUE

    // Constant keys used to access device saved information
    val PREFS_LICENSE_KEY = "licenseKey"
    val PREFS_MAC_ADDR = "mac"
    val PREFS_IP_ADDR = "ip"
    val PREFS_LICENSE_SECRET = "licenseSecret"
    val PREFS_DEVICE_NAME = "deviceName"
    val PREFS_VENUE_KEY = "venueKey"
    val PREFS_MODE_KEY = "deviceMode"
    val PREFS_ORDERS_KEY = "savedOrders"
    val PREFS_DELAYED_KEY = "delayedOrders"

    init {
        ipAddress = gatherIpAddress()

        venueKey = prefs.getString(PREFS_VENUE_KEY, "")!!
        deviceName = prefs.getString(PREFS_DEVICE_NAME, "")!!
        licenseKey = prefs.getString(PREFS_LICENSE_KEY, "")!!
        licenseSecret = prefs.getString(PREFS_LICENSE_SECRET, "")!!
        mode = prefs.getString(PREFS_MODE_KEY, "prod")!!
        macAddress = prefs.getString(PREFS_MAC_ADDR, generateFauxMac())!!
    }

    fun generateFauxMac() : String {
        // Using the ANDOID_ID allows re-registration of device if it was uninstalled/reinstalled.
        // This prevents releasing the license in the back end and going through registration
        // process again.
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    private fun gatherIpAddress() : String {
        try {
            val networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (netInterface in networkInterfaces) {
                // Loop until we find wlan0
                val addresses = Collections.list(netInterface.inetAddresses)
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress) {
                        val stringAddress = addr.hostAddress
                        val isIPv4 = !stringAddress.contains(":")

                        if (isIPv4) return stringAddress
                    }
                }
            }
        } catch (ex: Exception) {
        }

        return "0.0.0.0"
    }

    fun generateIntegratorHeader() : String {
        // Header value used to access registration endpoints
        val apiID = "43beb5d1-40cd-48fe-8f3a-378d4d43d821"
        val apiSecret = "5bb6fc0b-9195-4ccf-9528-b3433163fa6b"
        val signature = AuthGenerator.generateHash(apiID, apiSecret)

        return "hmac $apiID:$signature"
    }

    fun generateDeviceLicenseHeader() : String {
        // Header value used to access post registration endpoints
        val signature = AuthGenerator.generateHash("$licenseKey:$venueKey:$macAddress", licenseSecret)

        return "hmac $licenseKey:$signature"
    }
}