package com.amorphik.focuskitchen

import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.StringBufferInputStream
import java.util.concurrent.TimeUnit

interface LogglyService {
    @POST("/inputs/fd42b91a-d06a-4585-84d5-ec3a9e1cbcc7/tag/focuskitchen")
    suspend fun log(@Body body: LogglyBody)

    companion object {
        fun create(ctx: Context): LogglyService {
            var baseUrl = "https://logs-01.loggly.com"

            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging) // add logging as last interceptor
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(
                    GsonConverterFactory.create()
                )
                .build()

            return retrofit.create(LogglyService::class.java)
        }
    }
}

data class LogglyBody(
    val level: String = "info",
    val message: String,
    val payload: String? = null,
    val checkNumber: String? = null,
    val errorContext: String? = null,
    val appVersionCode: Int = BuildConfig.VERSION_CODE,
    val appVersionName: String = BuildConfig.VERSION_NAME,
    val deviceType: String = Build.MODEL,
    val licenseKey: String? = deviceLicenseKey,
    val venueKey: String? = com.amorphik.focuskitchen.venueKey,
    val printerId: String? = com.amorphik.focuskitchen.printerId
)