package com.amorphik.focuskitchen

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

data class AllDayCountRecord(
    var menuItemName: String,
    var count: Float,
    var displayCount: String
)

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

data class CheckSurvey (
    val checkKey: String,
    val seatKey: Int,
    val happy: Boolean? = null
): Serializable

data class DailySales (
    @SerializedName("quantity"     ) var quantity     : Int?                   = null,
    @SerializedName("revenue"      ) var revenue      : Int?                   = null,
    @SerializedName("discount"     ) var discount     : Int?                   = null,
    @SerializedName("businessDate" ) var businessDate : String?                = null,
    @SerializedName("dayOfWeek") var dayOfWeek : String? = null,
    @SerializedName("hourlySales"  ) var hourlySales  : ArrayList<HourlySales> = arrayListOf()
): Serializable

data class Employee (
    val venueKey : Int,
    val ID : Int,
    val FirstName : String,
    val LastName : String,
    val NickName : String,
    val ID1 : String,
    val ID2 : String,
    val Address : String,
    val City : String,
    val State : String,
    val ZipCode : String,
    val Phone1 : String,
    val Email : String,
    val BirthDate : String,
    val HireDate : String,
    val LastRaiseDate : String,
    val EmergencyPhone : String,
    val EmergencyContact : String,
    val W4Allowances : Int,
    val W4Status : Int,
    val Picture : String,
    val Language : Int,
    val FoodHandlerCertDate : String,
    val BartenderCardDate : String,
    val RequireCard : Boolean,
    val EnforceScheduling : Boolean,
    val ExtendRights : Boolean,
    val ClockInOutOnly : Boolean,
    val FingerprintAtClockIn : Boolean,
    val FingerprintRequired : Boolean,
    val IsDeleted : Boolean,
    val IsTerminated : Boolean,
    val IsInactive : Boolean,
    val IsActive : Boolean,
    val Status : String,
    val TerminationDate : String,
    val TerminationID : String,
    val EmployeeJob : List<EmployeeAvailableJob>
) : Serializable

data class EmployeeMessage(
    val author: String,
    val authorEmail: String,
    val sk: String,
    val subject: String,
    val messageBody: String,
    val sentDateTime: String,
    val unread: Boolean,
    val requireReply: Boolean,
    val created: String,
    val fileName: String
): Serializable

data class EmployeeMessageReadBody(
    var fileName: String
)

data class EmployeeAvailableJob (
    val venueKey : Int,
    val Position : Int,
    val EmployeeRecordID : Int,
    val Rate : Double,
    val ID : Int,
    val Job : JobTypes
): Serializable

data class LogPayload(
    var message: String,
    var context: String,
    var dateTime: String
): Serializable

data class JobTypes (
    val VenueKey : Int,
    val ID : Int,
    val Name : String,
    val LaborGroupID : Int,
    val ExternalID : String,
    val HotSchedulesID : Int,
    val ScheduleID : Int
)

data class GiftBalanceResponse(
    val responseOrigin: String,
    val cmdStatus: String,
    val textResponse: String,
    val tranCode: String,
    val operatorID: String,
    val acctNo: String,
    val balance: Double
)

data class ValidateCheckResponse(
    val checkTotal: Double?,
    val taxTotal: Double?,
    val orderTypeFee: Double?,
    val creditCardPassAlongFee: Double?,
    val outOfStock: List<Int>?
)

data class ItemSingleButton (
    val id: String,
    val name: String?,
    var idx: Int? = null,
    var disabled: Boolean? = null
)

data class FocusLinkApiCommandResponse(
    @SerializedName("status") val status: String,
    @SerializedName("statuCode") val statusCode: Int,
    @SerializedName("message") val message: String
)

data class HourlySales (

    @SerializedName("quantity" ) var quantity : Int?    = null,
    @SerializedName("revenue"  ) var revenue  : Int?    = null,
    @SerializedName("discount" ) var discount : Int?    = null,
    @SerializedName("hour"     ) var hour     : String? = null,
@SerializedName("hourBlockName") var hourBlockName : String? = null
)

