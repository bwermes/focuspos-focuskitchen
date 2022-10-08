package com.amorphik.focuskitchen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import com.google.android.things.device.DeviceManager
import java.lang.IllegalStateException
import java.lang.RuntimeException
import androidx.core.content.ContextCompat.getSystemService
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager

import android.widget.Toast
import androidx.fragment.app.Fragment
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit
import android.content.pm.PackageManager





object Utility {
    public fun appConnectivityCheck(
        context: Context,
        credentials: DeviceCredentials,
        connectionDownMinutes: Int,
        connectionDrops: Int
    ): Boolean {
        if (osNetworkCheck(context)) {
            try {
                focusLinkNetworkCheck(credentials, connectionDownMinutes, connectionDrops)
                Logger.d("heartbeat", "Successful")
                return true
            } catch (e: Exception) {
                Logger.d("heartbeat", "Failed: ${e.message}")
                return false
            }


        }
        return false
    }

    fun clearLicenseInfo(credentials: DeviceCredentials) {

        // Null out the saved license details
        val editor = credentials.sharedPreferences.edit()
        editor.putString(credentials.PREFS_LICENSE_KEY, null)
        editor.putString(credentials.PREFS_VENUE_KEY, null)
        editor.putString(credentials.PREFS_MAC_ADDR, null)
        editor.putString(credentials.PREFS_IP_ADDR, null)
        editor.putString(credentials.PREFS_DEVICE_NAME, null)
        editor.putString(credentials.PREFS_LICENSE_SECRET, null)
        editor.apply()

        // Clear the properties in memory
        credentials.venueKey = ""
        credentials.deviceName = ""
        credentials.licenseKey = ""
        credentials.licenseSecret = ""
        credentials.macAddress = credentials.generateFauxMac()
        credentials.mode = "prod"
    }



    public fun osNetworkCheck(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION") return networkInfo.isConnected
        }
    }

    fun focusLinkNetworkCheck(
        credentials: DeviceCredentials,
        connectionDownMinutes: Int,
        connectionDrops: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                try {
                    val response = api!!.postStatus(
                        prefs.venueKey, prefs.license!!.key!!,
                        LicenseStatus(
                            key = prefs.license!!.key!!,
                            connectionDownMinutes = connectionDownMinutes,
                            connectionDrops = connectionDrops
                        )
                    )
                    Logger.d("heartbeat", "${response}")
                    if (credentials.deviceName != response.name) {
                        credentials.deviceName = response.name.toString()
                    } else {
                    }
                    if (credentials.printerNum != response.deviceId?.toString()) {
                        credentials.printerNum = response.deviceId.toString()
                    } else {
                    }

                } catch (e: Exception) {
                    Logger.d("heartbeat", "Error ${e.message}")
                }

            }
        }
    }

    fun Fragment.hideKeyboard() {
        view?.let { activity?.hideKeyboard(it) }
    }

    fun Activity.hideKeyboard() {
        hideKeyboard(currentFocus ?: View(this))
    }

    fun Context.hideKeyboard(view: View) {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun submitPosPropertyChange(posConfigurationChangeType: PosConfigurationChangeType, itemIdentifier: String, value: Any){
        val posConfigurationModel = PosConfigurationDataModel(itemIdentifier, value.toString(), posConfigurationChangeType.changeType)
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                api!!.postPosConfigurationChange(prefs.venueKey,posConfigurationModel)

            }
        }
    }

    fun notification(notificationText: String){
        val toast = Toast.makeText(
            FocusKitchenApplication.appContext,
            notificationText, Toast.LENGTH_SHORT
        )

        toast.show()
    }
}


