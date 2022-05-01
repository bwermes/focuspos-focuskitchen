package com.amorphik.focuskitchen

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.text.Spannable
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.util.Log
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.get
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import com.amorphik.focuskitchen.*
import com.beust.klaxon.Klaxon
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.header_cell.view.*
import kotlinx.android.synthetic.main.order_cell.view.*
import java.util.*
import kotlin.concurrent.thread
import kotlinx.coroutines.*
import java.security.AccessController.getContext

class OrderAdapter : RecyclerView.Adapter<OrderViewHolder>() {
    val dataSet = OrdersModel.orders
    val ORDER_VIEW_TYPE = 1
    val HEADER_VIEW_TYPE = 2
    lateinit var context: Context
    var recyclerView: RecyclerView? = null
    var recallAdapter: RecallAdapter? = null
    var availableSpace = 206
    var lastItem = 0
    var WRAP_SPACE_THRESHOLD = 0
    lateinit var credentials: DeviceCredentials
    var isAnimating = false
    lateinit var mainActivity: MainActivity

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        this.recyclerView = recyclerView
    }

    override fun onViewRecycled(holder: OrderViewHolder) {
    }
    override fun getItemViewType(position: Int): Int {
        return if (dataSet[position].isHeader) HEADER_VIEW_TYPE else ORDER_VIEW_TYPE
    }
    // Number of items
    override fun getItemCount(): Int {
        return dataSet.size
    }
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cellForRow: View

        cellForRow = when (viewType == ORDER_VIEW_TYPE){
            true  -> layoutInflater.inflate(R.layout.order_cell, parent, false)
            false -> layoutInflater.inflate(R.layout.header_cell, parent, false)
        }
        cellForRow.layoutParams.width = (parent.width - DeviceDetails.DECORATION_SUM) / DeviceDetails.COLUMN_COUNT


        val viewHolder = OrderViewHolder(cellForRow)
        viewHolder.view.setOnClickListener {
            val position = viewHolder.adapterPosition
            val orderCells = selectIndices(position)
            if (!dataSet[position].isTagged) {
                tagItem(position, orderCells, false)
            } else {
                tagItem(position, orderCells, true)
            }

            notifyDataSetChanged()
        }

        if (viewType != ORDER_VIEW_TYPE) {
            viewHolder.view.setBackgroundColor(Color.parseColor(prefs.licenseFeatures!!.kitchenHeaderBackgroundColor))

            viewHolder.view.header_overlay.setOnClickListener {
                val dpOfBumpButton = 70
                val dpOfPriorityButton = 70
                val dpOfPrinterButton =if(credentials.bumpToPrinterEnabled) 70 else 0
                var dpOfSmsButton = if(credentials.sms && viewHolder.view.sms_button.visibility == View.VISIBLE) 70 else 0



                var dpToPx = (dpOfBumpButton + dpOfPriorityButton + dpOfPrinterButton + dpOfSmsButton) * Resources.getSystem().displayMetrics.density

                if (viewHolder.view.header_overlay.x == 0f) {
                    dpToPx = -dpToPx
                }

                if (!isAnimating) {
                    isAnimating = true
                    viewHolder.view.header_overlay.animate().translationXBy(dpToPx).setDuration(250).setInterpolator(OvershootInterpolator()).withEndAction {
                        isAnimating = false
                    }.start()
                }

            }

            viewHolder.view.bump_button.setOnClickListener {
                viewHolder.view.header_overlay.x = 0f
                bumpOrder(viewHolder.adapterPosition)
            }

            viewHolder.view.priority_button.setOnClickListener{
                viewHolder.view.header_overlay.x = 0f
                priorityOrder(viewHolder.adapterPosition)
            }

            viewHolder.view.print_button.setOnClickListener {
                if(credentials.bumpToPrinterEnabled){
                    viewHolder.view.header_overlay.x = 0f
                    printPrintOrder(viewHolder.adapterPosition)
                } else{
                    Logger.d("printOnDemand","bumpToPrinterEnabled = false")
                }
            }


            viewHolder.view.sms_button.setOnClickListener {
                Logger.d("smsOrder","button pressed")
                if(credentials.sms){
                    Logger.d("smsOrder","pass credentials")
                    viewHolder.view.header_overlay.x = 0f
                    mainActivity.smsPrintOrderPrompt(dataSet[viewHolder.adapterPosition].orderKey, dataSet[viewHolder.adapterPosition].orderReadySms)

                }
            }
        } else{
            viewHolder.view.setBackgroundColor(Color.parseColor(prefs.licenseFeatures!!.kitchenOrderBackgroundColor))
        }
        Logger.d("printOnDemand","init")
        return viewHolder
    }

    override fun onBindViewHolder(holder: OrderViewHolder, @SuppressLint("RecyclerView") position: Int) {

        if(dataSet[position].isHeader){
            dataSet[position].lastPosition = position


            if(dataSet[position].orderReadySms != null &&  dataSet[position].orderReadySms != ""){

                holder.view.sms_button.visibility = View.VISIBLE
                holder.view.sms_imageView.visibility = View.VISIBLE
            } else{

                holder.view.sms_button.visibility = View.GONE
                holder.view.sms_imageView.visibility = View.GONE

            }

        }

        constructOrderItem(holder, position)
        holder.printOrderSessionKey = dataSet[position].printOrderSessionKey
        holder.printOrderKey = dataSet[position].orderKey

        // Grab the position of the last item on the screen
        // Possibly can be used for wrap before
        // TODO: Figure out how to work this
        if (dataSet[position].isLastItemInOrder) {
            holder.view.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    holder.view.viewTreeObserver.removeOnGlobalLayoutListener(this)

                    availableSpace = getSpaceAfter(holder.view)
                    lastItem = position
                }
            })
        }


        if (availableSpace < WRAP_SPACE_THRESHOLD && dataSet[position].isHeader && position > lastItem) {
            (holder.view.layoutParams as FlexboxLayoutManager.LayoutParams).isWrapBefore = true
            availableSpace = DeviceDetails.pixelHeight
            recyclerView!!.requestLayout()
        } else {
            holder.view.alpha = 1.0f
        }
    }

    private fun tagItem(selectedPosition: Int, cells: MutableList<Int>, untag: Boolean) {
        val recyclerView = if (this.recyclerView == null) return else this.recyclerView!!
        if (selectedPosition >= recyclerView.size ||
            recyclerView[selectedPosition].order_cell_checkMark_imageView == null) return
        val alphaLevel = if (untag) 0.0f else 1.0f

        // Tag Item
        dataSet[selectedPosition].isTagged = !untag
        recyclerView[selectedPosition].order_cell_checkMark_imageView.alpha = alphaLevel

        // Tag Item Modifiers
        val indexAfterPosition = cells.indexOf(selectedPosition) + 1
        for (i in indexAfterPosition.until(cells.size)) {
            val modifierPosition = cells[i]
            if (!dataSet[modifierPosition].isModifier) {
                // Next item is not a modifier
                // Nothing else to tag
                break
            } else {
                val selectedPositionItemLevel = dataSet[selectedPosition].itemLevel!!
                val modifierPositionItemLevel = dataSet[modifierPosition].itemLevel!!

                if (dataSet[modifierPosition].isModifier && modifierPositionItemLevel > selectedPositionItemLevel) {
                    dataSet[modifierPosition].isTagged = !untag

                    if (cells[i] < recyclerView.size) {
                        recyclerView[modifierPosition].order_cell_checkMark_imageView.alpha = alphaLevel
                    }
                }
            }
        }
    }

    private fun highlightSelectedCells(selectedPosition: Int, cells: MutableList<Int>) {
        val recyclerView = if (this.recyclerView == null) return else this.recyclerView!!

        // Highlight the header
        if (selectedPosition == cells.min()) {
            recyclerView[cells[0]].header_cell_bottom_border.setBackgroundColor(Color.YELLOW)
        }
        recyclerView[cells[0]].header_cell_top_border.setBackgroundColor(Color.YELLOW)
        recyclerView[cells[0]].header_cell_left_border.setBackgroundColor(Color.YELLOW)
        recyclerView[cells[0]].header_cell_right_border.setBackgroundColor(Color.YELLOW)


        // Highlights selected cell
        if (selectedPosition != cells.min()) {
            recyclerView[selectedPosition].order_cell_bottom_border.setBackgroundColor(Color.YELLOW)
            recyclerView[selectedPosition].order_cell_top_border.setBackgroundColor(Color.YELLOW)
            recyclerView[selectedPosition].order_cell_left_border.setBackgroundColor(Color.YELLOW)
            recyclerView[selectedPosition].order_cell_right_border.setBackgroundColor(Color.YELLOW)
        }

        // Highlights other order items
        for (i in 1.until(cells.size)) {
            if (cells[i] < recyclerView.size) {
                recyclerView[cells[i]].order_cell_left_border.setBackgroundColor(Color.YELLOW)
                recyclerView[cells[i]].order_cell_right_border.setBackgroundColor(Color.YELLOW)

                if (i == cells.size - 1) {
                    recyclerView[cells[i]].order_cell_bottom_border.setBackgroundColor(Color.YELLOW)
                }
            }
        }
    }
    private fun unhighlightCells() {
        val recyclerView = if (this.recyclerView == null) return else this.recyclerView!!

        for (i in 0.until(recyclerView.size)) {
            // Highlight the header
            if (recyclerView[i].header_cell_bottom_border != null) {
                recyclerView[i].header_cell_bottom_border.setBackgroundColor(
                    Color.TRANSPARENT
                )
                recyclerView[i].header_cell_top_border.setBackgroundColor(
                    ContextCompat.getColor(
                        recyclerView.context,
                        R.color.colorFocusLinkSecondaryGray
                    )
                )
                recyclerView[i].header_cell_left_border.setBackgroundColor(
                    ContextCompat.getColor(
                        recyclerView.context,
                        R.color.colorFocusLinkSecondaryGray
                    )
                )
                recyclerView[i].header_cell_right_border.setBackgroundColor(
                    ContextCompat.getColor(
                        recyclerView.context,
                        R.color.colorFocusLinkSecondaryGray
                    )
                )
            } else {
                recyclerView[i].order_cell_bottom_border.setBackgroundColor(
                    Color.TRANSPARENT
                )
                recyclerView[i].order_cell_top_border.setBackgroundColor(
                    Color.TRANSPARENT
                )
                recyclerView[i].order_cell_left_border.setBackgroundColor(
                    ContextCompat.getColor(recyclerView.context, R.color.colorFocusLinkSecondaryGray)
                )
                recyclerView[i].order_cell_right_border.setBackgroundColor(
                    ContextCompat.getColor(recyclerView.context, R.color.colorFocusLinkSecondaryGray)
                )

                if (i == dataSet.size - 1 || dataSet[i + 1].isHeader) {
                    recyclerView[i].order_cell_bottom_border.setBackgroundColor(
                        ContextCompat.getColor(recyclerView.context, R.color.colorFocusLinkSecondaryGray)
                    )
                }
            }
        }
    }

    fun bumpOrder(position: Int) {
        Logger.d("printOrder-Adapter","onBumpOrder called for position ${position}")
        var orderKey: String? = null

        try{


            val toRemove = selectIndices(position)
            val bumpedItems = mutableListOf<OrderAdapterDataItem>()
            for (item in toRemove.reversed()) {
                bumpedItems.add(0, dataSet[item])
            }

            if(credentials.smsOnBump && bumpedItems[0].orderReadySms != null && bumpedItems[0].orderReadySms != ""){
                if(credentials.smsOnBumpPrompt){
                    Thread{
                        mainActivity.smsPrintOrderPrompt(bumpedItems[0].orderKey, bumpedItems[0].orderReadySms)
                    }.run()
                }else{
                    Thread{
                        mainActivity.smsPrintOrderSend(bumpedItems[0].orderKey)
                    }.run()
                }
            }


            if (OrdersModel.bumpedOrders.size > 20) {
                OrdersModel.bumpedOrders.removeAt(20)
            }
            OrdersModel.bumpedOrders.add(0, bumpedItems)
            orderKey = bumpedItems[0].orderKey

            if (OrdersModel.bumpedOrders[1].isEmpty()) {
                recallAdapter?.dataSet = OrdersModel.bumpedOrders[0]
                recallAdapter?.position = 0
            } else {
                recallAdapter!!.position += 1
            }

            recallAdapter?.notifyDataSetChanged()
            val payload = """{
                                "message": "printorder-pending",
                                "key": "${bumpedItems[0].orderKey}"
                             }"""


            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    try{
                        loggly!!.log(
                            LogglyBody(
                                "info",
                                "bumpOrder",
                                Gson().toJson(bumpedItems[0]),
                                bumpedItems[0].checkNum
                            )
                        )
                    }
                    catch(e: Exception){}

                }
            }
            Networking.postData(url = "${credentials.baseApiUrl}stores/${credentials.venueKey}/printorders/bump",
                headerName = "Authorization",
                headerValue = credentials.generateDeviceLicenseHeader(),
                payload = payload) { _, _, _ -> }

            for (itemPosition in toRemove.reversed()) {
                if (itemPosition < recyclerView!!.size) {
                    dataSet[itemPosition].isAnimating = true
                    recyclerView!![itemPosition].animate().alpha(0f).setDuration(150).withEndAction {
                        if (dataSet[itemPosition].isHeader) {
                            recyclerView?.getChildAt(itemPosition)?.header_overlay?.x = 0f
                        }

                        //Update the recyclerView
                        notifyDataSetChanged()
                        recyclerView!![itemPosition].alpha = 1.0f

                        dataSet.removeAt(itemPosition)

                        if (itemPosition == toRemove.min()!!) {
                            Thread {
                                updateSavedOrders()
                            }.start()

                            wrapNextColumn(0, itemPosition)
                        }
                    }
                } else {
                    dataSet.removeAt(itemPosition)

                    //Update the recyclerView
                    notifyDataSetChanged()
                }
            }
        }
        catch(e: Exception){
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    try{
                        loggly!!.log(
                            LogglyBody(
                                "error",
                                "bumpOrderError",
                                null,
                                orderKey,
                                Gson().toJson(e.message)
                            )
                        )
                    }
                    catch(e: Exception){}

                }
            }
        }
    }

    fun printPrintOrder(position: Int){
        Logger.d("printOnDemand","init printPrintOrder")
        val printOrder = dataSet.find { i -> i.lastPosition == position }
        if(printOrder != null){
            CoroutineScope(Dispatchers.IO).launch {
                withContext(Dispatchers.Main) {
                    try{
                        val response = api!!.printPrintOrder(credentials.venueKey.toInt(), printOrder.orderKey)
                        Logger.d("printOnDemand","response status = ${response.status}")
                    }
                    catch(e: Exception){
                        Logger.e("printOnDemand","${e.message}",false)
                    }

                }
            }
        }

    }

    fun priorityOrder(position: Int){
        dataSet[position].isPriority = true;
        notifyDataSetChanged()
        val payload = """{
                                "message": "printorder-priority",
                                "key": "${dataSet[position].orderKey}"
                             }"""


        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                try{
                    loggly!!.log(
                        LogglyBody(
                            "info",
                            "priorityOrder",
                            Gson().toJson(dataSet[position]),
                            dataSet[position].checkNum
                        )
                    )
                }
                catch(e: Exception){}

            }
        }
        Networking.postData(url = "${credentials.baseApiUrl}stores/${credentials.venueKey}/printorders/priority",
            headerName = "Authorization",
            headerValue = credentials.generateDeviceLicenseHeader(),
            payload = payload) { _, _, _ -> }

    }

    fun wrapNextColumn(startPosition: Int, recyclerIndex: Int) {
        // Base case
        if (startPosition == DeviceDetails.COLUMN_COUNT - 1) {
            return
        }

        if (startPosition == 0) {
            for (i in recyclerIndex.until(recyclerView!!.size)) {
                (recyclerView!![i].layoutParams as FlexboxLayoutManager.LayoutParams).isWrapBefore = false
            }
        }

        // Perform for each column
        recyclerView!!.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                for (i in recyclerIndex.until(recyclerView!!.size)) {
                    val space = getSpaceAfter(recyclerView!![i])
                    if (space < WRAP_SPACE_THRESHOLD && i + 1 < recyclerView!!.size && recyclerView!![i + 1].header_cell_top_border != null) {
                        (recyclerView!![i + 1].layoutParams as FlexboxLayoutManager.LayoutParams).isWrapBefore =
                            true
                        recyclerView!!.requestLayout()
                        wrapNextColumn(startPosition + 1, i + 1)
                        break
                    }
                }

                recyclerView!!.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }
    fun updateSavedOrders() {
        val editor = credentials.sharedPreferences.edit()
        val ordersAsJson = Klaxon().toJsonString(OrdersModel.orders)
        editor.putString(credentials.PREFS_ORDERS_KEY, ordersAsJson)
        editor.apply()
    }
    private fun constructOrderItem(holder: OrderViewHolder, position: Int) {
        if (!dataSet[position].isHeader) {
            val itemText = holder.view.order_cell_order_text
            val itemCheckMark = holder.view.order_cell_checkMark_imageView
            val quantityText = if (dataSet[position].quantity > 1) "${dataSet[position].quantity}" else ""
            itemText.text = "${quantityText}  ${dataSet[position].itemName}"
            //itemText.textSize = DeviceDetails.defaultTextSize
            if (dataSet[position].isModifier) {
                itemText.setTextColor(Color.parseColor(prefs.licenseFeatures!!.kitchenModifierFontColor))
                itemText.setTextSize(prefs.licenseFeatures!!.kitchenModifierFontSize.toFloat())
                Logger.d("customUi","setting modifier fontSize ${prefs.licenseFeatures!!.kitchenModifierFontSize.toFloat()}")
            } else {
                itemText.setTextColor(Color.parseColor(prefs.licenseFeatures!!.kitchenItemFontColor))
                itemText.setTextSize(prefs.licenseFeatures!!.kitchenItemFontSize.toFloat())
                Logger.d("customUi","setting item fontSize ${prefs.licenseFeatures!!.kitchenItemFontSize.toFloat()}")
            }

            if (dataSet[position].voided) {
                itemText.setText(itemText.text, TextView.BufferType.SPANNABLE)
                val spannable: Spannable = itemText.text as Spannable
                var spot = 0
                for (i in 0.until(itemText.text.length)) {
                    if (itemText.text[i] != ' ') {
                        spot = i
                        break
                    }
                }
                spannable.setSpan(StrikethroughSpan(), spot, itemText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                itemText.paintFlags = itemText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            }

            itemCheckMark.alpha = if (dataSet[position].isTagged) 1.0f else 0.0f

            // Item is the last item of the order
            if (dataSet[position].isLastItemInOrder) {
                    holder.view.order_cell_bottom_border.setBackgroundColor(
                        ContextCompat.getColor(
                            holder.view.context,
                            R.color.colorFocusLinkSecondaryGray
                        )
                    )
                } else {
                    holder.view.order_cell_bottom_border.setBackgroundColor(Color.TRANSPARENT)
                }
        } else {
            if (dataSet[position].isUnbumped) {
                holder.view.header_overlay.setBackgroundColor(Color.MAGENTA)
                holder.view.header_layout.setBackgroundColor(Color.MAGENTA)
            } else {
                Logger.d("printorder-complete","constructOrderItem ${position}")
                if (dataSet[position].minutesInSystem < credentials.urgentTime) {
                    Logger.d("printorder-complete","order is normal")
//                    holder.view.header_overlay.setBackgroundColor(Color.rgb(0, 153, 204))
//                    holder.view.header_layout.setBackgroundColor(Color.rgb(0, 153, 204))
                    holder.view.header_overlay.setBackgroundColor(Color.parseColor(prefs.licenseFeatures!!.kitchenHeaderBackgroundColor))
                    holder.view.header_layout.setBackgroundColor(Color.parseColor(prefs.licenseFeatures!!.kitchenHeaderBackgroundColor))

                } else {
                    Logger.d("printorder-complete","order is urgent")
                    holder.view.header_overlay.setBackgroundColor(Color.parseColor(prefs.licenseFeatures!!.kitchenUrgentHeaderBackgroundColor))
                    holder.view.header_layout.setBackgroundColor(Color.parseColor(prefs.licenseFeatures!!.kitchenUrgentHeaderBackgroundColor))
                }

                if(dataSet[position].isPriority){
                    Logger.d("printorder-complete","order is priority")
                    holder.view.header_overlay.setBackgroundColor(Color.parseColor(prefs.licenseFeatures!!.kitchenPriorityHeaderBackgroundColor))
                    holder.view.header_layout.setBackgroundColor(Color.parseColor(prefs.licenseFeatures!!.kitchenPriorityHeaderBackgroundColor))
                }

                if(dataSet[position].isComplete){
                    Logger.d("printorder-complete","order is complete")
                    holder.view.header_overlay.setBackgroundColor(Color.parseColor(prefs.licenseFeatures!!.kitchenCompleteHeaderBackgroundColor))
                    holder.view.header_layout.setBackgroundColor(Color.parseColor(prefs.licenseFeatures!!.kitchenCompleteHeaderBackgroundColor))
                }

            }
            holder.view.header_cell_server_text.text = dataSet[position].server
            holder.view.header_cell_time_text.text = dataSet[position].timeInSystem
            if (dataSet[position].table == "Table ") {
                holder.view.header_cell_table_name_text.text = dataSet[position].checkNum
            } else {
                holder.view.header_cell_table_name_text.text = dataSet[position].table
            }
            holder.view.header_cell_order_type_text.text = dataSet[position].orderType

            if(dataSet[position].smsCount > 0){
                Logger.d("smsOrder","after sms count = ${dataSet[position].smsCount}")
                holder.view.header_cell_sms_block.visibility = View.VISIBLE
                holder.view.header_cell_sms_sent.text = "(${dataSet[position].smsCount.toString()})"
            }

//            holder.view.header_cell_server_text.textSize = DeviceDetails.defaultTextSize
//            holder.view.header_cell_time_text.textSize = DeviceDetails.defaultTextSize
//            holder.view.header_cell_table_name_text.textSize = DeviceDetails.defaultTextSize
//            holder.view.header_cell_order_type_text.textSize = DeviceDetails.defaultTextSize
            holder.view.header_cell_server_text.textSize = prefs.licenseFeatures!!.kitchenHeaderFontSize.toFloat()
            holder.view.header_cell_time_text.textSize = prefs.licenseFeatures!!.kitchenHeaderFontSize.toFloat()
            holder.view.header_cell_table_name_text.textSize = prefs.licenseFeatures!!.kitchenHeaderFontSize.toFloat()
            holder.view.header_cell_order_type_text.textSize = prefs.licenseFeatures!!.kitchenHeaderFontSize.toFloat()

        }
    }

    private fun selectIndices(position: Int) : MutableList<Int> {
        // List to hold indices to remove from the data set
        val indices = mutableListOf(position)

        // Handle remove based on type of view tapped
        // TODO(): May be able to improve this and remove the if statement
        if (position > 0 && dataSet[position].isHeader) {
            var counter = position + 1
            while (counter < dataSet.size && !dataSet[counter].isHeader) {
                indices.add(counter)
                counter += 1
            }
        } else {
            var counter = if (position > 0) position - 1 else 0
            while (counter >= 0 && !dataSet[counter].isHeader) {
                indices.add(counter)
                counter -= 1
            }

            counter = position + 1
            while (counter < dataSet.size && !dataSet[counter].isHeader) {
                indices.add(counter)
                counter += 1
            }
        }

        // Sort and remove selected indices
        indices.sort()
        return indices
    }

    private fun getSpaceAfter(view : View) : Int {
        val locs = IntArray(2)
        view.getLocationOnScreen(locs)
        val availableSpace = DeviceDetails.pixelHeight - locs[1]
        return availableSpace
    }
}

class OrderViewHolder(val view: View,
                      var printOrderSessionKey: String? = null,
                      var printOrderKey: String? = null): RecyclerView.ViewHolder(view) {

}

data class PrintOrderSessionPosition(
    var position: Int = 0,
    var printOrderSessionKey: String? = null,
    var printOrderKey: String? = null
)

