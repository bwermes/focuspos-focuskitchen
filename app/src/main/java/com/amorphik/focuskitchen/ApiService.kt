package com.amorphik.focuskitchen

import android.content.Context
import android.util.Log.d
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
    suspend fun postStatus(
        @Path("venueKey") venueKey: Int,
        @Path("licenseKey") licenseKey: String,
        @Body body: LicenseStatus
    ): LicenseStatus

    @GET("v2/stores/{venueKey}/pos/menus")
    suspend fun getMenu(
        @Path("venueKey") venueKey: Int,
        @Query("source") source: String = "dyn"
    ): MutableList<MenuItemRecord>


    @GET("v2/stores/{venueKey}/data/config/reportgroups")
    suspend fun getReportGroups(
        @Path("venueKey") venueKey: Int
    ): Array<ReportGroupRecord>

    @GET("v4/stores/{venueKey}/pos/checks/items/{menuItemKey}")
    suspend fun itemSales(
        @Path("venueKey") venueKey: Int,
        @Path("menuItemKey") menuItemKey: Int,
        @Query("days") days: Int = 7

    ): MutableList<MenuItemSalesRecord>

    @POST("v2/stores/{venueKey}/pos/configuration")
    suspend fun postPosConfigurationChange(
        @Path("venueKey") venueKey: Int,
        @Body payload: PosConfigurationDataModel
    ):PosConfigurationResponseDataModel

    @POST("v2/stores/{venueKey}/printorders/{printOrderKey}/print")
    suspend fun printPrintOrder(
        @Path("venueKey") venueKey: Int,
        @Path("printOrderKey") printOrderKey: String
    ):FocusLinkApiCommandResponse

    @POST("v2/stores/{venueKey}/printorders/{printOrderKey}/sms")
    suspend fun smsPrintOrder(
        @Path("venueKey") venueKey: Int,
        @Path("printOrderKey") printOrderKey: String
    ): Order

    @POST("v2/utility/{venueKey}/log/{level}")
    suspend fun logToFocusLink(
        @Path("venueKey") venueKey: Int,
        @Path("level") level: String = "Error",
        @Body payload: LogPayload
    )

    companion object {
        fun create(ctx: Context): ApiService {
            var baseUrl = ctx.resources.getString(R.string.api_url_prod)
            if (prefs.mode == "dev") {

                baseUrl = ctx.resources.getString(R.string.api_url_dev)
            }

            Logger.d("apiService","baseUrl ${baseUrl}")
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
//                    Logger.d(TAG, "Logging request to URL: " + request.url().toString());
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