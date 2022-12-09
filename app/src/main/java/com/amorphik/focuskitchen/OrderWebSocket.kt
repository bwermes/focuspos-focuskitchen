package com.amorphik.focuskitchen

import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import com.amorphik.focuskitchen.allDayCount.AllDayCountAdapter
import com.beust.klaxon.Klaxon
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.emitter.Emitter
//import nl.dionsegijn.konfetti.compose.KonfettiView
//import nl.dionsegijn.konfetti.models.Size
import okhttp3.*
import okio.ByteString
import java.lang.reflect.Modifier
import java.util.concurrent.TimeUnit

class OrderWebSocket(private val credentials: DeviceCredentials,
                     private val adapter: OrderAdapter,
                     private val context: Context,
                     private val mainActivity: MainActivity
) : WebSocketListener() {

    lateinit var steveImageView: ImageView
//    lateinit var konfettiView: nl.dionsegijn.konfetti.compose
    lateinit var mediaPlayer: MediaPlayer
    lateinit var connectStatus: ImageView
    lateinit var client: OkHttpClient
    var isConnected = false


    override fun onOpen(webSocket: WebSocket, response: Response) {
        sendRegistrationMessage(webSocket)
        println("Websocket opening: $response")
        isConnected = true
        intent = Intent(OrderWebSocket.BROADCAST_ACTION)

        if((venueKey == "" || venueKey == null) && (credentials.venueKey != "")){
            venueKey = credentials.venueKey
        }

        if((deviceLicenseKey == "" || deviceLicenseKey == null) && (credentials.licenseKey != "")){
            deviceLicenseKey = credentials.licenseKey
        }

        if((printerId == "" || printerId == null) && (credentials.printerNum != "")){
            printerId = credentials.printerNum
        }

        val handler = Handler(Looper.getMainLooper())
        handler.post {
            connectStatus.setBackgroundResource(R.drawable.blue_check)
        }
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        isConnected = false

        val handler = Handler(Looper.getMainLooper())
        handler.post {
            connectStatus.setBackgroundResource(R.drawable.cross)
        }

        Logger.d("orderSocket","closed $reason")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {

        Logger.d("orderSocket","closing $reason")
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            connectStatus.setBackgroundResource(R.drawable.cross)
        }
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                try{
                    loggly!!.log(
                        LogglyBody(
                            "info",
                            "socketClosing",
                            null,
                            null,
                            reason
                        )
                    )
                }
                catch(e: Exception){}

            }
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        println("failed to connect to websocket: ${t.message}")
        isConnected = false

        val handler = Handler(Looper.getMainLooper())
        handler.post {
            connectStatus.setBackgroundResource(R.drawable.cross)
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        println("message received: ${bytes.hex()}")
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val printerMessage = Klaxon().parse<Map<String, String>>(text)
        println("Message received: ${printerMessage.toString()}")
        Logger.d("printorder-message","Message received ${printerMessage.toString()}")

        if(printerMessage != null &&
            printerMessage["message"] != null &&
            printerMessage["key"] != null){

            when (printerMessage["message"]){
                "printorder-pending" ->{
                    if(printerMessage["key"] != null){
                        fetchPrinterOrder(printerMessage.getValue("key"))
                    }
                }
                "printorder-bump" ->{
                    //Logger.d("printOrder-message","Remote bump request ${printerMessage["key"]}")
                    var sourceDeviceName = if(printerMessage["sourceDeviceName"] != null) printerMessage["sourceDeviceName"] else null
                    var checkId = if(printerMessage["checkId"] != null) printerMessage["checkId"].toString() else null
                    if(checkId != null && sourceDeviceName != null){
                        remoteBumpOrder(printerMessage["key"]!!, checkId, sourceDeviceName)
                    }
                }

                "printorder-complete" ->{
                    Logger.d("printorder-complete","Remote order complete request ${printerMessage["key"]}")
                    remoteCompleteOrder(printerMessage["key"]!!)
                }
                "printorder-priority" ->{
                    Logger.d("printorder-priority","Remote order priority request ${printerMessage["key"]}")
                    remotePriorityOrder(printerMessage["key"]!!)
                }
                "printorder-smschange" ->{
                    Logger.d("printorder-orderReadySms","Notifictaion of SMS change ${printerMessage["key"]}: ${printerMessage["meta"]}")
                    if(printerMessage["key"] != null && printerMessage["meta"] != null){
                        updateOrderReadySms(printerMessage["key"]!!, printerMessage["meta"]!!)
                    }

                }
                "printorder-confetti" ->{
                    Logger.d("printorder-confetti", "Celebrate good times!")
                    konfetti()
                }
            }
        }
    }

    private fun broadcastDatasetChange(){
        intent?.putExtra("message",OrderWebSocket.REFRESH_DATASET_MESSAGE)
        context.sendBroadcast(intent)
    }

    private fun fetchPrinterOrder(orderKey: String) {
        val url = "${credentials.baseApiUrl}stores/${credentials.venueKey}/printorders/$orderKey"
        val headerName = "Authorization"
        val headerBody = credentials.generateDeviceLicenseHeader()

        Networking.fetchJson(url = url, headerName = headerName, headerValue = headerBody, performOnCallback = ::handlePrintJob)
    }

    private fun handlePrintJob(call: Call, response: Response?, responseBody: String) {
        //PUT print job received
        println("print response: $responseBody")

        var printJob: Map<String, Any> = mapOf()
        var logLevel: String = "info";
        try{
            printJob = Klaxon().parse<Map<String, Any>>(responseBody)!!
            val key = printJob!!["key"] as String
            val payload = printJob["payload"] as String
            println("passed payload")
            val order = Klaxon().parse<Order>(payload)
            if(printJob["orderReadySms"] != null){
                order!!.orderReadySms = printJob["orderReadySms"].toString()
            }
            if(printJob["orderReadySmsCount"] != null){
                order!!.smsCount = printJob["orderReadySmsCount"] as Int?
            }
            if(printJob["orderHeaderColor"] != null){
                order!!.orderHeaderColor = printJob["orderHeaderColor"].toString()
            }
            println("passed order")

            if(credentials.printerNum != "" && (com.amorphik.focuskitchen.printerId == null || com.amorphik.focuskitchen.printerId == "")){
                com.amorphik.focuskitchen.printerId = credentials.printerNum
            }

            if(adapter.dataSet.size > 0 && adapter.dataSet.all { i -> i.orderKey == key }){
                return
            }

            val url = "${credentials.baseApiUrl}stores/${credentials.venueKey}/printorders"
            val headerName = "Authorization"
            val headerBody = credentials.generateDeviceLicenseHeader()
            var putPayload = """{
                                "key": "$key",
                                "status": "displayed"
                            }"""


            if (order != null) {
                // PUT Print job successful
                order.key = key
                addOrderToAdapter(order)

                val handler = Handler(Looper.getMainLooper())
                handler.post{
                    adapter.notifyDataSetChanged()
                }

                Networking.putData(url, headerName, headerBody, putPayload) { _,_,_ -> }

                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.Main) {
                        try{
                            loggly!!.log(
                                LogglyBody(
                                    logLevel,
                                    "printOrderReceived",
                                    Gson().toJson(order),
                                    order?.check
                                )
                            )
                        }
                        catch(e: Exception){}

                    }
                }
            } else {
                // PUT Print job error
                putPayload = """{
                                "key": "$key",
                                "status": "error",
                                "error": "parsing-error"
                            }"""

                logLevel = "error"
                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.Main) {
                        try{
                            loggly!!.log(
                                LogglyBody(
                                    "error",
                                    "printOrderReceived",
                                    credentials.venueKey,
                                    Gson().toJson(order),
                                    key
                                )
                            )
                        }
                        catch(e: Exception){}

                    }
                }
                Networking.putData(url, headerName, headerBody, putPayload) { _,_,_ -> }
            }
        }catch(error: java.lang.Exception){
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    try{
                        loggly!!.log(
                            LogglyBody(
                                "error",
                                "printOrderReceived",
                                responseBody
                            )
                        )
                    }
                    catch(e: Exception){}
                }
            }
            intent!!.putExtra("source",BroadcastIntentSource.ORDERERROR.value)
            intent!!.putExtra("printOrderKey","noOrderKey")
            context.sendBroadcast(intent)
            return
        }
    }

    private fun addOrderToAdapter(order: Order) {
        try{
            OrdersModel.addOrder(order)
            saveOrdersToDevice()
            playSound()

//            order.items.forEach { item ->
//                if(item.level == 0){
//                    Logger.d("allDay","before add item ${item.itemName}")
//                    val itemName = item.remoteName.subSequence(2, item.remoteName.length).toString()
//                    mainActivity.runOnUiThread(Runnable {
//                        mainActivity.allDayCountAddItem(itemName, item.quantity)
//                    })
//                }
//            }

        }catch(e: Exception){
            Logger.e("addPrintOrderError","${e.message}",false)
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    try{
                        loggly!!.log(
                            LogglyBody(
                                "error",
                                "addPrintOrderError",
                                Gson().toJson(order),
                                order.check,
                                Gson().toJson(e.message)
                            )
                        )
                    }
                    catch(e: Exception){}

                }
            }
        }

    }

    private fun remoteBumpOrder(printOrderKey: String, checkId: String, bumpFromStationName: String){
        if(adapter.dataSet.any{i -> i.orderKey == printOrderKey}){
            Thread.sleep(1500)
            intent!!.putExtra("source",BroadcastIntentSource.BUMPORDER.value)
            intent!!.putExtra("printOrderKey",printOrderKey)
            context.sendBroadcast(intent)
        } else{
            Logger.d("printOrder-message","No orders matching printOrderKey ${printOrderKey}")
        }
    }

    private fun remoteCompleteOrder(printOrderKey: String){
        if(adapter.dataSet.any{i -> i.orderKey == printOrderKey}){

            intent!!.putExtra("source",BroadcastIntentSource.COMPLETEORDER.value)
            intent!!.putExtra("printOrderKey",printOrderKey)
            context.sendBroadcast(intent)

        } else{
            Logger.d("printorder-complete","No orders matching printOrderKey ${printOrderKey}")
        }
    }

    private fun remotePriorityOrder(printOrderKey: String){
        if(adapter.dataSet.any{i->i.orderKey == printOrderKey}){
            intent!!.putExtra("source",BroadcastIntentSource.PRIORITYORDER.value)
            intent!!.putExtra("printOrderKey",printOrderKey)
            context.sendBroadcast(intent)
        } else{
            Logger.d("printorder-priority","No orders matching printOrderKey ${printOrderKey}")
        }
    }
    private fun saveOrdersToDevice() {

        Thread {
            val editor = credentials.sharedPreferences.edit()
            val ordersAsJson = Klaxon().toJsonString(OrdersModel.orders)

            editor.putString(credentials.PREFS_ORDERS_KEY, ordersAsJson)
            editor.apply()
        }.run()
    }

    private fun playSound() {
        mediaPlayer.start()
    }
    private fun sendRegistrationMessage(webSocket: WebSocket) {
        val signature = AuthGenerator.generateHash("${credentials.licenseKey}:${credentials.venueKey}:${credentials.macAddress}", credentials.licenseSecret)

        val message = """{
                            "action":"registerDevice",
                            "key":"${credentials.licenseKey}",
                            "signature":"$signature"
                        }"""


        webSocket.send(message)

    }

    private fun updateOrderReadySms(printOrderKey: String, orderReadySms: String){
        if(adapter.dataSet != null && adapter.dataSet.size > 0){
            val order = adapter.dataSet.find { i -> i.orderKey == printOrderKey }
            if(order != null && (order.orderReadySms == null || order.orderReadySms != orderReadySms)){
                order.orderReadySms = orderReadySms
                adapter.notifyDataSetChanged()
                Logger.d("printorder-orderReadySms","updated printOrder")
            }
        }

    }

    private fun konfetti(){
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            val mainActivity = MainActivity()
            mainActivity.konfettiView.start(session.party)
        }

    }

