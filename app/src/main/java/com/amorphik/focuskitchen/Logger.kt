package com.amorphik.focuskitchen

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


object Logger {
    fun d(tag: String, msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg)
        }
    }

    fun e(tag: String, msg: String, toLoggly: Boolean) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, msg)
            if(toLoggly){
                toLoggly(tag, msg, true)
            }

        }
    }

    fun toLoggly(message: String, payload: String, isError: Boolean){
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                try{
                    loggly!!.log(
                        LogglyBody(
                            if(isError) "error" else "info",
                            message,
                            payload,
                            null
                        )
                    )
                }
                catch(e: Exception){}
            }
        }
    }
}