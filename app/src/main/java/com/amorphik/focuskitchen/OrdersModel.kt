package com.amorphik.focuskitchen

import android.util.Log
import java.util.*

class OrdersModel {
    companion object {
        var orders = mutableListOf<OrderAdapterDataItem>()
        var bumpedOrders = mutableListOf<MutableList<OrderAdapterDataItem>>(mutableListOf())
        var heldOrders = mutableListOf<MutableList<OrderAdapterDataItem>>()

        fun addOrder(order: Order) {
            val orderType = order.orderType
            val server = order.server
            val table = order.table
            val time = Date().time
            val checkNum =  order.check
            val orderKey = order.key
            val printOrderSessionKey = order.printOrderSessionKey
            val orderReadySms = order.orderReadySms
            val orderReadySmsCount = order.smsCount
            var delayTime = 0
            if (order.delayTime > 0) {
                delayTime = order.delayTime + 1
            }

            Logger.d("smsOrder","OrderReadySms value =${orderReadySms}")

            val headerItem = OrderAdapterDataItem(
                isModifier = false,
                isHeader = true,
                isLastItemInOrder = false,
                orderType = orderType,
                server = server,
                time = time,
                table = table,
                itemName = "",
                itemLevel = 0,
                checkNum = checkNum,
                voided = false,
                quantity = 0,
                printOrderSessionKey = printOrderSessionKey,
                orderReadySms = orderReadySms)

            headerItem.orderKey = orderKey
            headerItem.delayTime = delayTime

            val heldOrder = mutableListOf<OrderAdapterDataItem>()
            if (delayTime == 0) {
                orders.add(headerItem)
            } else {
                heldOrder.add(headerItem)
            }

            for (i in 0.until(order.items.size)) {
                val item = order.items[i]
                val itemIsModifier = item.level > 0
                var remoteName = item.remoteName
                val lastItem = i == order.items.size - 1
                val voided = item.voided
                val quantity = item.quantity.toInt()
                for (j in 1..item.level) {
                    remoteName = "   $remoteName"
                }
                if (item.level == 0) {
                    remoteName = remoteName.subSequence(2, remoteName.length).toString()
                }
                val orderItem = OrderAdapterDataItem(
                    isModifier = itemIsModifier,
                    isHeader = false,
                    isLastItemInOrder = lastItem,
                    orderType = orderType,
                    server = server,
                    time = time,
                    table = table,
                    itemName = remoteName,
                    itemLevel = item.level,
                    checkNum = checkNum,
                    voided = voided,
                    quantity = quantity,
                    printOrderSessionKey = printOrderSessionKey,
                    orderReadySms = null)
                orderItem.delayTime = delayTime
                if (delayTime == 0) {
                    orders.add(orderItem)
                } else {
                    heldOrder.add(orderItem)
                }
            }

            if (delayTime > 0) {
                heldOrders.add(heldOrder)
                println("Delayed order ${heldOrder[0].checkNum} received - Holding for ${heldOrder[0].delayTime} minutes.")
                println("Currently held orders: ${heldOrders.size}")
            }
        }

        fun findOrdersToBump(printOrderKey: String): OrderAdapterDataItem? {
            return orders.find { i -> i.orderKey == printOrderKey }
        }

        fun markOrderComplete(printOrderKey: String): Boolean{
            val order = orders.find { i -> i.orderKey == printOrderKey }
            if (order != null) {
                order.isComplete = true
                return true
            };
            return false
        }

        fun markOrderPriority(printOrderKey: String): Boolean{
            val order = orders.find { i -> i.orderKey == printOrderKey }
            if (order != null) {
                order.isPriority = true
                return true
            };
            return false
        }
    }
}