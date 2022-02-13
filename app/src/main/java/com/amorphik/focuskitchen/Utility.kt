package com.amorphik.focuskitchen

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Utility {
    public fun appConnectivityCheck(context: Context, credentials: DeviceCredentials): Boolean{
        if(osNetworkCheck(context)){
            if(focusLinkNetworkCheck(credentials)){
                return true
            }
        }
        return false
    }

    public fun osNetworkCheck(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        }else{
            @Suppress("DEPRECATION") val networkInfo = connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION") return networkInfo.isConnected
        }
    }

    public fun focusLinkNetworkCheck(credentials: DeviceCredentials): Boolean{
        val url = "${credentials.baseApiUrl}stores/${credentials.venueKey}/kitchen/device/${credentials.licenseKey}/checkin"
        val headerName = "Authorization"
        val headerBody = credentials.generateDeviceLicenseHeader()
        val response = Networking.getSynchronous(url = url, headerName = headerName, headerValue = headerBody)
        if(response.isSuccessful){
            return true
        }
        return false
    }
}