data class License(
    @SerializedName("key"              ) var key              : String?   = null,
    @SerializedName("secret") var secret: String? = null,
   @SerializedName("name"             ) var name             : String?   = null,
   @SerializedName("type"             ) var type             : String?   = null,
   @SerializedName("venueKey"         ) var venueKey         : Int?      = null,
   @SerializedName("dealerKey"        ) var dealerKey        : String?   = null,
   @SerializedName("integratorKey"    ) var integratorKey    : String?   = null,
   @SerializedName("bundleKey"        ) var bundleKey        : String?   = null,
   @SerializedName("mac"              ) var mac              : String?   = null,
   @SerializedName("ip"               ) var ip               : String?   = null,
   @SerializedName("printerId"        ) var printerId        : Int?      = null,
   @SerializedName("stationId"        ) var stationId        : String?   = null,
   @SerializedName("posStationId"     ) var posStationId     : String?   = null,
   @SerializedName("claimed"          ) var claimed          : Boolean?  = null,
   @SerializedName("active"           ) var active           : Boolean?  = null,
   @SerializedName("created"          ) var created          : String?   = null,
   @SerializedName("updated"          ) var updated          : String?   = null,
   @SerializedName("lastUsed"         ) var lastUsed         : String?   = null,
   @SerializedName("softwareVersion"  ) var softwareVersion  : String?   = null,
   @SerializedName("posVersion"       ) var posVersion       : String?   = null,
   @SerializedName("deviceType"       ) var deviceType       : String?   = null,
   @SerializedName("price"            ) var price            : String?   = null,
   @SerializedName("paymentType"      ) var paymentType      : String?   = null,
   @SerializedName("paymentName"      ) var paymentName      : String?   = null,
   @SerializedName("sourceName"       ) var sourceName       : String?   = null,
   @SerializedName("suppressEmailSms" ) var suppressEmailSms : Int?      = null,
   @SerializedName("features"         ) var features         : LicenseFeatures? = LicenseFeatures()

)


data class LicenseFeatures (
    @SerializedName("pay") var pay : Boolean? = false,
    @SerializedName("order") var order: Boolean? = false,
    @SerializedName("loyaltyTab") var loyaltyTab: Boolean? = false,
    @SerializedName("survey") var survey: Boolean? = false,
    @SerializedName("loyalty") var loyalty: Boolean? = false,
    @SerializedName("timeclock") var timeclock: Boolean? = false,
    @SerializedName("manager") var manager: Boolean? = false,
    @SerializedName("kiosk") var kiosk: Boolean? = false,
    @SerializedName("host") var host: Boolean? = false,
    @SerializedName("kitchen") var kitchen: Boolean? = false,
    @SerializedName("expo") var expo: Boolean? = false,
    @SerializedName("orderReady") var orderReady: Boolean? = false,
    @SerializedName("onServerPay") var onServerPay           : Boolean?          = false,
    @SerializedName("nfcPay") var nfcPay                : Boolean?          = false,
    @SerializedName("giftCardPay") var giftCardPay           : Boolean?          = false,
    @SerializedName("customer") var customer              : Boolean?          = false,
    @SerializedName("cashPay") var cashPay               : Boolean?          = false,
    @SerializedName("qrPay") var qrPay                 : Boolean?          = false,
    @SerializedName("sms") var sms                   : Boolean?          = false,
    @SerializedName("smsOnBump") var smsOnBump: Boolean? = false,
    @SerializedName("smsOnBumpPrompt") var smsOnBumpPrompt: Boolean? = true,
    @SerializedName("promptTableNumber") var promptTableNumber     : Boolean?          = false,
    @SerializedName("promptPosition") var promptPosition        : Boolean?          = false,
    @SerializedName("promptTip") var promptTip             : Boolean?          = false,
    @SerializedName("bumpToPrinterIdList") var bumpToPrinterIdList : List<String>? = emptyList(),
    @SerializedName("bumpFromPrinterIdList") var bumpFromPrinterIdList: List<Int>? = emptyList(),
    @SerializedName("kitchenHeaderFontSize") var kitchenHeaderFontSize: Int? = 20,
    @SerializedName("kitchenItemFontSize") var kitchenItemFontSize: Int? = 30,
    @SerializedName("kitchenModifierFontSize") var kitchenModifierFontSize: Int? = 30,
    @SerializedName("kitchenHeaderBackgroundColor") var kitchenHeaderBackgroundColor: String? = "#56707D",
    @SerializedName("kitchenUrgentHeaderBackgroundColor") var kitchenUrgentHeaderBackgroundColor: String? = "#FF0000",
    @SerializedName("kitchenCompleteHeaderBackgroundColor") var kitchenCompleteHeaderBackgroundColor: String? = "#5E8F32",
    @SerializedName("kitchenPriorityHeaderBackgroundColor") var kitchenPriorityHeaderBackgroundColor: String? = "#f99d24",
    @SerializedName("kitchenOrderBackgroundColor") var kitchenOrderBackgroundColor: String? = "#000000",
    @SerializedName("kitchenItemFontColor") var kitchenItemFontColor: String? = "#FFFFFF",
    @SerializedName("kitchenModifierFontColor") var kitchenModifierFontColor: String? = "#00FF00",
    @SerializedName("kitchenHeaderFontColor") var kitchenHeaderFontColor: String? = "#FFFFFF",
    @SerializedName("kitchenViewBackgroundColor") var kitchenViewBackgroundColor: String? = "#000000",
    @SerializedName("kitchenViewAllDayBackgroundColor") var kitchenViewAllDayBackgroundColor: String? = "#FFFFFF"
)


