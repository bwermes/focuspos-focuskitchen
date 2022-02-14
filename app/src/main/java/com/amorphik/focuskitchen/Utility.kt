package com.amorphik.focuskitchen

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import java.lang.Exception

object Utility {
    public fun appConnectivityCheck(context: Context, credentials: DeviceCredentials, connectionDownMinutes: Int, connectionDrops: Int): Boolean{
        if(osNetworkCheck(context)){
            try{
                focusLinkNetworkCheck(credentials, connectionDownMinutes, connectionDrops)
                            Log.d("heartbeat","Successful")
                return true
            }catch(e: Exception){
                            Log.d("heartbeat","Failed: ${e.message}")
                return false
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

    public fun focusLinkNetworkCheck(credentials: DeviceCredentials, connectionDownMinutes: Int, connectionDrops: Int){
        api!!.postStatus(prefs.venueKey, prefs.license!!.key!!,
            LicenseStatus(
                key = prefs.license!!.key!!,
                connectionDownMinutes = connectionDownMinutes,
                connectionDrops = connectionDrops))
//        try{
//
//            Log.d("heartbeat","Successful")
//            return true
//        }catch(e: Exception){
//            Log.d("heartbeat","Failed: ${e.message}")
//            return false
//        }
    }
}