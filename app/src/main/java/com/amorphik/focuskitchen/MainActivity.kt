package com.amorphik.focuskitchen

import android.annotation.SuppressLint
import android.content.*
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.media.MediaPlayer
import android.os.*
import android.transition.TransitionManager
import androidx.appcompat.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amorphik.focuskitchen.allDayCount.AllDayCountAdapter
import com.amorphik.focuskitchen.itemControl.ActivityItemView
import com.beust.klaxon.Json
import com.beust.klaxon.Klaxon
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_all_day_count.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import nl.dionsegijn.konfetti.xml.KonfettiView


import okhttp3.*
import org.json.JSONArray
import java.io.Serializable
import java.lang.Error
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


var venueKey: String? = null
var deviceLicenseKey: String? = null
var printerId: String? = null

class MainActivity : AppCompatActivity() {
    lateinit var credentials : DeviceCredentials
    lateinit var orderAdapter: OrderAdapter
    lateinit var recallAdapter: RecallAdapter
    lateinit var allDayCountAdapter: AllDayCountAdapter
    lateinit var websocketClient: OkHttpClient
    lateinit var websocket: WebSocket
    lateinit var websocketListener: OrderWebSocket
    lateinit var managerModeButton: ImageView
    var connectionDrops: Int = 0
    var connectionDownMinutes: Int = 0
    var menuIsAnimating = false
    private lateinit var res: Resources
    lateinit var konfettiView: KonfettiView
    var displayAllDayCount: Boolean = false


    lateinit var mainHandler: Handler

