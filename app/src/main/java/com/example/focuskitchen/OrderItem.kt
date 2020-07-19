package com.example.focuskitchen

class OrderItem(val type: String,
                val id: Int,
                val parentItemId: Int,
                val quantity: Float,
                val itemRecordNumber: Int,
                val itemName: String,
                val remoteName: String,
                val course: String?,
                val position: Int,
                val level: Int,
                val price: Float,
                val cookTime: Int,
                val voided: Boolean) {


}