//    private fun steve() {
//        val handler = Handler(Looper.getMainLooper())
//        handler.post {
//            steveImageView.animate().alpha(1f).setDuration(1000L).start()
//            val konfettiView = KonfettiView(modifier = Modifier.)
//            konfettiView.build()
//                .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA, Color.BLUE)
//                .setDirection(0.0, 359.0)
//                .setSpeed(1f, 10f)
//                .setFadeOutEnabled(true)
//                .setTimeToLive(2000L)
//                .addShapes(nl.dionsegijn.konfetti.models.Shape.RECT, nl.dionsegijn.konfetti.models.Shape.CIRCLE)
//                .addSizes(Size(12))
//                .setPosition(1000f, 1000f, -50f, -50f)
//                .streamFor(100, 15000L)
//
//        }
//        handler.postDelayed({
//            steveImageView.animate().alpha(0f).setDuration(1000L).start()
//        }, 16000)
//    }

    companion object{
        val BROADCAST_ACTION = "com.amorphik.focuskitchen"
        val REFRESH_DATASET_MESSAGE = "refreshDataset"
        var intent: Intent? = null
    }

    enum class BroadcastIntentSource(val value: String){
        BUMPORDER("bumpOrder"),
        COMPLETEORDER("completeOrder"),
        PRIORITYORDER("priorityOrder"),
        SMSPRINTORDER("smsPrintOrder"),
        ORDERERROR("orderError"),
        LICENSEVERIFICATION("licenseVerification")
    }
}