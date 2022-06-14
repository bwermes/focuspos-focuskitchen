package com.amorphik.focuskitchen

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import java.lang.Exception

class Prefs(context: Context) {

    val PREFS_FILENAME = "com.amorphik.focuskitchen.prefs"
    var deviceThemeMode: Int? = null;
    val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0);

    var authToken: String
        get() = prefs.getString("auth_token", "")!!
        set(value) = prefs.edit().putString("auth_token", value).apply()

    var defaultPrinterId: Int
        get() = prefs.getInt("default_printer_id", 0)
        set(value) = prefs.edit().putInt("default_printer_id", value).apply()

    var deviceId: String
        get() = prefs.getString("device_id", "")!!
        set(value) = prefs.edit().putString("device_id", value).apply()

    var lastEmployeeId: Int
        get() = prefs.getInt("last_employee_id", 0)
        set(value) = prefs.edit().putInt("last_employee_id", value).apply()

    var license: License?
        get() {
            val json = prefs.getString("license", "")
            return if (json!!.isNotEmpty()) {
                Gson().fromJson(json, License::class.java)
            } else {
                null
            }
        }
        set(value) {
            if (value != null) {
                prefs.edit().putString("license", Gson().toJson(value)).apply()
            } else {
                prefs.edit().remove("license").apply()
            }
        }

    var licenseVerified: Boolean
        get() = prefs.getBoolean("license_verified", false)
        set(value) = prefs.edit().putBoolean("license_verified", value).apply()

    var mode: String
        get() = prefs.getString("mode", "")!!
        set(value) = prefs.edit().putString("mode", value).apply()

    var tip1: Int
        get() = prefs.getInt("tip_1", 0)
        set(value) = prefs.edit().putInt("tip_1", value).apply()

    var tip2: Int
        get() = prefs.getInt("tip_2", 0)
        set(value) = prefs.edit().putInt("tip_2", value).apply()

    var tip3: Int
        get() = prefs.getInt("tip_3", 0)
        set(value) = prefs.edit().putInt("tip_3", value).apply()

    var venueKey: Int
        get() {
            var value: Int = 0

            try {
                value = prefs.getInt("venue_key", 0)
            } catch(e: Exception) {
                try {
                    val valueStr = prefs.getString("venue_key", "")
                    if (!valueStr.isNullOrEmpty()) {
                        value = valueStr.toInt()
                    }
                } catch(e: Exception) {
                }
            }

            return value
        }
        set(value) = prefs.edit().putInt("venue_key", value).apply()

    var venueName: String
        get() = prefs.getString("venue_name", "")!!
        set(value) = prefs.edit().putString("venue_name", value).apply()

    var venuePreferences: VenuePreference?
        get() {
            val json = prefs.getString("venuePreferences", "")
            return if (json!!.isNotEmpty()) {
                Gson().fromJson(json, VenuePreference::class.java)
            } else {
                null
            }
        }
        set(value) = prefs.edit().putString("venuePreferences", Gson().toJson(value)).apply()

    var licenseFeatures: LicenseFeatures?
        get(){
            val json = prefs.getString("licenseFeatures","")
            return if(json!!.isNotEmpty()){
                Gson().fromJson(json, LicenseFeatures::class.java)
            } else{
                return null
            }
        }
    set(value) = prefs.edit().putString("licenseFeatures", Gson().toJson(value)).apply()
}