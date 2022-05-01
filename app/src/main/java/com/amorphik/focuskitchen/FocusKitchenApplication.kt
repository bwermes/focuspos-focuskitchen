package com.amorphik.focuskitchen

import android.app.Application
import android.content.Context
import android.content.res.Resources

class FocusKitchenApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        FocusKitchenApplication.appContext = applicationContext
        res = resources
    }
    companion object {

        lateinit  var appContext: Context
        lateinit var res: Resources

    }
}