package com.amorphik.focuskitchen

import android.content.res.Resources
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.beust.klaxon.Klaxon
import kotlinx.android.synthetic.main.header_cell.view.*
import kotlinx.android.synthetic.main.order_cell.view.*

class RecallAdapter : RecyclerView.Adapter<OrderViewHolder>() {
    var dataSet = OrdersModel.bumpedOrders[0]
    var position = 0
    val ORDER_VIEW_TYPE = 1
    val HEADER_VIEW_TYPE = 2
    var recyclerView: RecyclerView? = null
    var isAnimating = false
    lateinit var credentials: DeviceCredentials
    lateinit var positionTextView: TextView
    lateinit var orderAdapter: OrderAdapter

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        this.recyclerView = recyclerView
    }

    // Determine what type of view needs to be created
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
        cellForRow.layoutParams.width = (parent.width - DeviceDetails.DECORATION_SUM/8)

        return OrderViewHolder(cellForRow)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        constructOrderItem(holder, position)
        positionTextView.text = "${this.position + 1} / ${OrdersModel.bumpedOrders.size - 1}"

        if (dataSet[position].isHeader) {
            holder.view.bump_button.setBackgroundColor(Color.MAGENTA)
            holder.view.bump_imageView.setImageResource(R.drawable.unbump)

            holder.view.header_overlay.setOnClickListener {
                val dpOfBumpButton = 75
                var dpToPx = dpOfBumpButton * Resources.getSystem().displayMetrics.density

                if (holder.view.header_overlay.x == 0f) {
                    dpToPx = -dpToPx
                }

                if (!isAnimating) {
                    isAnimating = true
                    holder.view.header_overlay.animate().translationXBy(dpToPx).setDuration(250).setInterpolator(
                        OvershootInterpolator()
                    ).withEndAction {
                        isAnimating = false
                    }.start()
                }
            }

            holder.view.bump_button.setOnClickListener {
                unbumpOrder()
                holder.view.header_overlay.x = 0f
                notifyDataSetChanged()
            }

        }
    }

    private fun unbumpOrder() {
        for (item in 0.until(dataSet.size).reversed()) {
            dataSet[item].isUnbumped = true
            OrdersModel.orders.add(0, dataSet[item])
        }

        orderAdapter.notifyDataSetChanged()
        orderAdapter.wrapNextColumn(0, 1)
        OrdersModel.bumpedOrders.removeAt(position)
        if (position > 0) {
            dataSet = OrdersModel.bumpedOrders[position - 1]
            position -= 1
        } else {
            dataSet = OrdersModel.bumpedOrders[0]
            position = 0
        }

        if (OrdersModel.bumpedOrders[0].isEmpty()) {
            positionTextView.text = "0 / 0"
        }

        notifyDataSetChanged()

        Thread {
            val editor = credentials.sharedPreferences.edit()
            val ordersAsJson = Klaxon().toJsonString(OrdersModel.orders)
            editor.putString(credentials.PREFS_ORDERS_KEY, ordersAsJson)
            editor.apply()
        }.start()
    }

    fun moveToNextOrder () {
        if (position < OrdersModel.bumpedOrders.size - 2) {
            position += 1
        }
        if (position > 20) {
            position = 19
        }
        dataSet = OrdersModel.bumpedOrders[position]
        notifyDataSetChanged()
    }

    fun moveToPrevOrder() {
        if (position > 20) {
            position = 20
        }
        if (position > 0) {
            position -= 1
        }

        dataSet = OrdersModel.bumpedOrders[position]
        notifyDataSetChanged()
    }

    private fun constructOrderItem(holder: OrderViewHolder, position: Int) {
        if (!dataSet[position].isHeader) {
            val quantityText = if (dataSet[position].quantity > 1) "${dataSet[position].quantity}" else ""
            holder.view.order_cell_order_text.text = "${quantityText}  ${dataSet[position].itemName}"
            holder.view.order_cell_order_text.textSize = DeviceDetails.defaultTextSize
            if (dataSet[position].isModifier) {
                holder.view.order_cell_order_text.setTextColor(Color.GREEN)
            } else {
                holder.view.order_cell_order_text.setTextColor(Color.WHITE)
            }

            holder.view.order_cell_checkMark_imageView.visibility = ImageView.INVISIBLE

            if (position == dataSet.size - 1 || dataSet[position + 1].isHeader) {
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
            holder.view.header_cell_server_text.text = dataSet[position].server
            holder.view.header_cell_time_text.text = dataSet[position].timeInSystem
            holder.view.header_overlay.x = 0f
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

            dataSet[position].isAnimating = false
        }
    }
}