    private val updateDeviceStatus = object: Runnable{
        override fun run(){
            Log.d("heartbeat","updateDeviceStatus")
            deviceHeartbeatAndConnectivityCheck()

            mainHandler.postDelayed(this, 120000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Init stuff
        loggly = LogglyService.create(applicationContext)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FocusKitchenApplication.appContext = applicationContext
        res = resources

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        // Build main layout and bumped orders
        orderRecyclerView_main.layoutManager = FlexboxLayoutManager(this, FlexDirection.COLUMN)
        recall_recycler_view.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        managerModeButton = this.findViewById<ImageView>(R.id.manager_mode_access_button)
        managerModeButton.visibility = View.GONE

        konfettiView = this.findViewById<KonfettiView>(R.id.viewKonfetti)

        // Init device info
        setDeviceDetails()
        gatherCredentials()
        // Init prefs
        prefs.authToken = "noToken"

        setupAdapters()
        //deviceNameAddsOrder()           Line can be uncommented to use JSON file to add orders (never deploy)
        deviceNameShowsDeviceDetails() // If above line is uncommented, comment out this line (always deploy)
        checkDeviceRegistration()
        runWebsocket()
        layoutMenu()
        mainHandler = Handler(Looper.getMainLooper())

        mainHandler.post(updateDeviceStatus)


        // Set text size
        previous_orders_textView.textSize = DeviceDetails.defaultTextSize
        prev_order_count_textView.textSize = DeviceDetails.defaultTextSize
        more_indicator.textSize = DeviceDetails.defaultTextSize

        // Bumped orders view gesture controls
        recall_recycler_view.setOnTouchListener(OnSwipeTouchListener(this) {
            gesture ->

            when (gesture) {
                1 -> recallAdapter.moveToPrevOrder()
                2 -> recallAdapter.moveToNextOrder()
            }
        })

        val allDayButton = this.findViewById<View>(R.id.main_activity_button_all_day)
        allDayButton.setOnClickListener { it ->
            buildMainUi(!displayAllDayCount)
        }




        // Will run after layout has happened
        // Need to do this for existing orders to load from saved and display properly
        // Couldnt get it to work without the counter. 3rd time through it will remove this listener
        var counter = 0
        view.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (counter == 2) {
                    // Used to purge specific key values a single time from memory if needed
                    if (!credentials.sharedPreferences.getBoolean("hasPurged1.0", false)) {
                        val editor = credentials.sharedPreferences.edit()
                        editor.putBoolean("hasPurged1.0", true)
                        editor.putString(credentials.PREFS_ORDERS_KEY, null)
                        editor.apply()
                    }

                    // Add the existing orders
                    try{
                        addExistingOrders()
                    }catch(e: Exception){
                        clearAllOrders()
                    }

                }
                counter++

                if (counter == 3){
                    view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            }
        })

        if(credentials.venueKey != "" && (venueKey == null || venueKey == "")){
            venueKey = credentials.venueKey
            deviceLicenseKey = credentials.licenseKey
        }
        if(credentials.printerNum != "" && (printerId == "" || printerId == null)){
            printerId = credentials.printerNum
        }

        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                try{
                    loggly!!.log(
                        LogglyBody(
                            "info",
                            "appLaunch",
                            "venueKey = " + credentials.venueKey + " licenseKey = " + credentials.licenseKey + " printerNum = " + credentials.printerNum,
                            null
                        )
                    )
                }
                catch(e: Exception){}

            }
        }
    }

    override fun onStart() {
        super.onStart()

        // Hide nav bar and status bar
        val viewFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        window.decorView.systemUiVisibility = viewFlags
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        // Hide nav bar and status bar
        val viewFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        window.decorView.systemUiVisibility = viewFlags
    }

    override fun onStop() {
        super.onStop()
        mainHandler.removeCallbacks(updateDeviceStatus)
        // Close websock if app moves into closed state
        closeWebsocket("On stop")
    }

    override fun onResume() {
        super.onResume()

        registerReceiver(
            broadcastReceiver, IntentFilter(
                OrderWebSocket.BROADCAST_ACTION
            )
        )
    }

    fun allDayCountAddItem(itemName: String, count: Float){
        Logger.d("allDay","addItemToAllDayCount")
        try{
            val existingItem = session.allDayCountRecords.find{ i-> i.menuItemName == itemName.trim()}
            if(existingItem != null){
                Logger.d("allDay","existing item found. changing count")
                existingItem.count += count
                existingItem.displayCount = existingItem.count.toString().replace(".0","")
            } else{
                Logger.d("allDay","add new item to session.allDayCountRecords")
                session.allDayCountRecords.add(AllDayCountRecord(itemName.trim(), count, count.toString().replace(".0","")))
            }

            session.allDayCountRecords.sortBy { it.menuItemName }
            allDayCountAdapter.notifyDataSetChanged()
        }catch(ex: java.lang.Exception){

        }


    }

    fun allDayCountRemoveItem(itemName: String, count: Float){
        Logger.d("allDay","Remove ${itemName}")
        val idx = session.allDayCountRecords.indexOfFirst{ i -> i.menuItemName == itemName.trim()}
        Logger.d("allDay","idx = $idx")
        if(idx != -1){
            if(session.allDayCountRecords[idx].count > count){
                Logger.d("allDay","Reducing count")
                session.allDayCountRecords[idx].count = session.allDayCountRecords[idx].count - count
                session.allDayCountRecords[idx].displayCount = session.allDayCountRecords[idx].count.toString().replace(".0","")
            } else{
                Logger.d("allDay","Removing item")
                session.allDayCountRecords.removeAt(idx)
            }
        }
        allDayCountAdapter.notifyDataSetChanged()
    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val message = intent.getStringExtra("message")
            Log.d("broadcast","on broadcast receive")
            Logger.d("smsOrder","in broadcast")
            val source = intent.getStringExtra("source");
            Logger.d("smsOrder","broadcast source ${source}")
            when(source){
                "bumpOrder" -> removeOrderFromAdapter(intent.getStringExtra("printOrderKey")!!)
                "completeOrder" -> completeOrderOnAdapter(intent.getStringExtra("printOrderKey")!!)
                "priorityOrder" -> priorityOrder(intent.getStringExtra("printOrderKey")!!)
                "smsPrintOrder" -> smsPrintOrderPrompt(intent.getStringExtra("printOrderKey")!!, intent.getStringExtra("orderReadySms")!!)
            }

        }
    }

    private fun buildMainUi(showAllDayCount: Boolean){
        val mainActivityView = this.findViewById<ConstraintLayout>(R.id.main_view_body)
        val allDayCountView = this.findViewById<ConstraintLayout>(R.id.main_all_day_count_view)
        var constraintSet = ConstraintSet();

        constraintSet.clone(mainActivityView)
        allDayCountView.visibility = View.VISIBLE


        if(showAllDayCount){
            Logger.d("allDay","showing all day count")

            constraintSet.connect(R.id.main_order_recycler_constraint_holder, ConstraintSet.END, R.id.main_guide_75, ConstraintSet.END)
        }else{
            Logger.d("allDay","hiding all day count")

            constraintSet.connect(R.id.main_order_recycler_constraint_holder, ConstraintSet.END, R.id.main_guide_100, ConstraintSet.END)
        }
        TransitionManager.beginDelayedTransition(mainActivityView)
        constraintSet.applyTo(mainActivityView)
        Logger.d("allDay","after apply")

        displayAllDayCount = !displayAllDayCount
    }

    private fun completeOrderOnAdapter(printOrderKey: String){
        val orderComplete: Boolean = OrdersModel.markOrderComplete(printOrderKey)
        if(orderComplete){
            Log.d("printorder-complete","order found. marked completed order")
            orderAdapter.notifyDataSetChanged()
        } else{
            Log.d("printorder-complete","order not found")
        }

    }

    private fun priorityOrder(printOrderKey: String){
        val orderPriority: Boolean = OrdersModel.markOrderPriority(printOrderKey)
        if(orderPriority){
            Log.d("printorder-priority","order found. marked priority order")
            orderAdapter.notifyDataSetChanged()
        } else{
            Log.d("printorder-priority","order not found")
        }
    }

    private fun removeOrderFromAdapter(printOrderKey: String){
        val order = OrdersModel.findOrdersToBump(printOrderKey)
        if(order != null){
            Log.d("printOrder-main","bumping order ${order.orderKey} position ${order.lastPosition}")
            orderAdapter.bumpOrder(order.lastPosition)
        }
    }

    // Turns the device name label into a button to add orders
    private fun deviceNameAddsOrder() {
        val orders = mutableListOf<Order>()
        for (i in 0..4) {
            // Pull order from JSON file
            val order = Klaxon().parse<Order>(applicationContext.assets.open("order$i.JSON"))
            if (order != null) orders.add(order)
        }
        venue_info_textView.setOnClickListener {
            // Choose a random order and add it
            val random = Random().nextInt(4)
            OrdersModel.addOrder(orders[random])
            saveOrdersToDevice()
            orderAdapter.notifyDataSetChanged()
        }
    }

    // 5 taps on device name will show details
    private fun deviceNameShowsDeviceDetails() {
        var tapCount = 0
        var startTime: Long = 0
        venue_info_textView.setOnClickListener {
            val time = System.currentTimeMillis()
            if (startTime == 0L || (time - startTime > 3000)) {
                startTime = time
                tapCount = 1
            } else {
                tapCount++
            }

            if (tapCount == 5) {
                showDeviceDetails()
            }
        }
    }

    private fun deviceHeartbeatAndConnectivityCheck(){

        Log.d("heartbeat","deviceHeartbeatAndConnectivityCheck")
        try{
            val statusConnected = Utility.appConnectivityCheck(this@MainActivity, credentials, connectionDownMinutes, connectionDrops)
            val main_connectionDownDuration = this.findViewById<TextView>(R.id.main_connectionDownDuration)
            if(!statusConnected){
                val viewHeaderResource = this.findViewById<View>(R.id.view)
                val res = resources
                viewHeaderResource.setBackgroundColor(getResources().getColor(R.color.yellow_danger))
                Log.d("heartbeat","status post failed")
                connectionDownMinutes += 1
                connectionDrops += 1
                if(connectionDownMinutes > 1){
                    main_connectionDownDuration.visibility = View.VISIBLE
                    main_connectionDownDuration.text = String.format(res.getString(R.string.main_connection_status),"$connectionDownMinutes minutes")
                }


            } else{
                main_connectionDownDuration.visibility = View.INVISIBLE
                val viewHeaderResource = this.findViewById<View>(R.id.view)
                val res = resources
                viewHeaderResource.setBackgroundColor(getResources().getColor(R.color.colorFocusBlue))
                Log.d("heartbeat","status post successful")
                connectionDownMinutes = 0

            }

        }catch(e: java.lang.Exception){
            Log.d("heartbeat","status post errored")
            Log.e("heartbeat", "error: ${e.message}")
        }
    }

    private fun checkDeviceRegistration() {
        val deviceWasRegistered = verifyCredentials(credentials)
        if (!deviceWasRegistered) {

            // Let user know we are looking for a license.
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Checking License Status")
            builder.setMessage("Please wait...")
            builder.setCancelable(false)

            val dialog = builder.create()
            dialog.show()
            dialog.window?.setLayout(700, dialog.window!!.attributes!!.height)

            // No device details are saved
            // Check to see if device had been registered before
            // If it has, use that license. Otherwise, prompt registration
            val url = "https://focuslink.focuspos.com/v2/licenses/claimed?deviceId=${credentials.macAddress}" // URL to find match device
            val header = "Authorization"
            val headerBody = credentials.generateIntegratorHeader() // Integrator Header Value
            Networking.fetchJson(url, header, headerBody) {
                    call, response, body ->

                if(response != null) {
                    // Handle registration has already occurred
                    val jsonResponse = Klaxon().parse<Map<String, Any>>(body)
                    if (jsonResponse != null) {
                        credentials.licenseKey = jsonResponse["key"] as String
                        credentials.licenseSecret = jsonResponse["secret"] as String
                        credentials.deviceName = jsonResponse["name"] as String
                        credentials.venueKey = (jsonResponse["venueKey"] as Int).toString()

                        deviceLicenseKey = jsonResponse["key"] as String
                        venueKey = (jsonResponse["venueKey"] as Int).toString()
                        printerId = (jsonResponse["printerId"] as Int).toString()

                        CoroutineScope(Dispatchers.IO).launch {
                            withContext(Dispatchers.Main) {
                                try{
                                    loggly!!.log(
                                        LogglyBody(
                                            "info",
                                            "checkDeviceRegistration",
                                            "venueKey = " + (jsonResponse["venueKey"] as Int).toString() + " licenseKey = " + jsonResponse["key"] as String,
                                            null,
                                            null
                                        )
                                    )
                                }
                                catch(e: Exception){}

                            }
                        }

                        if (venue_info_textView.text == "") {
                            runOnUiThread {
                                venue_info_textView.text = credentials.deviceName
                            }
                        }

                        // Save necessary info and proceed as if device had this info already saved
                        val editor = credentials.sharedPreferences.edit()
                        editor.putString(credentials.PREFS_LICENSE_KEY, credentials.licenseKey)
                        editor.putString(credentials.PREFS_VENUE_KEY, credentials.venueKey)
                        editor.putString(credentials.PREFS_MAC_ADDR, credentials.macAddress)
                        editor.putString(credentials.PREFS_IP_ADDR, credentials.ipAddress)
                        editor.putString(credentials.PREFS_DEVICE_NAME, credentials.deviceName)
                        editor.putString(credentials.PREFS_LICENSE_SECRET, credentials.licenseSecret)
                        editor.apply()

                        verifyCredentials(credentials)
                    }

                } else {
                    // Error occurs or we find no license
                    openDialog(credentials, "UNREGISTERED")
                }

                dialog.dismiss()
            }
        }
    }

    // Begin websocket
    private fun runWebsocket() {
        websocket_status_imageView.setOnClickListener {
            closeWebsocket("Socket status tapped")
            startWebSocket(credentials, websocketListener)
        }

        websocketListener = OrderWebSocket(credentials, orderAdapter, this, this)
        val websocketReconnection = object: Runnable {
            override fun run() {
                Handler().postDelayed(this, 5000)
                if (!websocketListener.isConnected) {
                    startWebSocket(credentials, websocketListener)
                }
            }
        }
        websocketReconnection.run()
    }

    // Timer ticks to update the time on the order headers as well as pulling held orders
    private fun runUpdateTimer() {
        val REPEAT_TIME = 60000L // Interval in ms for timer to tick (Long)

        val updateTime = object: Runnable {
            override fun run() {
                val dataSet = orderAdapter.dataSet
                val heldOrders = OrdersModel.heldOrders

                // Format of time received from api
                val currentTime = Date()

                for (item in 0.until(dataSet.size)) {
                    if (dataSet[item].isHeader) {
                        // Determine the time the order came in
                        val timeOfOrder = dataSet[item].time
                        val delayTime = dataSet[item].delayTime

                        // Time difference
                        val diff = currentTime.time - timeOfOrder - (delayTime * 60000)
                        val seconds = diff / 1000
                        val mins = seconds / 60
                        val hours = mins / 60

                        // Format time displayed on header
                        // Should show as m:ss or mm:ss when hours are 0
                        // Should show as h:mm:ss when hours > 0
                        // Use string format to achieve wanted display format

                        var format = "%2dm"
                        var time = String.format(format, mins)
                        if (mins < 1) {
                            time = "<1m"
                        }
                        if (hours > 0) {
                            format = "%2dh%02dm"
                            if ((mins - (hours * 60)).toInt() == 0) {
                                format = "%2dh"

                            }
                            time = String.format(
                                format,
                                hours,
                                mins - (hours * 60)
                            )
                        }

                        dataSet[item].minutesInSystem = mins
                        dataSet[item].timeInSystem = time

                        // Only notify items that aren't in the process of animating that their time changed
                        if (!dataSet[item].isAnimating) {
                            runOnUiThread {
                                orderAdapter.notifyItemChanged(item)
                            }
                        }
                    }
                }

                // Wont do anything if there are no orders held
                // No impact w/ no held orders
                var toRemove = mutableListOf<MutableList<OrderAdapterDataItem>>()
                for (order in heldOrders) {
                    val orderHeader = order[0]

                    // Determine the time the order came in
                    val timeOfOrder = orderHeader.time

                    // Time difference
                    val diff = currentTime.time - timeOfOrder
                    val seconds = diff / 1000
                    val mins = seconds / 60

                    if (mins >= orderHeader.delayTime) {
                        println("Moving delayed order ${order[0].checkNum} to current orders.")
                        for (item in order) {
                            dataSet.add(item)
                        }
                        orderAdapter.notifyDataSetChanged()
                        Thread {
                            val editor = credentials.sharedPreferences.edit()
                            val ordersAsJson = Klaxon().toJsonString(OrdersModel.orders)
                            editor.putString(credentials.PREFS_ORDERS_KEY, ordersAsJson)
                            editor.apply()
                        }.run()
                        toRemove.add(order)
                    }
                }

                // Dump and held orders that were moved over
                if (toRemove.size > 0) {
                    heldOrders.removeAll(toRemove)
                }

                // Spin up new thread to save the delayed orders
                thread {
                    val editor = credentials.sharedPreferences.edit()
                    val delayedOrdersJSON = Klaxon().toJsonString(OrdersModel.heldOrders)
                    editor.putString(credentials.PREFS_DELAYED_KEY, delayedOrdersJSON)
                    editor.apply()
                }

                // Used to set device name after its retrieved from the back end
                runOnUiThread {
                    venue_info_textView.text = credentials.deviceName
                }

                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed(this, REPEAT_TIME)
            }
        }

        val handler = Handler(Looper.getMainLooper())

        handler.post {
            updateTime.run()
        }
    }

    // Ticks every 30 mins to get printer id and name periodically
    private fun runThirtyMinTimer() {
        val updateTime = object: Runnable {
            override fun run() {
                if (credentials.licenseKey != "") {
                    val signature = AuthGenerator.generateHash("${credentials.licenseKey}:${credentials.venueKey}:${credentials.macAddress}", credentials.licenseSecret)

                    val payload = """{
                                "key": "${credentials.licenseKey}",
                                "signature": "$signature"
                             }"""

                    Networking.postData(url = "${credentials.baseApiUrl}licenses/verify",
                        headerName = "Authorization",
                        headerValue = credentials.generateDeviceLicenseHeader(),
                        payload = payload) {
                            call, response, responseBody ->

                        if (response != null && response.code == 200) {
                            val jsonResponse = Klaxon().parse<Map<String, Any>>(responseBody)
                            println("verification response: $jsonResponse")
                            credentials.printerNum = jsonResponse!!["printerId"].toString()
                            credentials.deviceName = jsonResponse["name"].toString()
                            fetchDevicePrefs(credentials)
                        }
                    }
                }

                println("THIRTY MIN TICK")
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed(this, 1800000)
            }
        }

        val handler = Handler(Looper.getMainLooper())

        handler.post {
            updateTime.run()
        }
    }

    // Determine layout for sliding bumped orders menu
    private fun layoutMenu () {
        menu_layout.layoutParams.width = (DeviceDetails.pixelWidth - DeviceDetails.DECORATION_SUM / DeviceDetails.COLUMN_COUNT) / DeviceDetails.COLUMN_COUNT
        menu_layout.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                menu_layout.viewTreeObserver.removeOnGlobalLayoutListener(this)

                setupMenu()
            }
        })
    }

    // Setup display for the bumped orders menu
    private fun setupMenu() {
        var moveAmount: Float = menu_layout.width.toFloat()
        var menuVisible = false
        menu_layout.layoutParams.width = moveAmount.toInt()
        menu_layout.x += moveAmount
        menu_layout.isClickable = true
        menu_button.setOnClickListener {
            if (!menuIsAnimating) {
                menuVisible = !menuVisible
                if (menuVisible) {
                    menu_button.setBackgroundResource(R.drawable.close)
                } else {
                    menu_button.setBackgroundResource(R.drawable.recall)
                }
                moveAmount = -moveAmount
                recallAdapter.position = 0
                recallAdapter.dataSet = OrdersModel.bumpedOrders[0]
                recallAdapter.notifyDataSetChanged()

                menuIsAnimating = true
                menu_layout.animate().translationXBy(moveAmount).setInterpolator(AccelerateInterpolator()).setDuration(250).withEndAction {
                    menuIsAnimating = false
                }.start()
            }
        }

        managerModeButton.setOnClickListener{
            Log.d("itemControl","launch intent")
            val intent = Intent(this@MainActivity, ActivityItemView::class.java)
            startActivity(intent)
        }
    }

    // Adapters hold the displayed orders
    private fun setupAdapters() {
        orderAdapter = createOrderAdapter()
        recallAdapter = createRecallAdapter()
        allDayCountAdapter = createAllDayAdapter()
        orderAdapter.recallAdapter = recallAdapter
        recallAdapter.orderAdapter = orderAdapter
    }

    private fun createOrderAdapter() : OrderAdapter {
        val adapter = OrderAdapter()
        adapter.setHasStableIds(true)
        adapter.mainActivity = this
        adapter.context = this
        orderRecyclerView_main.adapter = adapter
        orderRecyclerView_main.setHasFixedSize(true)
        orderRecyclerView_main.addItemDecoration(OrderDecoration())
        orderRecyclerView_main.setItemViewCacheSize(10000)
        orderRecyclerView_main.itemAnimator = null
        adapter.credentials = credentials

        // Wrap space threshold determines height available before wrapping to next column
        val dpOfHeader = 85
        val dpOfOrderItem = 40
        adapter.WRAP_SPACE_THRESHOLD = (((dpOfHeader) + (3 * dpOfOrderItem)) * Resources.getSystem().displayMetrics.density).toInt()

        return adapter
    }

    private fun createRecallAdapter() : RecallAdapter {
        val adapter = RecallAdapter()
        adapter.setHasStableIds(true)
        recall_recycler_view.adapter = adapter
        recall_recycler_view.setHasFixedSize(true)
        recall_recycler_view.addItemDecoration(RecallDecoration())
        recall_recycler_view.setItemViewCacheSize(10000)
        recall_recycler_view.itemAnimator = null
        adapter.credentials = credentials

        // Allows scrolling of order if its contents are longer than the screen
        recall_recycler_view.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                // Indicator to let user know there is more content
                if ((recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition() == (recyclerView.layoutManager as LinearLayoutManager).itemCount - 1) {
                    more_indicator.alpha = 0f
                } else {
                    more_indicator.alpha = 1f
                }
                super.onScrolled(recyclerView, dx, dy)
            }
        })
        adapter.positionTextView = prev_order_count_textView

        return adapter
    }

    private fun createAllDayAdapter(): AllDayCountAdapter{
        val adapter = AllDayCountAdapter(session.allDayCountRecords)
        all_day_recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        all_day_recycler.adapter = adapter
        return adapter
    }



    // Import existing orders saved in the system. Will happen at app relaunch
    private fun addExistingOrders() {
        if (credentials.sharedPreferences.getString(credentials.PREFS_ORDERS_KEY, null) != null) {
            var savedOrders: List<OrderAdapterDataItem> = emptyList()
            try{
                savedOrders = Klaxon().parseArray<OrderAdapterDataItem>(credentials.sharedPreferences.getString(credentials.PREFS_ORDERS_KEY, "[]")!!)!!
            }catch(ex: java.lang.Exception){
                Logger.d("orderSave","EX: ${ex.message}")
            }


            for (order in savedOrders) {
                OrdersModel.orders.add(order)
                orderAdapter.notifyDataSetChanged()

                if(!order.isHeader && order.itemLevel == 0){
                    Logger.d("allDay","existing item to add ${order.orderKey}")
                    order.itemName?.let { allDayCountAddItem(it, order.quantity.toFloat()) }
                }
            }
        }
        if (credentials.sharedPreferences.getString(credentials.PREFS_DELAYED_KEY, null) != null) {
            val jsonString = credentials.sharedPreferences.getString(credentials.PREFS_DELAYED_KEY, "[]")!!
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until(jsonArray.length())) {
                val savedOrder = Klaxon().parseArray<OrderAdapterDataItem>(jsonArray[i].toString())!!
                OrdersModel.heldOrders.add(savedOrder.toMutableList())
                println("Adding order ${savedOrder[0].checkNum} to held orders. Delay time is ${savedOrder[0].delayTime}.")
            }
        }

        runUpdateTimer()
        runThirtyMinTimer()
    }

    // Used to test manually adding orders
