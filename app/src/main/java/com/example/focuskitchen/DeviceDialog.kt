package com.example.focuskitchen

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment

class DeviceDialog: AppCompatDialogFragment() {
    private lateinit var dialog: AlertDialog
    private var dialogView: View? = null
    private lateinit var deviceNameText: TextView
    private lateinit var printerNumText: TextView
    private lateinit var ipAddrText: TextView
    private lateinit var guidText: TextView
    private lateinit var venueKeyText: TextView
    private lateinit var versionText: TextView
    lateinit var credentials: DeviceCredentials
    lateinit var orderAdapter: OrderAdapter

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)

        val inflater = activity?.layoutInflater

        dialogView = inflater?.inflate(R.layout.info_dialog, null)
        deviceNameText = dialogView!!.findViewById(R.id.dev_name_text)
        printerNumText = dialogView!!.findViewById(R.id.print_num_text)
        ipAddrText = dialogView!!.findViewById(R.id.ip_addr_text)
        guidText = dialogView!!.findViewById(R.id.guid_text)
        venueKeyText = dialogView!!.findViewById(R.id.store_key_text)
        versionText = dialogView!!.findViewById(R.id.version_text)
        setInfo()

        builder.setView(dialogView).setTitle("Device Details").setPositiveButton("Close", null).setNegativeButton("Clear Orders") {
                dialogInterface: DialogInterface, i: Int ->

            AlertDialog.Builder(context).setTitle("Clear all orders")
                .setMessage("Are you sure you want to clear ALL orders?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setNegativeButton("No", null)
                .setPositiveButton("Yes") {
                    dialogInterface: DialogInterface, i: Int ->
                OrdersModel.orders.clear()
                OrdersModel.bumpedOrders.clear()
                orderAdapter.dataSet.clear()
                    OrdersModel.bumpedOrders.add(mutableListOf())
                val editor = credentials.prefs.edit()
                editor.putString(credentials.PREFS_ORDERS_KEY, null)
                editor.apply()
                orderAdapter.notifyDataSetChanged()
            }.show().getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
        }
        dialog = builder.create()

        return dialog
    }

    private fun setInfo() {
        deviceNameText.text = "Device: ${credentials.deviceName}"
        printerNumText.text = "Printer #: ${credentials.printerNum}"
        versionText.text = "Build: ${BuildConfig.VERSION_NAME} - ${BuildConfig.VERSION_CODE}"
        venueKeyText.text = "Store Key: ${credentials.venueKey}"
        ipAddrText.text = "IP Address: ${credentials.ipAddress}"
        guidText.text = "Unique ID: ${credentials.macAddress}"
    }
}