data class LicenseStatus(
    var key: String? = null,
    var connectionDrops: Int? = 0,
    var connectionDownMinutes: Int? = 0,
    var name: String? = null,
    var deviceId: Int? = 0,
    var lastPulse: String? = null
)

//source: from Dynamo
//https://json2kt.com/
data class MenuItemRecord(
    @SerializedName("count"      ) var count      : Int?          = null,
    @SerializedName("countdown"      ) var countdown      : Boolean           = false,
    @SerializedName("guestCheckName" ) var guestCheckName : String?           = null,
    @SerializedName("imageKey"       ) var imageKey       : String?           = null,
    @SerializedName("lastUpdated"    ) var lastUpdated    : String?           = null,
    @SerializedName("menuItemId"     ) var menuItemId     : String?           = null,
    @SerializedName("menuItemKey"    ) var menuItemKey    : Int?              = null,
    @SerializedName("name"           ) var name           : String?           = null,
    @SerializedName("outOfStock"     ) var outOfStock     : Boolean         = false,
    @SerializedName("pk"             ) var pk             : String?           = null,
    @SerializedName("price"          ) var price          : Double              = 0.00,
    @SerializedName("price2"         ) var price2         : Double              = 0.00,
    @SerializedName("price3"         ) var price3         : Double              = 0.00,
    @SerializedName("price4"         ) var price4         : Double              = 0.00,
    @SerializedName("price5"         ) var price5         : Double              = 0.00,
    @SerializedName("price6"         ) var price6         : Double               = 0.00,
    @SerializedName("priceExtra"     ) var priceExtra     : Double?              = 0.00,
    @SerializedName("priceMod"       ) var priceMod       : Double?              = 0.00,
    @SerializedName("priceNo"        ) var priceNo        : Double?              = 0.00,
    @SerializedName("priceSub"       ) var priceSub       : Double?              = 0.00,
    @SerializedName("sk"             ) var sk             : String?           = null,
    @SerializedName("tagKeys"        ) var tagKeys        : ArrayList<String> = arrayListOf(),
@SerializedName("reportGroupId") var reportGroupId: Int? = null
): Serializable

data class MenuItemSalesRecord(
    @SerializedName("menuItemKey"     ) var menuItemKey     : Int?                  = null,
    @SerializedName("name"            ) var name            : String?               = null,
    @SerializedName("quantity"        ) var quantity        : Int?                  = null,
    @SerializedName("revenue"         ) var revenue         : Int?                  = null,
    @SerializedName("discount"        ) var discount        : String?               = null,
@SerializedName("menuItem"        ) var menuItem        : MenuItemRecord?             = MenuItemRecord(),
    @SerializedName("reportGroupId"   ) var reportGroupId   : Int?                  = null,
    @SerializedName("reportGroupName" ) var reportGroupName : String?               = null,
    @SerializedName("dailySales"      ) var dailySales      : ArrayList<DailySales> = arrayListOf()
)

enum class PosConfigurationChangeType(val changeType: Int){
    OUTOFSTOCK(1),
    COUNTVALUE(2),
    PRICE(3),
    ITEMNAME(4)
}

data class PosConfigurationDataModel(
    var recordId: String? = null,
    var value: String? = null,
    var posConfigurationChangeType: Int? = null
): Serializable

data class PosConfigurationResponseDataModel(
    @SerializedName("pk"                         ) var pk                         : String? = null,
    @SerializedName("sk"                         ) var sk                         : String? = null,
    @SerializedName("configurationKey"           ) var configurationKey           : String? = null,
    @SerializedName("posRecordId"                ) var posRecordId                : String? = null,
    @SerializedName("value"                      ) var value                      : String? = null,
    @SerializedName("posConfigurationChangeType" ) var posConfigurationChangeType : Int?    = null,
    @SerializedName("created"                    ) var created                    : String? = null,
    @SerializedName("updated"                    ) var updated                    : String? = null,
    @SerializedName("status"                     ) var status                     : String? = null,
    @SerializedName("posObject"                  ) var posObject                  : String? = null,
    @SerializedName("fileName"                   ) var fileName                   : String? = null,
    @SerializedName("url"                        ) var url                        : String? = null
): Serializable


