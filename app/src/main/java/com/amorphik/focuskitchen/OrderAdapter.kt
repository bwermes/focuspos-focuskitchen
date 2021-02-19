package com.example.focuskitchen

import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.text.Spannable
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.view.*
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.view.size
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.Klaxon
import com.google.android.flexbox.FlexboxLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.header_cell.view.*
import kotlinx.android.synthetic.main.order_cell.view.*
import java.util.*
import kotlin.concurrent.thread

class OrderAdapter : RecyclerView.Adapter<OrderViewHolder>() {
    val dataSet = OrdersModel.orders
    val ORDER_VIEW_TYPE = 1
    val HEADER_VIEW_TYPE = 2
    var recyclerView: RecyclerView? = null
    var recallAdapter: RecallAdapter? = null
    var availableSpace = 206
    var lastItem = 0
    var WRAP_SPACE_THRESHOLD = 0
    lateinit var credentials: DeviceCredentials
    var isAnimating = false

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        this.recyclerView = recyclerView
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
            viewHolder.view.header_overlay.setOnClickListener {
                val dpOfBumpButton = 75
                var dpToPx = dpOfBumpButton * Resources.getSystem().displayMetrics.density

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
        }

        return viewHolder
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        constructOrderItem(holder, position)

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
                        R.color.colorAccent
                    )
                )
                recyclerView[i].header_cell_left_border.setBackgroundColor(
                    ContextCompat.getColor(
                        recyclerView.context,
                        R.color.colorAccent
                    )
                )
                recyclerView[i].header_cell_right_border.setBackgroundColor(
                    ContextCompat.getColor(
                        recyclerView.context,
                        R.color.colorAccent
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
                    ContextCompat.getColor(recyclerView.context, R.color.colorAccent)
                )
                recyclerView[i].order_cell_right_border.setBackgroundColor(
                    ContextCompat.getColor(recyclerView.context, R.color.colorAccent)
                )

                if (i == dataSet.size - 1 || dataSet[i + 1].isHeader) {
                    recyclerView[i].order_cell_bottom_border.setBackgroundColor(
                        ContextCompat.getColor(recyclerView.context, R.color.colorAccent)
                    )
                }
            }
        }
    }

    private fun bumpOrder(position: Int) {
        val toRemove = selectIndices(position)
        val bumpedItems = mutableListOf<OrderAdapterDataItem>()
        for (item in toRemove.reversed()) {
            bumpedItems.add(0, dataSet[item])
        }

        if (OrdersModel.bumpedOrders.size > 20) {
            OrdersModel.bumpedOrders.removeAt(20)
        }
        OrdersModel.bumpedOrders.add(0, bumpedItems)
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
    private fun updateSavedOrders() {
        val editor = credentials.prefs.edit()
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
            itemText.textSize = DeviceDetails.defaultTextSize
            if (dataSet[position].isModifier) {
                itemText.setTextColor(Color.GREEN)
            } else {
                itemText.setTextColor(Color.WHITE)
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
                            R.color.colorAccent
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
                if (dataSet[position].minutesInSystem < credentials.urgentTime) {
                    holder.view.header_overlay.setBackgroundColor(Color.rgb(0, 153, 204))
                    holder.view.header_layout.setBackgroundColor(Color.rgb(0, 153, 204))
                } else {
                    holder.view.header_overlay.setBackgroundColor(Color.RED)
                    holder.view.header_layout.setBackgroundColor(Color.RED)
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

            holder.view.header_cell_server_text.textSize = DeviceDetails.defaultTextSize
            holder.view.header_cell_time_text.textSize = DeviceDetails.defaultTextSize
            holder.view.header_cell_table_name_text.textSize = DeviceDetails.defaultTextSize
            holder.view.header_cell_order_type_text.textSize = DeviceDetails.defaultTextSize

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

class OrderViewHolder(val view: View): RecyclerView.ViewHolder(view) {

}