package com.amorphik.focuskitchen

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import com.example.focuskitchen.OrderAdapter
import okhttp3.WebSocket

// Dialog popup to show device details as well as allowing resetting license and clearing orders
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
    lateinit var supportFragmentManager: FragmentManager
    lateinit var websocket: WebSocket

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

        // Sets buttons for the dialog
        // Positive button = close
        // Neutral button = clear license
        // Negative button = clear orders
        builder.setView(dialogView).setTitle("Device Details").setPositiveButton("Close", null).setNeutralButton("Clear License") {
                dialogInterface: DialogInterface, i: Int ->
            println("Attempting to clear license")

            // Make a network request to reset the license
            val headerName = "Authorization"
            val integratorValue = credentials.generateIntegratorHeader()
            val licensesURL = "${credentials.baseApiUrl}licenses/reset"
            val payload = """{
                            "key": "${credentials.licenseKey}"
                        }"""
            Networking.postData(licensesURL, headerName, integratorValue, payload) {
                call, response, responseBody ->

                // Clearing license on back end successful
                if (response != null && response.isSuccessful) {
                    // Clear all the orders and license info
                    clearAllOrders()
                    clearLicenseInfo()
                    websocket.close(1001, "Cleared License")

                    // Perform on callback that license has been cleared
                    openDialog(credentials, "UNREGISTERED")
                }
            }


        }.setNegativeButton("Clear Orders") {
                dialogInterface: DialogInterface, i: Int ->

            AlertDialog.Builder(context).setTitle("Clear all orders")
                .setMessage("Are you sure you want to clear ALL orders?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setNegativeButton("No", null)
                .setPositiveButton("Yes") {
                    dialogInterface: DialogInterface, i: Int ->
                clearAllOrders()
            }.show().getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
        }

        dialog = builder.create()

        return dialog
    }

    private fun clearLicenseInfo() {
        // Null out the saved license details
        val editor = credentials.prefs.edit()
        editor.putString(credentials.PREFS_LICENSE_KEY, null)
        editor.putString(credentials.PREFS_VENUE_KEY, null)
        editor.putString(credentials.PREFS_MAC_ADDR, null)
        editor.putString(credentials.PREFS_IP_ADDR, null)
        editor.putString(credentials.PREFS_DEVICE_NAME, null)
        editor.putString(credentials.PREFS_LICENSE_SECRET, null)
        editor.apply()

        // Clear the properties in memory
        credentials.venueKey = ""
        credentials.deviceName = ""
        credentials.licenseKey = ""
        credentials.licenseSecret = ""
        credentials.macAddress = credentials.generateFauxMac()
        credentials.mode = "prod"
    }

    // Purges orders
    private fun clearAllOrders() {
        // Empty arrays
        OrdersModel.orders.clear()
        OrdersModel.bumpedOrders.clear()
        orderAdapter.dataSet.clear()
        OrdersModel.bumpedOrders.add(mutableListOf())

        // Clear any saved orders
        val editor = credentials.prefs.edit()
        editor.putString(credentials.PREFS_ORDERS_KEY, null)
        editor.apply()

        // Update displayed orders
        activity?.runOnUiThread {
            orderAdapter.notifyDataSetChanged()
        }
    }

    // Used to open venue dialog to claim license after reset
    private fun openDialog(credentials: DeviceCredentials, status: String) {
        val registrationDialog = VenueDialog()
        registrationDialog.status = status
        registrationDialog.credentials = credentials
        registrationDialog.isCancelable = false
        registrationDialog.show(supportFragmentManager, "venueDialog")
    }

    // Set text labels to display device info
    private fun setInfo() {
        deviceNameText.text = "Device: ${credentials.deviceName}"
        printerNumText.text = "Printer #: ${credentials.printerNum}"
        versionText.text = "Build: ${BuildConfig.VERSION_NAME} - ${BuildConfig.VERSION_CODE}"
        venueKeyText.text = "Store Key: ${credentials.venueKey}"
        ipAddrText.text = "IP Address: ${credentials.ipAddress}"
        guidText.text = "Unique ID: ${credentials.macAddress}"
    }
}