data class PosCheck(
    val id: Int?,
    val key: Int?,
    val numberOfSeats: Int = 1,
    val seatCount: Int? = null,
    val total: Double = 0.0,
    val remainingBalance: Double? = null,
    var timeOpened: String? = null,
    val openTime: String? = null,
    val table: String? = null,
    val name: String? = null,
    val ownerName: String,
    val flagsOpen: Boolean = true,
    val open: Boolean = true,
    val seats: ArrayList<PosCheckSeat> = arrayListOf(),
    var guid: String? = null,
    var resync: Boolean = false
) {
    override fun toString(): String {
        return total.toString()
    }
}

data class PosCheckSeat(
    val key: Int,
    val position: Int,
    val seatNumber: Int,
    val flagsUsed: Boolean,
    val flagsOpen: Boolean,
    val open: Boolean,
    val discountTotal: Double,
    val orderTypeChargeTotal: Double,
    val gratuityTotal: Double,
    val gratuityId: Int,
    val subtotal: Double,
    val total: Double,
    val taxtotal: Double,
    val remainingBalance: Double?,
    var items: ArrayList<PosCheckItem>?
)

data class PosCheckItem(
    val key: Int? = null,
    val position: Int? = null,
    val seatKey: Int? = null,
    val seatNumber: Int? = null,
    val recordNumber: Int? = null,
    var name: String? = null,
    var kitchenComment: Boolean? = null,
    var price: Double? = null,
    val priceMod: Double? = null,
    val priceExtra: Double? = null,
    val priceSub: Double? = null,
    val prepModCommand: Int? = null,
    var priceNumber: Int? = null,
    var priceNumberName: String? = null,
    val level: Int? = null,
    val pricePerUnit: Double? = null,
    val quantity: Double? = null,
    val discounted: Boolean = false,
    val void: Boolean = false,
    var sub: Boolean = false,
    var extra: Boolean = false,
    var no: Boolean = false,
    var courseId: Int? = null,
    var positionId: Int? = null,
    var ignore: Boolean? = false,
    var type: String? = null,
    var parentPosition: Int? = null,
    var modUnit: Int? = null,
    var canvasIdx: Int? = null,
    var _position: Int? = null
)

data class PosStartCheckNotify(
    val checkId: Int,
    val checkKey: String? = null
)



data class ReportGroupRecord(
    @SerializedName("venueKey") var venueKey : String,
    @SerializedName("ID") var ID : String,
    @SerializedName("Name") var name: String,
    @SerializedName("SuperGroupID") var superGroupId: Int,
    @SerializedName("NonSales") var nonSales: Boolean,
    @SerializedName("NS") var NS: Boolean,
    @SerializedName("SuperGroup") var superGroup: SuperGroupRecord
)

data class SuperGroupRecord(
    @SerializedName("venueKey") var venueKey: Int,
    @SerializedName("ID") var ID: Int,
    @SerializedName("Name") var name: String
)

data class SpinnerItem(
    val label: String,
    val value: Any
) {
    override fun toString(): String = label
}

data class Tab(
    val id: String,
    val name: String? = null,
    val tabName: String? = null,
    val ownerName: String? = null,
    var amount: Double? = null,
    val openTime: String? = null
)

data class Venue(
    val name: String,
    val mode: String
)

data class VenuePreference(
    val tip1: Int,
    val tip2: Int,
    val tip3: Int,
    val tipDollarThreshold: Double?,
    val tipDollar1: Double?,
    val tipDollar2: Double?,
    val tipDollar3: Double?,
    val showSurvey: Boolean,
    val canvasSort: String,
    val checkListSyncInterval: Int,
    val accessCodeLength: Int,
    val loyaltyTabQrUrl: String,
    val promptTip: Boolean,
    val displayTipSuggestions: Boolean,
    val printCreditVoucher: Boolean,
    val printedReceiptFooter: String,
    val printedReceiptLogoS3key: String? = null
)



enum class CheckPaymentTypeEnum(val value: Int) {
    CREDITCARD(1),
    CASH(2),
    GIFTCARD(3),
    LOYALTY(4),
    INTEGRATOR(5);

    companion object {
        fun fromInt(value: Int) = CheckPaymentTypeEnum.values().first { it.value == value }
    }
}