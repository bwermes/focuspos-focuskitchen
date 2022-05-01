package com.amorphik.focuskitchen.itemControl

import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.media.Image
import android.os.Bundle
import android.os.PersistableBundle
import android.text.Editable
import android.text.Layout
import android.text.TextWatcher
import android.transition.TransitionManager
import android.util.Log
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Guideline
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.amorphik.focuskitchen.*
import com.amorphik.focuskitchen.FocusKitchen.Companion.session
import com.amorphik.focuskitchen.Utility.hideKeyboard
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.charts.Chart
import com.google.gson.internal.bind.ReflectiveTypeAdapterFactory
import kotlinx.android.synthetic.main.activity_item_control.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.header_cell.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter


class ActivityItemView: AppCompatActivity() {
    var reportGroupFilterValue: Int = 0
    lateinit var progressBar: ProgressBar
    var isEditing: Boolean = false
    var isAnimating: Boolean = false
    var hasAnimated: Boolean = false
    lateinit var res: Resources
    lateinit var menuItemListAdapter: AdapterItemControlItemList
    lateinit var selectedItemSalesRecord: MenuItemSalesRecord
    lateinit var hourlySalesChartDataSet: LineDataSet
    lateinit var dailySalesChartDataSet: LineDataSet
    var chartTypeMode: ChartType = ChartType.DAY
    private lateinit var lineChart: LineChart


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("itemControl","onCreate-init")
        super.onCreate(savedInstanceState)
        res = resources
        setContentView(R.layout.activity_item_control)
        Log.d("itemControl","onCreate-setContent")
        progressBar = this.findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE


        getMenuItems()
        getReportGroup()

        item_control_button_back.setOnClickListener {
            finish()
        }

