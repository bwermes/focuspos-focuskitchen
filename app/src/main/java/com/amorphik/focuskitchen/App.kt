package com.amorphik.focuskitchen

import android.app.Application
import android.util.Log
import android.view.View.*

var api: ApiService? = null

var loggly: LogglyService? = null

val prefs: Prefs by lazy {
    FocusKitchen.prefs!!
}

val session: Session by lazy {
    FocusKitchen.session!!
}

val uiFlags = SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
        SYSTEM_UI_FLAG_FULLSCREEN or
        SYSTEM_UI_FLAG_LAYOUT_STABLE or
        SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
        SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

class FocusKitchen : Application() {
    companion object {
        var prefs: Prefs? = null
        var session: Session? = null
    }

    override fun onCreate() {
        Log.d("prefs","init")
        prefs = Prefs(applicationContext)
        session = Session()
        api = ApiService.create(applicationContext)
        loggly = LogglyService.create(applicationContext)
        super.onCreate()
    }
}