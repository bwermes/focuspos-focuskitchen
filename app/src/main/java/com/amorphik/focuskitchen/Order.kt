package com.amorphik.focuskitchen

import com.google.gson.annotations.SerializedName

class Order (val printerId: Int,
             val checkId: Int?,
             val printOrderSessionKey: String?,
             val printerName: String,
             val revenueCenter: String,
             val orderType: String,
             val server: String,
             val serverNotOwner: String,
             val time: String,
             val date: String,
             val station: String,
             val table: String,
             val check: String,
             val guests: Int,
             val delayTime: Int,
             val orderDisplayTime: String,
             val customerName: String,
             val items: Array<OrderItem>,
             @SerializedName("orderReadySmsCount") var smsCount: Int? = 0,
            @SerializedName("orderReadySms") var orderReadySms: String? = null,
             @SerializedName("orderHeaderColor") var orderHeaderColor: String? = null
)

{

    var bumpTime : String = "0:00:00"
    var key: String = ""


}