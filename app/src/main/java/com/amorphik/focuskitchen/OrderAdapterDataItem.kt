package com.amorphik.focuskitchen

class OrderAdapterDataItem (val isModifier: Boolean,
                            val isHeader: Boolean,
                            val isLastItemInOrder: Boolean,
                            val orderType: String,
                            val server: String,
                            val time: Long,
                            val table: String?,
                            var itemName: String?,
                            val itemLevel: Int?,
                            val checkNum: String?,
                            val voided: Boolean,
                            val quantity: Int = 1) {

    var isTagged = false
    var isUnbumped = false
    var timeInSystem = "<1m"
    var minutesInSystem: Long = 0
    var isAnimating = false
    var orderKey = ""
    var delayTime: Int = 0
}