        item_control_menu_item_search_field.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if(session != null && session!!.menuItemRecords != null){
                    buildMenuItems(session!!.menuItemRecords!!, s)
                }

            }
        })



    }

    private fun getReportGroup(){
        CoroutineScope(Dispatchers.IO).launch{
            withContext(Dispatchers.Main){
                var reportGroupList: Array<ReportGroupRecord> = api!!.getReportGroups(prefs.venueKey)
                buildSpinner(reportGroupList)
            }
        }
    }

    private fun getMenuItems(){
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                val menuItemReturn = api!!.getMenu(prefs.venueKey,"dyn")
                session!!.menuItemRecords = menuItemReturn.sortedBy { it.name }
                buildMenuItems(session!!.menuItemRecords!!, null);

            }
        }
    }

    private fun buildMenuItems(menuItemList: List<MenuItemRecord>, filterMenuItemName: CharSequence?){
        val listViewMenuItem = this.findViewById<RecyclerView>(R.id.item_control_recycler_item_list)

        var filteredItems: List<MenuItemRecord> = menuItemList.toList()
        if( reportGroupFilterValue > 0){
            filteredItems = filteredItems.filter { it.reportGroupId == reportGroupFilterValue}

        }
        if(filterMenuItemName != null){
            filteredItems = filteredItems.filter { it.name!!.contains(filterMenuItemName,true)  || it.guestCheckName!!.contains(filterMenuItemName,true)  }

        }

        if(filteredItems.size == 0){
            this.findViewById<TextView>(R.id.item_control_header_no_items_found).visibility = View.VISIBLE
        } else{
            this.findViewById<TextView>(R.id.item_control_header_no_items_found).visibility = View.GONE
        }

        item_control_recycler_item_list.layoutManager = GridLayoutManager(this@ActivityItemView, 1)
        item_control_recycler_item_list.adapter =  AdapterItemControlItemList(filteredItems) {
            Log.d("menuItemEdit","on click")
            this.hideKeyboard()
            resetChart()

            if(isEditing){
                setItemEditUi(it)
            } else{
                slideItemEditUi(true)
                setItemEditUi(it)
                isEditing = true
            }
            menuItemGetSales(it.menuItemKey!!)
        }

        menuItemListAdapter = item_control_recycler_item_list.adapter as AdapterItemControlItemList

        progressBar.visibility = View.GONE
    }

    private fun menuItemGetSales(menuItemKey: Int){
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                try{
                    val itemSalesList = api!!.itemSales(prefs.venueKey, menuItemKey, 7)
                    if(itemSalesList.size > 0){
                        selectedItemSalesRecord = itemSalesList[0]
                        chartTypeMode = ChartType.DAY
                        chartButtonsEnable()
                        salesDataBuild(ChartType.DAY)
                    }

                }catch(error: Exception){
                    Logger.e("itemSales",error.message!!,false)
                }
            }
        }
    }

    private fun chartButtonsEnable(){
        val chartButtonContainer = this.findViewById<ConstraintLayout>(R.id.chart_button_holder)
        chartButtonContainer.visibility = View.VISIBLE
        val chartButtonSalesByDay = this.findViewById<Button>(R.id.chart_button_salesByDay)
        val chartButtonSalesByHour = this.findViewById<Button>(R.id.chart_button_salesByHour)

        when(chartTypeMode){
            ChartType.DAY -> {
                chartButtonSalesByDay.background =
                    res.getDrawable(R.drawable.ripple_effect_blue_to_grey)
                chartButtonSalesByHour.background =
                    res.getDrawable(R.drawable.ripple_effect_grey_to_blue)
            }
            ChartType.HOUR -> {
                chartButtonSalesByHour.background=
                res.getDrawable(R.drawable.ripple_effect_blue_to_grey)
                chartButtonSalesByDay.background =
                    res.getDrawable(R.drawable.ripple_effect_grey_to_blue)
            }
        }

        chartButtonSalesByDay.setOnClickListener { i ->
            chartButtonSalesByDay.background = res.getDrawable(R.drawable.ripple_effect_blue_to_grey)
            chartButtonSalesByHour.background = res.getDrawable(R.drawable.ripple_effect_grey_to_blue)
            chartSetByButton(ChartType.DAY)
        }

        chartButtonSalesByHour.setOnClickListener { i ->
            chartButtonSalesByHour.background = res.getDrawable(R.drawable.ripple_effect_blue_to_grey)
            chartButtonSalesByDay.background = res.getDrawable(R.drawable.ripple_effect_grey_to_blue)
            chartSetByButton(ChartType.HOUR)
        }
    }

    private fun chartSetByButton(type: ChartType){
        when(type){
            ChartType.HOUR -> {
                chartTypeMode = ChartType.HOUR
                lineChart.data = LineData(hourlySalesChartDataSet)
                lineChart.description.text  = "Sales for today by hour"
            }
            ChartType.DAY -> {
                chartTypeMode = ChartType.DAY
                lineChart.data = LineData(dailySalesChartDataSet)
                lineChart.description.text  = "Sales for the last 7 days"
            }
        }

        lineChart.invalidate()
    }

    private fun setDataSet(type: ChartType, data: ArrayList<Entry>){
        val lineDataSet = LineDataSet(data,selectedItemSalesRecord.name)

        lineDataSet.setColor(Color.WHITE)
        lineDataSet.lineWidth = 5f
        lineDataSet.setDrawFilled(true)

        when (type){
            ChartType.HOUR -> hourlySalesChartDataSet = lineDataSet
            ChartType.DAY -> dailySalesChartDataSet = lineDataSet
        }

    }

    private fun salesDataBuild(type: ChartType){
        Logger.d("itemChart","init build")
        lineChart = findViewById<View>(R.id.item_sales_chart) as LineChart


        val dailyValues: ArrayList<Entry> = arrayListOf<Entry>()
        val hourlyValues: ArrayList<Entry> = arrayListOf<Entry>()

        selectedItemSalesRecord.dailySales.forEachIndexed { index, day ->
            dailyValues.add(Entry(index.toFloat(),day.quantity!!.toFloat()))
            if(index == selectedItemSalesRecord.dailySales.size - 1){
                day.hourlySales.forEachIndexed { index2, hourlySales ->
                    hourlyValues.add(Entry(index2.toFloat(),hourlySales.quantity!!.toFloat()))
                }
            }
        }

        setDataSet(ChartType.HOUR, hourlyValues)
        setDataSet(ChartType.DAY, dailyValues)

        lineChart.animateX(1000, Easing.EasingOption.EaseInSine)
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.labelRotationAngle = 45f
        xAxis.valueFormatter = MyAxisFormatter()
        xAxis.textColor = Color.WHITE
        xAxis.textSize = 14f
        xAxis.setDrawGridLines(false)

        when(type){
            ChartType.HOUR -> {
                lineChart.data = LineData(hourlySalesChartDataSet)
                lineChart.description.text  = "Sales for today by hour"
            }
            ChartType.DAY -> {
                lineChart.data = LineData(dailySalesChartDataSet)
                lineChart.description.text  = "Sales for the last 7 days"
            }
        }

        lineChart.description.textSize = 10f
        lineChart.description.textColor = Color.WHITE
        lineChart.setPinchZoom(true)
        lineChart.invalidate()
    }

    private fun resetChart() {
        if(this::lineChart.isInitialized && lineChart != null){
            lineChart.fitScreen()
            lineChart.data?.clearValues()
            lineChart.xAxis.valueFormatter = null
            lineChart.notifyDataSetChanged()
            lineChart.clear()
            lineChart.invalidate()
        }

    }

    inner class MyAxisFormatter : IndexAxisValueFormatter() {

        override fun getFormattedValue(value: Float, axis: AxisBase?): String {
            val index = value.toInt()
            Logger.d("itemChart","axis formatter $value")
            when(chartTypeMode){
                ChartType.DAY ->{
                    return if (index < selectedItemSalesRecord.dailySales.size -1) {
                        selectedItemSalesRecord.dailySales[index].dayOfWeek!!
                    } else if(index == selectedItemSalesRecord.dailySales.size - 1) {
                        "Today"
                    }
                    else {
                        ""
                    }
                }
                ChartType.HOUR ->{
                    val currentDay = selectedItemSalesRecord.dailySales[selectedItemSalesRecord.dailySales.size - 1]
                    return if (index < currentDay.hourlySales.size) {
                        currentDay.hourlySales[index].hourBlockName!!
                    } else {
                        ""
                    }
                }
            }

        }
    }

    private fun setItemEditUi(menuItem: MenuItemRecord){

        val itemNameText = findViewById<TextView>(R.id.item_edit_text_item_name)
        itemNameText!!.text = menuItem.name

        val itemEditUiButton = findViewById<ImageView>(R.id.item_edit_close_image)
        val stockToggleButton = findViewById<ImageView>(R.id.item_edit_stock_toggle_button)

        itemEditUiButton.setOnClickListener { clearItemEditUi() }

        setStockStatus(menuItem)
        stockToggleButton?.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            toggleItemStockStatus(menuItem)
            menuItemListAdapter.notifyDataSetChanged()
            progressBar.visibility = View.GONE
        }

    }



    private fun clearItemEditUi(){
        slideItemEditUi(false)

        isEditing = false
    }

    private fun setStockStatus(menuItem: MenuItemRecord){
        val currentStatus = findViewById<TextView>(R.id.item_count_dialog_item_current_status)
        val stockToggleButton = findViewById<ImageView>(R.id.item_edit_stock_toggle_button)
        val itemCurrentOnHandText = findViewById<TextView>(R.id.item_edit_dialog_text_item_current_on_hand)
        if(menuItem.outOfStock){
            currentStatus?.text = res.getString(R.string.item_edit_dialog_currently_out_of_stock)
            stockToggleButton?.setImageResource(R.drawable.out_of_stock_icon)

        } else if(menuItem.count != null && menuItem.count!! > 0){
            currentStatus?.text = menuItem.count.toString()
            stockToggleButton?.setImageResource(R.drawable.in_stock_icon)

        } else{

            itemCurrentOnHandText?.visibility = View.GONE
            currentStatus?.text = res.getString(R.string.item_edit_dialog_currently_in_stock)
            currentStatus?.setTypeface(currentStatus.typeface, Typeface.ITALIC )
            stockToggleButton?.setImageResource(R.drawable.in_stock_icon)
        }

        menuItemListAdapter.notifyDataSetChanged()
    }

    private fun slideItemEditUi(showItemEdit: Boolean){

        val itemEditView = this.findViewById<View>(R.id.item_control_item_edit_view)
        itemEditView.visibility = View.VISIBLE

        val constraintLayout = this.findViewById<ConstraintLayout>(R.id.activity_item_control_parent_layout)
        val constraintSet2 = ConstraintSet()
        constraintSet2.clone(constraintLayout)
        if(showItemEdit){
            constraintSet2.connect(R.id.item_control_recycler_with_header,ConstraintSet.END,R.id.item_control_guide_v7,ConstraintSet.END)

        } else{
            Log.d("itemEdit","this fired")
            constraintSet2.connect(R.id.item_control_recycler_with_header,ConstraintSet.END,R.id.item_control_guide_v100,ConstraintSet.END)
        }

        var changed = false
        TransitionManager.beginDelayedTransition(constraintLayout)
        constraintSet2.applyTo(constraintLayout)
        changed = !changed
    }

    private fun toggleItemStockStatus(menuItem: MenuItemRecord){
        Logger.d("itemControl","changing ${menuItem.name} OOS = ${menuItem.outOfStock}")
        Utility.notification("${menuItem.name} is now ${if(menuItem.outOfStock) "out of stock" else "in stock"}")

        menuItem.outOfStock = !menuItem.outOfStock
        Logger.d("itemControl","changing ${menuItem.name} OOS = ${menuItem.outOfStock}")
        Utility.submitPosPropertyChange(PosConfigurationChangeType.OUTOFSTOCK,menuItem.menuItemId!!, if(menuItem.outOfStock) false else true)
        setStockStatus(menuItem)
        val sessionItem = session?.menuItemRecords?.find { i -> i.menuItemKey == menuItem.menuItemKey }
        sessionItem?.outOfStock   = menuItem.outOfStock
        Logger.d("itemControl","changing ${sessionItem!!.name} OOS = ${sessionItem!!.outOfStock}")
        menuItemListAdapter.notifyDataSetChanged()
    }

    private fun buildSpinner(reportGroupList: Array<ReportGroupRecord>){
        val reportGroupValues = arrayListOf<SpinnerItem>()
        reportGroupValues.add(
            SpinnerItem(
                label = "All",
                value = 0
            )
        )
        for(rg in reportGroupList){
            reportGroupValues.add(
                SpinnerItem(
                    label = rg.name,
                    value = rg.ID
                ))
        }

        val spinnerReportGroup = this.findViewById<Spinner>(R.id.item_control_item_search_search_spinner_report_group)
        val reportGroupSpinnerAdapter = ArrayAdapter(this@ActivityItemView, android.R.layout.simple_spinner_item, reportGroupValues)
        reportGroupSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line)
        spinnerReportGroup.adapter = reportGroupSpinnerAdapter;
        spinnerReportGroup.avoidDropdownFocus()


        spinnerReportGroup.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                Log.d("menuItemSearch","Item selected ${reportGroupList[p2].name}")

                if(p2 > 0){
                    reportGroupFilterValue = reportGroupList[p2].ID.toInt() - 1 //subtract 1 to the position index to account for the "all" element
                } else{
                    reportGroupFilterValue = 0
                }

                if(session != null && session!!.menuItemRecords != null){
                    buildMenuItems(session!!.menuItemRecords!!, null)
                    Log.d("menuItemSearch","Items refreshed")
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                TODO("Not yet implemented")
            }

        }
    }

    private fun Spinner.avoidDropdownFocus() {
        try {
            val isAppCompat = this is androidx.appcompat.widget.AppCompatSpinner
            val spinnerClass = if (isAppCompat) androidx.appcompat.widget.AppCompatSpinner::class.java else Spinner::class.java
            val popupWindowClass = if (isAppCompat) androidx.appcompat.widget.ListPopupWindow::class.java else android.widget.ListPopupWindow::class.java

            val listPopup = spinnerClass
                .getDeclaredField("mPopup")
                .apply { isAccessible = true }
                .get(this)
            if (popupWindowClass.isInstance(listPopup)) {
                val popup = popupWindowClass
                    .getDeclaredField("mPopup")
                    .apply { isAccessible = true }
                    .get(listPopup)
                if (popup is PopupWindow) {
                    popup.isFocusable = false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    enum class ChartType(){
        HOUR,DAY
    }
}

