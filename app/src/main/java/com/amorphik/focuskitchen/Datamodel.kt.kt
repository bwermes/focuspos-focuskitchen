package com.amorphik.focuskitchen

import com.google.gson.annotations.SerializedName
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

data class CheckSurvey (
    val checkKey: String,
    val seatKey: Int,
    val happy: Boolean? = null
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

data class License(
    var key: String? = null,
    var secret: String? = null,
    var venueKey: Int? = 0,
    var venueName: String? = null,
    var name: String? = null,
    var dealerKey: String? = null,
    var dealerName: String? = null,
    var features: LicenseFeatures? = null,
    var status: String? = null,
    var latestVersion: Double? = 0.0,
    var alertMessage: String? = null,
    var posStationId: Int? = null
)

data class LicenseFeatures(
    val pay: Boolean,
    val order: Boolean,
    val loyaltyTab: Boolean,
    val survey: Boolean,
    val loyalty: Boolean,
    val onServerPay: Boolean,
    val nfcPay: Boolean,
    val giftCardPay: Boolean,
    val cashPay: Boolean,
    val qrPay: Boolean,
    val customer: Boolean,
    val sms: Boolean,
    val promptTableNumber: Boolean,
    val promptPosition: Boolean,
    val promptTip: Boolean
)

data class LicenseStatus(
    var key: String? = null,
    var connectionDrops: Int? = 0,
    var connectionDownMinutes: Int? = 0,
    var name: String? = null,
    var deviceId: Int? = 0,
    var lastPulse: String? = null
)

data class Menu(
    val modifierSub: String,
    val modifierExtra: String,
    val modifierNo: String,
    val prepModifier1: String,
    val prepModifier2: String,
    val prepModifier3: String,
    val prepModifier4: String,
    val prepModifier5: String,
    val prepModifier6: String,
    val prepModifier7: String,
    val prepModifier8: String,
    val prepModifier9: String,
    val prepModifier10: String,
    val course1: String?,
    val course2: String?,
    val course3: String?,
    val course4: String?,
    val course5: String?,
    val course6: String?,
    val course7: String?,
    val course8: String?,
    val course9: String?,
    val course10: String?,
    val creditCardPassAlongFeePercentage: Double?,
    val creditCardPassAlongFeeName: String?,
    val timeRatesSpanTimeRanges: Boolean?,
    val alphanumericTableNumbers: Boolean?,
    val canvases: ArrayList<MenuCanvas>,
    val menuItems: ArrayList<MenuMenuItem>,
    val priceLevels: ArrayList<MenuPriceLevel>,
    val stations: ArrayList<MenuStation>,
    val timeRanges: ArrayList<MenuTimeRange>
)

data class MenuCanvas (
    val id: String,
    val name: String?,
    val prepModifier1: String?,
    val prepModifier2: String?,
    val prepModifier3: String?,
    val menuItems: ArrayList<MenuMenuItem>
)

data class MenuMenuItem(
    val id: Int,
    var name: String?,
    var price: Double?,
    var price2: Double?,
    var price3: Double?,
    var price4: Double?,
    var price5: Double?,
    var price6: Double?,
    var priceMod: Double?,
    var priceDoublePercent: Double?,
    var modUnit: Int?,
    var priceExtra: Double?,
    var priceSub: Double?,
    var priceNo: Double?,
    var kitchenComment: Boolean?,
    var variablePrice: Boolean?,
    var outOfStock: Boolean?,
    var courseNumber: Int?,
    var positionPrompt: Boolean?,
    var level: Int?,
    var priceLevel: Int?,
    var effectiveTimeRange: Int?,
    var priceTime1: Int?,
    var priceTime2: Int?,
    var priceTime3: Int?,
    var priceTime4: Int?,
    var priceTime5: Int?,
    var priceTime6: Int?,
    var dependentItemKey: Int?,
    var suggestionItemKey: Int?,
    var suggestionText: String?,
    var modCanvases: ArrayList<MenuMenuItemModCanvas>
)

data class MenuItemRecord (

    @SerializedName("ID") var ID : String,
    @SerializedName("INVENTORYID") var INVENTORYID : String,
    @SerializedName("GuestCheckName") var GuestCheckName : String,
    @SerializedName("SortKey") var SortKey : String,
    @SerializedName("MenuName") var MenuName : String,
    @SerializedName("RemoteName") var RemoteName : String,
    @SerializedName("AudioFile") var AudioFile : String? = null,
    @SerializedName("SuggestionText") var SuggestionText : String? = null,
    @SerializedName("venueKey") var venueKey : Int,
    @SerializedName("RecordNumber") var RecordNumber : Int,
    @SerializedName("SuggestionID") var SuggestionID : Int? = null,
    @SerializedName("DependentItem") var DependentItem : Int? = null,
    @SerializedName("ReportGroup") var ReportGroup : Int,
    @SerializedName("Type") var Type : Int,
    @SerializedName("PrinterGroupID") var PrinterGroupID : Int,
    @SerializedName("CourseID") var CourseID : Int,
    @SerializedName("BeverageSatisfiesCount") var BeverageSatisfiesCount : Int,
    @SerializedName("Concept") var Concept : Int,
    @SerializedName("MealStageID") var MealStageID : Int,
    @SerializedName("DblPercent") var DblPercent : String,
    @SerializedName("PriceLevelID") var PriceLevelID : Int,
    @SerializedName("Priority") var Priority : Int,
    @SerializedName("Count") var Count : Int,
    @SerializedName("ConversationalMods") var ConversationalMods : Boolean,
    @SerializedName("IncludeInItemPrice") var IncludeInItemPrice : Boolean,
    @SerializedName("Deleted") var Deleted : Boolean,
    @SerializedName("SatisfiesBeverage") var SatisfiesBeverage : Boolean,
    @SerializedName("RequiresBeverage") var RequiresBeverage : Boolean,
    @SerializedName("GuestCheck") var GuestCheck : Boolean,
    @SerializedName("VariablePrice") var VariablePrice : Boolean,
    @SerializedName("KitchenComment") var KitchenComment : Boolean,
    @SerializedName("FollowItem") var FollowItem : Boolean,
    @SerializedName("Countdown") var Countdown : Boolean,
    @SerializedName("OutOfStock") var OutOfStock : Boolean,
    @SerializedName("RepeatRound") var RepeatRound : Boolean,
    @SerializedName("KitchenPrice") var KitchenPrice : Boolean,
    @SerializedName("Scale") var Scale : Boolean,
    @SerializedName("RequestTareWeight") var RequestTareWeight : Boolean,
    @SerializedName("IncrementGuests") var IncrementGuests : Boolean,
    @SerializedName("FractionQuantity") var FractionQuantity : Boolean,
    @SerializedName("Inventoried") var Inventoried : Boolean,
    @SerializedName("FoodStampEligible") var FoodStampEligible : Boolean,
    @SerializedName("TimedRate") var TimedRate : Boolean,
    @SerializedName("SteeringModifier") var SteeringModifier : Boolean,
    @SerializedName("ReclassifyTax") var ReclassifyTax : Boolean,
    @SerializedName("QuantityPrompt") var QuantityPrompt : Boolean,
    @SerializedName("PositionPrompt") var PositionPrompt : Boolean,
    @SerializedName("RequireApproval") var RequireApproval : Boolean,
    @SerializedName("MenuItemPriceList") var MenuItemPriceList : MenuItemPriceList,
    @SerializedName("MenuItemTaxes") var MenuItemTaxes : List<MenuItemTaxes>

)

data class MenuItemPriceList (

    @SerializedName("venueKey") var venueKey : Int,
    @SerializedName("MenuItemRecordID") var MenuItemRecordID : Int,
    @SerializedName("Price1") var Price1 : Double,
    @SerializedName("Price2") var Price2 : Double,
    @SerializedName("Price3") var Price3 : Double,
    @SerializedName("Price4") var Price4 : Double,
    @SerializedName("Price5") var Price5 : Double,
    @SerializedName("Price6") var Price6 : Double,
    @SerializedName("Price7") var Price7 : Double,
    @SerializedName("Price8") var Price8 : Double,
    @SerializedName("Price9") var Price9 : Double,
    @SerializedName("Price10") var Price10 : Double,
    @SerializedName("PriceTimeId1") var PriceTimeId1 : Int,
    @SerializedName("PriceTimeId2") var PriceTimeId2 : Int,
    @SerializedName("PriceTimeId3") var PriceTimeId3 : Int,
    @SerializedName("PriceTimeId4") var PriceTimeId4 : Int,
    @SerializedName("PriceTimeId5") var PriceTimeId5 : Int,
    @SerializedName("PriceTimeId6") var PriceTimeId6 : Int,
    @SerializedName("Updated") var Updated : String

)

data class MenuItemTaxes (

    @SerializedName("venueKey") var venueKey : Int,
    @SerializedName("TaxID") var TaxID : Int,
    @SerializedName("MenuItemRecordId") var MenuItemRecordId : Int,
    @SerializedName("Enabled") var Enabled : Boolean

)

data class MenuMenuItemModCanvas(
    val id: String,
    val canvasId: String,
    val min: String,
    val max: String,
    var free: Int?,
    var name: String?,
    var prepModifier1: String?,
    var prepModifier2: String?,
    var prepModifier3: String?,
    var menuItems: ArrayList<MenuMenuItem>?,
    var idx: Int?
)

data class MenuPriceLevel (
    val id: Int,
    val name: String,
    val price_name1: String?,
    val price_name2: String?,
    val price_name3: String?,
    val price_name4: String?,
    val price_name5: String?,
    val price_name6: String?
)

data class MenuStation (
    val id: Int,
    val name: String
)

data class MenuTimeRange (
    val id: Int,
    val name: String,
    val day1Start1: String?,
    val day1End1: String?,
    val day1Start2: String?,
    val day1End2: String?,
    val day2Start1: String?,
    val day2End1: String?,
    val day2Start2: String?,
    val day2End2: String?,
    val day3Start1: String?,
    val day3End1: String?,
    val day3Start2: String?,
    val day3End2: String?,
    val day4Start1: String?,
    val day4End1: String?,
    val day4Start2: String?,
    val day4End2: String?,
    val day5Start1: String?,
    val day5End1: String?,
    val day5Start2: String?,
    val day5End2: String?,
    val day6Start1: String?,
    val day6End1: String?,
    val day6Start2: String?,
    val day6End2: String?,
    val day7Start1: String?,
    val day7End1: String?,
    val day7Start2: String?,
    val day7End2: String?
)

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