//    private fun addOrderToAdapter(order: Order) {
//        val orderType = order.orderType
//        val server = order.server
//        val table = order.table
//        val time = Date().time
//        val checkNum =  order.check
//
//        val headerItem = OrderAdapterDataItem(isModifier = false, isHeader = true, isLastItemInOrder = false, orderType = orderType,
//            server = server, time = time, table = table, itemName = "", itemLevel = 0, checkNum = checkNum, voided = false, quantity = 0)
//
//        OrdersModel.orders.add(headerItem)
//        for (i in 0.until(order.items.size)) {
//            val item = order.items[i]
//            val itemIsModifier = item.level > 0
//            var remoteName = item.remoteName
//            val lastItem = i == order.items.size - 1
//            val quantity = item.quantity.toInt()
//            val voided = item.voided
//            for (i in 1..item.level) {
//                remoteName = "   $remoteName"
//            }
//            if (item.level == 0) {
//                remoteName = remoteName.subSequence(2, remoteName.length).toString()
//            }
//            val orderItem = OrderAdapterDataItem(isModifier = itemIsModifier, isHeader = false, isLastItemInOrder = lastItem,orderType = orderType,
//                server = server, time = time, table = table, itemName = remoteName, itemLevel = item.level, checkNum = checkNum, voided = voided, quantity = quantity)
//
//            OrdersModel.orders.add(orderItem)
//        }
//    }
//    private fun addSwipeGesture(adapter: RecallAdapter, direction: Int) {
//        val callback = object: ItemTouchHelper.SimpleCallback(0, direction) {
//            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
//                when (direction) {
//                    ItemTouchHelper.LEFT -> adapter.moveToNextOrder()
//                    ItemTouchHelper.RIGHT -> adapter.moveToPrevOrder()
//                }
//            }
//
//            override fun onMove(
//                recyclerView: RecyclerView,
//                viewHolder: RecyclerView.ViewHolder,
//                target: RecyclerView.ViewHolder
//            ): Boolean {
//                return false
//            }
//
//            override fun onChildDraw(
//                c: Canvas,
//                recyclerView: RecyclerView,
//                viewHolder: RecyclerView.ViewHolder,
//                dX: Float,
//                dY: Float,
//                actionState: Int,
//                isCurrentlyActive: Boolean
//            ) {
//
//                if (actionState == ItemTouchHelper.UP) {
//                    viewHolder.itemView.translationY = 0f
//                    viewHolder.itemView.translationX = 0f
//                } else {
//                    super.onChildDraw(
//                        c,
//                        recyclerView,
//                        viewHolder,
//                        dX,
//                        dY,
//                        actionState,
//                        isCurrentlyActive
//                    )
//                }
//            }
//
//            override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
//                // Default value is 1200
//                // Default was too low to consistently react to swipe
//                // Set to 2000 for more consistency
//                return super.getSwipeVelocityThreshold(7000f)
//            }
//        }
//
//        val myHelper = ItemTouchHelper(callback)
//        myHelper.attachToRecyclerView(recall_recycler_view)
//    }
//    private fun formatReceivedTime(time: String) : String{
//        val timeFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss z y", Locale.US)
//        val date = timeFormat.parse(time)
//        val formatDate = SimpleDateFormat("h:mm a", Locale.US)
//        return if (date != null ) formatDate.format(date) else ""
//    }
//    private fun formatOrderTime(time: String) : String{
//        // Takes the time received from the JSON payload and formats by swapping from 24hr to 12hr
//        val timeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
//        val date = timeFormat.parse(time)
//        val formatDate = SimpleDateFormat("h:mm a", Locale.US)
//        return if (date != null ) formatDate.format(date) else ""
//    }

    private fun saveOrdersToDevice() {
        val editor = credentials.sharedPreferences.edit()
        val ordersAsJson = Klaxon().toJsonString(OrdersModel.orders)

        editor.putString(credentials.PREFS_ORDERS_KEY, ordersAsJson)
        editor.apply()

    }

    // Registration dialog functions
    private fun openDialog(status: String, errorCode: Int) {
        val registrationDialog = VenueDialog()
        registrationDialog.status = status
        registrationDialog.errorCode = errorCode
        registrationDialog.isCancelable = false
        registrationDialog.show(supportFragmentManager, "venueDialog")
    }

    private fun openDialog(credentials: DeviceCredentials, status: String) {
        val registrationDialog = VenueDialog()
        registrationDialog.status = status
        registrationDialog.credentials = credentials
        registrationDialog.isCancelable = false
        registrationDialog.show(supportFragmentManager, "venueDialog")
    }

    // Display device details
    private fun showDeviceDetails() {
        val deviceDetailsDialog = DeviceDialog()
        deviceDetailsDialog.credentials = credentials
        deviceDetailsDialog.isCancelable = false
        deviceDetailsDialog.supportFragmentManager = supportFragmentManager
        deviceDetailsDialog.show(supportFragmentManager, "deviceDialog")
        deviceDetailsDialog.orderAdapter = orderAdapter
        deviceDetailsDialog.websocket = websocket
    }

    private fun verifyCredentials(credentials: DeviceCredentials) : Boolean {
        //Returns false if no credentials were found
        //Returns true if credentials were found

        if(credentials.licenseSecret == "") {
            println("license secret null, credentials unverified")
            return false
        } else {
            val signature = AuthGenerator.generateHash("${credentials.licenseKey}:${credentials.venueKey}:${credentials.macAddress}", credentials.licenseSecret)

            prefs.license = License(
                key = credentials.licenseKey,
                secret = credentials.licenseSecret,
                venueKey = credentials.venueKey.toInt()
            )
            prefs.venueKey = credentials.venueKey.toInt()
            prefs.licenseVerified = true
            prefs.authToken = signature


            Log.d("license","authToken = ${prefs.authToken}")

            val payload = """{
                                "key": "${credentials.licenseKey}",
                                "signature": "$signature",
                                "softwareVersion": "${BuildConfig.VERSION_NAME}",
                                "softwareVersionCode": "${BuildConfig.VERSION_CODE}"
                             }"""

            Networking.postData(url = "${credentials.baseApiUrl}licenses/verify",
                headerName = "Authorization",
                headerValue = credentials.generateDeviceLicenseHeader(),
                payload = payload,
                performOnCallback = ::handleVerification)
            return true
        }
    }

    // Verify license is valid
    private fun handleVerification(call: Call, response: Response?, responseBody: String) {
        Logger.d("licenseCheck","${responseBody.toString()}")
        if (response == null || response != null && response.code >= 400) {
            if (response != null) {
                Utility.clearLicenseInfo(credentials)
                openDialog(credentials, "UNREGISTERED")
            }
            return
        }

        val license = Klaxon().parse<License>(responseBody)
        println("verification response: $license")
        if (!(license!!.active as Boolean)) {
            openDialog("DISABLED", 0)
        } else {
            credentials.printerNum = license.printerId.toString()
            credentials.deviceName = license.name.toString()
            if(license.features?.manager == true){
                runOnUiThread { managerModeButton.visibility = View.VISIBLE }
            }


            if(license.features == null){
                license.features = LicenseFeatures()
                Toast.makeText(applicationContext,"License features are unavailable, assigning defaults." , Toast.LENGTH_LONG).show()
                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.Main) {
                        loggly!!.log(
                            LogglyBody(
                                "error",
                                "license-features",
                                "licenseKey = ${license.key}, licenseFeatures is null assigned defaults",
                                null,
                                null
                            )
                        )
                    }
                }
            }
            license.features?.let { assignFeatures(it) }

            fetchDeviceMode(credentials)
        }
    }

    // Check if device should be running prod or dev
    private fun fetchDeviceMode(credentials: DeviceCredentials) {
        val url = "${credentials.baseApiUrl}stores/${credentials.venueKey}"
        val header = "Authorization"
        val headerBody = credentials.generateDeviceLicenseHeader()
        Networking.fetchJson(url, header, headerBody) { _, _, venueDetailsResponse ->
            val modeResponse = Klaxon().parse<Map<String, Any>>(venueDetailsResponse)

            println("mode response: $modeResponse")
            if (modeResponse != null && modeResponse["mode"] != null) {
                credentials.mode = modeResponse["mode"] as String

                val editor = credentials.sharedPreferences.edit()

                if (credentials.mode == "prod") {
                    credentials.baseApiUrl = "https://focuslink.focuspos.com/v2/"
                    credentials.baseWsUrl = "https://ws.focuslink.focuspos.com/"
                    editor.putString(credentials.PREFS_MODE_KEY, "prod")
                    prefs.mode = "prod"
                } else {
                    credentials.baseApiUrl = "https://dev.focuslink.focuspos.com/v2/"
                    credentials.baseWsUrl = "https://dev.ws.focuslink.focuspos.com/"
                    editor.putString(credentials.PREFS_MODE_KEY, "dev")
                    prefs.mode = "dev"
                }

                editor.apply()
            }

            fetchDevicePrefs(credentials)
        }
    }

    // Check back end for any device preferences
    private fun fetchDevicePrefs(credentials: DeviceCredentials) {
        val url = "${credentials.baseApiUrl}stores/${credentials.venueKey}/kitchendevicepreferences"
        val header = "Authorization"
        val headerBody = credentials.generateDeviceLicenseHeader()

        Networking.fetchJson(url, header, headerBody) {
                _, _, devicePrefsResponse ->

            val prefsResponse = Klaxon().parse<Map<String, Any>>(devicePrefsResponse)
            prefs.venuePreferences
            println("device prefs response: $prefsResponse")
            if (prefsResponse != null && prefsResponse["urgentTime"] != null) {
                credentials.urgentTime = prefsResponse["urgentTime"] as Int
            }

            compareIp(credentials)
        }
    }

    // Assign device color pallet based on license features
    private fun assignFeatures(features: LicenseFeatures){
        prefs.licenseFeatures = features;
        if(features.bumpToPrinterIdList != null && features.bumpToPrinterIdList!!.size > 0){
            credentials.bumpToPrinterEnabled = true
        }

        if(features.sms != null){
            Log.d("smsOrder","sms feature enabled = ${features.sms!!}")
            credentials.sms = features.sms!!
        }

        if(features.smsOnBump != null){
            credentials.smsOnBump = features.smsOnBump!!
        }

        if(features.smsOnBumpPrompt != null){
            credentials.smsOnBumpPrompt = features.smsOnBumpPrompt!!
        }

        runOnUiThread {
            var mainRecyclerViewBody = this@MainActivity.findViewById<RecyclerView>(R.id.orderRecyclerView_main)
            mainRecyclerViewBody.setBackgroundColor(Color.parseColor(prefs.licenseFeatures!!.kitchenViewBackgroundColor))
            var allDayCountBody = this@MainActivity.findViewById<RecyclerView>(R.id.all_day_recycler)
            allDayCountBody.setBackgroundColor(Color.parseColor((prefs.licenseFeatures!!.kitchenViewAllDayBackgroundColor)))
        }
    }

    // Check device ip vs backend and update if necessary
    private fun compareIp(credentials: DeviceCredentials) {
        val currentIp = credentials.ipAddress
        val savedIp = credentials.sharedPreferences.getString(credentials.PREFS_IP_ADDR, "")
        if (currentIp != savedIp && savedIp != "") {
            val editor = credentials.sharedPreferences.edit()
            editor.putString(credentials.PREFS_IP_ADDR, currentIp)
            editor.apply()
            updateIpAddress(credentials, currentIp)
        }
    }

    private fun updateIpAddress(credentials: DeviceCredentials, currentIp: String) {
        // Might not be implemented on the back end yet
        // Check w/ Brandon and Justin to see if this is
        val url = "${credentials.baseApiUrl}licenses/ip"
        val header = "Authorization"
        val headerBody = credentials.generateDeviceLicenseHeader()
        val payload = """{"key": "${credentials.licenseKey}", "ip": "$currentIp", "softwareVersion": "${BuildConfig.VERSION_NAME}"}"""
        Networking.putData(url,header,headerBody,payload) {
            call, response, responseBody ->
            println("update IP Addr response: $responseBody")
        }
    }

    fun smsPrintOrderPrompt(printOrderKey: String, orderReadySms: String?){
        Logger.d("smsOrder","smsPrintOrderPrompt")

        val smsHeaderText = "(XXX) XXX-${orderReadySms!!.takeLast(4)}"
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater

        builder.setTitle("Send Order Ready SMS?")

        val dialogLayout = inflater.inflate(R.layout.order_sms_dialog,null)
        val headerText = dialogLayout.findViewById<TextView>(R.id.order_sms_dialog_header)

        headerText.text = String.format(res.getString(R.string.order_sms_dialog_header),smsHeaderText)

        val yesButton = dialogLayout.findViewById<Button>(R.id.order_sms_button_yes)
        val noButton = dialogLayout.findViewById<Button>(R.id.order_sms_button_no)

        Logger.d("smsOrder","pass dialog build")
        builder.setView(dialogLayout)

        val dialog = builder.create()
        val params = dialog.window!!.attributes
        val displayMetrics = DisplayMetrics()
        params.y = 50;

        params.width = ((displayMetrics.widthPixels * 0.5f).toInt())
        params.height = ((displayMetrics.heightPixels * 0.5f).toInt())
        dialog.window!!.attributes = params
        dialog.window!!.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        )
        dialog.window!!.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        )
        dialog.window!!.decorView.systemUiVisibility = uiFlags
        dialog.show()
        dialog.window!!.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        Logger.d("smsOrder","pass dialog show")
        yesButton.setOnClickListener {
            Logger.d("smsOrder","yes pressed")
            smsPrintOrderSend(printOrderKey)
            dialog.dismiss()
        }

        noButton.setOnClickListener {
            Logger.d("smsOrder","no pressed")
            dialog.dismiss()
        }
    }

    fun smsPrintOrderSend(printOrderKey: String){
        Logger.d("smsOrder","in send function")

        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                try{
                    Logger.d("smsOrder","before send")
                    val response = api!!.smsPrintOrder(credentials.venueKey.toInt(), printOrderKey)
                    Logger.d("smsOrder","after send")
                    if(response != null && response.smsCount != null){
                        Logger.d("smsOrder","pass good response")
                        val printOrder = orderAdapter.dataSet.find { i -> i.orderKey == printOrderKey }
                        if(printOrder != null){
                            printOrder.smsCount = response?.smsCount!!
                            orderAdapter.notifyDataSetChanged()
                            orderAdapter.updateSavedOrders()
                            Utility.notification("Order Ready! sent for ${printOrder.checkNum}")
                        }
                    }
                }
                catch(e: Exception){
                    Logger.e("smsOrder","${e.message}",false)
                }

            }
        }

    }




    // Open WS for receiving orders
    private fun startWebSocket(credentials: DeviceCredentials, listener: OrderWebSocket) {
        val request = Request.Builder().url(credentials.baseWsUrl).build()
        listener.steveImageView = steve_imageView

        // media player was crashing app previously, plays beep when order comes in
        listener.mediaPlayer = MediaPlayer.create(this, R.raw.beep)
        listener.connectStatus = websocket_status_imageView
        websocketClient = OkHttpClient().newBuilder().retryOnConnectionFailure(true).pingInterval(1, TimeUnit.MINUTES).build()
        listener.client = websocketClient
        websocket = websocketClient.newWebSocket(request, listener)

        websocketClient.dispatcher.executorService.shutdown()
    }

    private fun closeWebsocket(reason: String) {
        websocket_status_imageView.setBackgroundResource(R.drawable.cross)
        websocket.close(1001, reason)
    }

    // Read credentials from disk
    private fun gatherCredentials() {
        val sharedPrefs = this.getPreferences(Context.MODE_PRIVATE)
        credentials = DeviceCredentials(sharedPrefs, this)

        venueKey = credentials.venueKey
        deviceLicenseKey = credentials.licenseKey
        printerId = credentials.printerNum

        if (credentials.mode == "prod") {
            credentials.baseApiUrl = "https://focuslink.focuspos.com/v2/"
            credentials.baseWsUrl = "https://ws.focuslink.focuspos.com/"
        } else {
            credentials.baseApiUrl = "https://dev.focuslink.focuspos.com/v2/"
            credentials.baseWsUrl = "https://dev.ws.focuslink.focuspos.com/"
        }

        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                try{
                    loggly!!.log(
                        LogglyBody(
                            "info",
                            "gatherCredentials",
                            null,
                            null,
                            null
                        )
                    )
                }
                catch(e: Exception){}

            }
        }
    }

    // Display settings
    private fun setDeviceDetails() {
        DeviceDetails.pixelWidth = Resources.getSystem().displayMetrics.widthPixels
        DeviceDetails.pixelHeight = Resources.getSystem().displayMetrics.heightPixels

        when(resources.displayMetrics.densityDpi) {
            DisplayMetrics.DENSITY_LOW -> DeviceDetails.scaleFactor = 1.2f
            DisplayMetrics.DENSITY_MEDIUM -> {
                DeviceDetails.scaleFactor = 1.0f
                DeviceDetails.COLUMN_COUNT = 5
                DeviceDetails.DECORATION_SUM = DeviceDetails.COLUMN_COUNT * 24
            }

            DisplayMetrics.DENSITY_HIGH -> DeviceDetails.scaleFactor = 0.8f
            DisplayMetrics.DENSITY_XHIGH -> DeviceDetails.scaleFactor = 0.6f
            DisplayMetrics.DENSITY_XXHIGH -> DeviceDetails.scaleFactor = 0.5f
            DisplayMetrics.DENSITY_XXXHIGH -> DeviceDetails.scaleFactor = 0.4f
        }

        DeviceDetails.defaultTextSize = 26 * DeviceDetails.scaleFactor
    }

    private fun clearLicenseInfo() {
        val editor = credentials.sharedPreferences.edit()
        editor.putString(credentials.PREFS_LICENSE_KEY, null)
        editor.putString(credentials.PREFS_VENUE_KEY, null)
        editor.putString(credentials.PREFS_MAC_ADDR, null)
        editor.putString(credentials.PREFS_IP_ADDR, null)
        editor.putString(credentials.PREFS_DEVICE_NAME, null)
        editor.putString(credentials.PREFS_LICENSE_SECRET, null)
        editor.apply()

        credentials.venueKey = ""
        credentials.deviceName = ""
        credentials.licenseKey = ""
        credentials.licenseSecret = ""
        credentials.macAddress = credentials.generateFauxMac()
        credentials.mode = "prod"
    }

    private fun clearAllOrders() {
        OrdersModel.orders.clear()
        OrdersModel.bumpedOrders.clear()
        orderAdapter.dataSet.clear()
        OrdersModel.bumpedOrders.add(mutableListOf())
        val editor = credentials.sharedPreferences.edit()
        editor.putString(credentials.PREFS_ORDERS_KEY, null)
        editor.apply()
        orderAdapter.notifyDataSetChanged()
    }

}

// Adds decoration (border/spacing) to orders
class OrderDecoration : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View,
                                parent: RecyclerView, state: RecyclerView.State) {
        with(outRect) {
            val viewPosition = parent.getChildAdapterPosition(view)
            val adapter = parent.adapter as OrderAdapter

            if (viewPosition == adapter.dataSet.size - 1 || adapter.dataSet[viewPosition + 1].isHeader) {
                bottom = 12
            }

            left =  12
            right = 12
        }
    }
}

class RecallDecoration : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View,
                                parent: RecyclerView, state: RecyclerView.State) {
        with(outRect) {
            left =  6
            right = 6
        }
    }
}