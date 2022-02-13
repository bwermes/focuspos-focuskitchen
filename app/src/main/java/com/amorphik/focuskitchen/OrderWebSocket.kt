package com.amorphik.focuskitchen

import android.graphics.Color
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import com.beust.klaxon.Klaxon
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.dionsegijn.konfetti.KonfettiView
import nl.dionsegijn.konfetti.models.Size
import okhttp3.*
import okio.ByteString

class OrderWebSocket(private val credentials: DeviceCredentials, private val adapter: OrderAdapter) : WebSocketListener() {
    lateinit var steveImageView: ImageView
    lateinit var konfettiView: KonfettiView
    lateinit var mediaPlayer: MediaPlayer
    lateinit var connectStatus: ImageView
    lateinit var client: OkHttpClient
    var isConnected = false

    override fun onOpen(webSocket: WebSocket, response: Response) {
        sendRegistrationMessage(webSocket)
        println("Websocket opening: $response")
        isConnected = true

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

        println("closed: $reason")
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        println("closing: $reason")
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

        if (printerMessage != null &&
            printerMessage["message"] != null &&
            printerMessage["message"] == "printorder-pending" &&
            printerMessage["key"] != null) {
            fetchPrinterOrder(printerMessage.getValue("key"))
        }
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

        val printJob = Klaxon().parse<Map<String, Any>>(responseBody)
        val key = printJob!!["key"] as String
        val payload = printJob["payload"] as String
        val order = Klaxon().parse<Order>(payload)


        if(credentials.printerNum != "" && (com.amorphik.focuskitchen.printerId == null || com.amorphik.focuskitchen.printerId == "")){
            com.amorphik.focuskitchen.printerId = credentials.printerNum
        }

        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                try{
                    loggly!!.log(
                        LogglyBody(
                            "info",
                            "printOrderReceived",
                            Gson().toJson(order),
                            order?.check
                        )
                    )
                }
                catch(e: Exception){}

            }
        }

        val url = "${credentials.baseApiUrl}stores/${credentials.venueKey}/printorders"
        val headerName = "Authorization"
        val headerBody = credentials.generateDeviceLicenseHeader()
        var putPayload = """{
                                "key": "$key",
                                "status": "consumed"
                            }"""

        Networking.putData(url, headerName, headerBody, putPayload) { _,_,_ -> }

        if (order != null) {
            // PUT Print job successful
            order.key = key
            addOrderToAdapter(order)

            val handler = Handler(Looper.getMainLooper())
            handler.post{
                adapter.notifyDataSetChanged()
            }

            putPayload = """{
                                "key": "$key",
                                "status": "complete"
                            }"""
            Networking.putData(url, headerName, headerBody, putPayload) { _,_,_ -> }

        } else {
            // PUT Print job error
            putPayload = """{
                                "key": "$key",
                                "status": "error",
                                "error": "Order retrieved was unable to be parsed"
                            }"""

            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    try{
                        loggly!!.log(
                            LogglyBody(
                                "error",
                                "parseError",
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
    }

    private fun addOrderToAdapter(order: Order) {
        try{
            OrdersModel.addOrder(order)
            playSound()
            saveOrdersToDevice()
        }catch(e: Exception){
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
    private fun saveOrdersToDevice() {
        Thread {
            val editor = credentials.prefs.edit()
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
    private fun steve() {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            steveImageView.animate().alpha(1f).setDuration(1000L).start()
            konfettiView.build()
                .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA, Color.BLUE)
                .setDirection(0.0, 359.0)
                .setSpeed(1f, 10f)
                .setFadeOutEnabled(true)
                .setTimeToLive(2000L)
                .addShapes(nl.dionsegijn.konfetti.models.Shape.RECT, nl.dionsegijn.konfetti.models.Shape.CIRCLE)
                .addSizes(Size(12))
                .setPosition(1000f, 1000f, -50f, -50f)
                .streamFor(100, 15000L)

        }
        handler.postDelayed({
            steveImageView.animate().alpha(0f).setDuration(1000L).start()
        }, 16000)
    }
}