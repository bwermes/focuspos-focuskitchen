package com.amorphik.focuskitchen

import java.io.Serializable

data class CheckDto(
    var employeeId: Int,
    var checkKey: String? = null,
    var checkNumber: Int,
    var timeOpened: String,
    var seats: MutableList<CheckSeatDto>,
    val venueKey : Int = 0,
    val created : String = "",
    val updated : String = "",
    val timestamp : String = "",
    val businessDate : String = "",
    val table : String = "",
    val owner : String = "",
    val status : Int = 0,
    val pos : Boolean = false,
    val orderType : Int = 0,
    val subtotal : Double = 0.0,
    val taxtotal : Double = 0.0,
    val discountTotal : Int = 0,
    val orderTypeChargeTotal : Int = 0,
    val numberOfGuests: Int = 0,
    val gratuityTotal : Int = 0,
    val creditCardPassAlongFee : Double = 0.0,
    val total : Double = 0.0,
    val remainingBalance : Int = 0,
    val open : Boolean = false,
    val used : Boolean = false,
    val dueDateTime : String = "",
    val loyaltyStatus : Int = 0,
    val customerKey : String = "",
    val receiptToken : String = "",
    val receiptUrl : String = "",
    val receiptShortUrl : String = "",
    val ooUrl : String = "",
    val ooShortUrl : String = ""
): Serializable

data class CheckSeatDto(
    val seatKey: Int? = null,
    var items: MutableList<CheckItemDto>? = null,
    var payments: MutableList<CheckPaymentDto>? = null,
    var transactions: MutableList<CheckTransactionDto>? = null
): Serializable

data class CheckItemDto(
    val itemKey: Int,
    val menuItemKey: Int,
    var name: String,
    val quantity: Double,
    var price: Double,
    var priceNumber: Int? = null,
    var priceNumberName: String? = null,
    val prepModCommand: Int? = null,
    var courseId: Int? = null,
    var positionId: Int? = null,
    var kitchenComment: Boolean? = null,
    var void: Boolean? = null,
    var modifiers: MutableList<CheckItemDto>? = null,
    val key: Int? = 0
): Serializable

data class CheckPaymentDto(
    val amount: Double,
    val tip: Double,
    val paymentName: String,
    val paymentType: String,
    val invoiceNumber: String,
    val token: String? = null,
    val focusLinkCredit: Boolean? = null,
    val type: Int? = null
): Serializable

data class CheckTransactionDto(
    val transactionKey: String,
    val amount: Double,
    val tip: Double,
    val type: Int,
    val account: String?,
    val displayName: String,
    val shift4: String? = null,
    val ccProcessor: Int?,
    val status: Int?,
    val created: String
): Serializable