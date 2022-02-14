package com.amorphik.focuskitchen

import android.content.Context
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiService {

    @GET("v3/stores/{venueKey}/pos/checks/{checkKey}")
    suspend fun getCheck(
        @Path("venueKey") venueKey: Int,
        @Path("checkKey") checkKey: String
    ):
            CheckDto

    @GET("v3/stores/{venueKey}/pos/checks")
    suspend fun getChecks(
        @Path("venueKey") venueKey: Int,
        @Query("open") open: Boolean
    ):
            ArrayList<CheckDto>

    @POST("v2/stores/{venueKey}/kitchen/device/{licenseKey}/checkin")
    fun postStatus(
        @Path("venueKey") venueKey: Int,
        @Path("licenseKey") licenseKey: String,
        @Body body: LicenseStatus
    ): Call<LicenseStatus>

    companion object {
        fun create(ctx: Context): ApiService {
            var baseUrl = ctx.resources.getString(R.string.api_url_prod)
            if (prefs.mode == "dev") {
                baseUrl = ctx.resources.getString(R.string.api_url_dev)
            }

            val headerAuthorizationInterceptor = Interceptor { chain ->
                var request = chain.request()

                request =
                    if (prefs.authToken.isNotBlank() && request.header("AppInternal-NoAuth") == null) {
                        request
                            .newBuilder()
                            .header("Authorization", "hmac " + prefs.authToken)
                            .build()
                    } else {
                        request
                            .newBuilder()
                            .removeHeader("AppInternal-NoAuth")
                            .build()
                    }

//                if (request.header("AppInternal-LogRequest") != null) {
//                    Log.d(TAG, "Logging request to URL: " + request.url().toString());
//                }

                chain.proceed(request)
            }

            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(headerAuthorizationInterceptor)
                .addInterceptor(logging) // add logging as last interceptor
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(
                    GsonConverterFactory.create()
                )
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}