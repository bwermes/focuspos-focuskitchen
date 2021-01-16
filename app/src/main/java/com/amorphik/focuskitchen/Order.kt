package com.amorphik.focuskitchen

class Order (val printerId: Int,
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
             val items: Array<OrderItem>) {

    var bumpTime : String = "0:00:00"
    var key: String = ""


}