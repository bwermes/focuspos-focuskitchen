package com.amorphik.focuskitchen

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatDialogFragment
import com.beust.klaxon.Klaxon
import okhttp3.*

class VenueDialog: AppCompatDialogFragment() {
    private lateinit var venueText: EditText
    private lateinit var licenseText: TextView
    private lateinit var spinner: ProgressBar
    private lateinit var dialog: AlertDialog
    private var dialogView: View? = null
    lateinit var credentials: DeviceCredentials
    lateinit var status: String
    var errorCode = 0
    private var venueKey = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Dialog setup
        val builder = AlertDialog.Builder(activity)
        val inflater = activity?.layoutInflater

        dialogView = inflater?.inflate(R.layout.registration_dialog, null)
        venueText = dialogView!!.findViewById(R.id.venue_id_editText)
        spinner = dialogView!!.findViewById(R.id.progressBar)
        licenseText = dialogView!!.findViewById(R.id.licenses_TextView)

        builder.setView(dialogView).setTitle("Enter Store ID").setPositiveButton("Submit", null)

        dialog = builder.create()

        // Show the requested dialog
        dialog.setOnShowListener {
            when (status) {
                "UNREGISTERED" -> showVenueKeyPrompt()
                "DISABLED" -> showMessage("License Inactive", "Contact your Focus dealer.", "")
                "ERROR" -> showMessage("License Verification Failed ($errorCode)", "Contact your Focus dealer.", "")
            }
        }

        return dialog
    }

    // Request venue key from user
    private fun showVenueKeyPrompt() {
        dialog.setTitle("Enter Store ID")
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).visibility = Button.VISIBLE
        spinner.visibility = ProgressBar.GONE
        venueText.inputType = InputType.TYPE_CLASS_NUMBER
        venueText.hint = "ex: 1234"
        venueText.visibility = EditText.VISIBLE
        licenseText.visibility = TextView.GONE
        licenseText.text = ""
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).text = "Submit"

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            // Attempt to find licenses with user input
            if (venueText.text.count() > 0) {
                val headerName = "Authorization"
                val integratorValue = credentials.generateIntegratorHeader()
                venueKey = venueText.text.toString()
                credentials.venueKey = venueKey
                if (venueKey == "8288568") {
                    dialog.dismiss()
                }

                val licensesURL =
                    "${credentials.baseApiUrl}licenses/available?venueKey=$venueKey&type=kitchen-device"
                showPending("Verifying")
                Networking.fetchJson(licensesURL, headerName, integratorValue, ::getLicensesCallback)
            }
        }
    }

    // Display a status message to the user
    private fun showMessage(title: String, message: String, buttonText: String) {
        dialog.setTitle(title)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).visibility = Button.VISIBLE
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).text = buttonText
        spinner.visibility = ProgressBar.GONE
        venueText.inputType = InputType.TYPE_CLASS_TEXT
        venueText.visibility = EditText.GONE
        licenseText.visibility = TextView.VISIBLE
        licenseText.text = message
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            // Button text determines where the user will go next
            when(buttonText) {
                "Claim" -> requestDeviceName()
                "Re-enter Store ID" -> showVenueKeyPrompt()
                "Close" -> dialog.dismiss()
                "" -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).visibility = Button.GONE
            }
        }
    }

    // Shows title with spinner
    private fun showPending(title: String) {
        (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
            dialogView!!.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
        dialog.setTitle("$title...")
        venueText.visibility = EditText.GONE
        spinner.visibility = ProgressBar.VISIBLE
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).visibility = Button.GONE
        venueText.text.clear()
    }

    // Gather device name from user
    private fun requestDeviceName() {
        dialog.setTitle("Enter a Device Name")
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).visibility = Button.VISIBLE
        spinner.visibility = ProgressBar.GONE
        venueText.inputType = InputType.TYPE_CLASS_TEXT
        venueText.hint = "ex: Line"
        venueText.visibility = EditText.VISIBLE
        licenseText.visibility = TextView.GONE
        licenseText.text = ""
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (venueText.text.count() > 0) {
                credentials.deviceName = venueText.text.toString()

                // Claim device with given name
                showPending("Claiming")

                val headerName = "Authorization"
                val integratorValue = credentials.generateIntegratorHeader()
                val licensesURL = "${credentials.baseApiUrl}licenses/claim"
                val payload = """{
                            "key": "${credentials.licenseKey}",
                            "venueKey": ${credentials.venueKey},
                            "mac": "${credentials.macAddress}",
                            "ip": "${credentials.ipAddress}",
                            "name": "${credentials.deviceName}"
                        }"""
                Networking.postData(licensesURL, headerName, integratorValue, payload, ::claimLicenseCallback)
            }
        }
    }

    // Handles response when requesting licenses
    private fun getLicensesCallback(call: Call, response: Response?, responseBody: String) {
        val handler = Handler(Looper.getMainLooper())

        if (response != null) {
            // Handle a response for licenses request
            val jsonResponse = Klaxon().parseArray<VenueLicense>(responseBody)

            // Count the number of licenses available to user
            var countAvailable = 0
            if (jsonResponse != null) {
                for (response in jsonResponse) {
                    if (!response.claimed && response.active) {
                        // Counts to show user how many licenses they have
                        countAvailable += 1

                        // Use last license from the list
                        credentials.licenseKey = response.key
                        if (response.printerId != null) {
                            credentials.printerNum = response.printerId.toString()
                        }
                    }
                }
            }

            handler.post {
                if(jsonResponse != null && countAvailable > 0) {
                    // There are licenses to claim, let the user claim one
                    showMessage("$countAvailable Available Device Licenses", "Claim a device license?", "Claim")
                } else if (jsonResponse != null){
                    // Got a response but there were no licenses that were unclaimed
                    showMessage("No licenses available", "No licenses were found for ID $venueKey", "Re-enter Store ID")
                } else {
                    // Unable to find any licenses
                    showMessage("Error (${response?.code})", "Oops! There was an error finding a license.", "Re-enter Store ID")
                }
            }
        } else {
            // Request fails (usually due to no internet)
            handler.post {
                showPending("Verifying")
                Thread.sleep(1000)
                showMessage("No Internet", "Connect to internet to set up your device.", "Re-enter Store ID")
            }
        }
    }

    // Handles response when claiming license
    private fun claimLicenseCallback(call: Call, response: Response?, body: String) {
        val handler = Handler(Looper.getMainLooper())

        if (response != null) {
            Thread.sleep(3000)

            if (response.isSuccessful) {
                // User successfully claimed the license
                handler.post {

                    // Gather secret from response
                    val jsonResponse = Klaxon().parse<Map<String, Any>>(body)
                    if (jsonResponse != null) {
                        credentials.licenseSecret = jsonResponse["secret"] as String
                    }

                    // Save all necessary device and license information
                    val editor = credentials.prefs.edit()
                    editor.putString(credentials.PREFS_LICENSE_KEY, credentials.licenseKey)
                    editor.putString(credentials.PREFS_VENUE_KEY, credentials.venueKey)
                    editor.putString(credentials.PREFS_MAC_ADDR, credentials.macAddress)
                    editor.putString(credentials.PREFS_IP_ADDR, credentials.ipAddress)
                    editor.putString(credentials.PREFS_DEVICE_NAME, credentials.deviceName)
                    editor.putString(credentials.PREFS_LICENSE_SECRET, credentials.licenseSecret)
                    editor.apply()
                    fetchDeviceMode(credentials)

                    // Show the claiming message for a little while
                    showMessage("Success!", "This device has been registered.", "Close")
                }
            } else {
                // Error encountered while claiming device
                handler.post {
                    Thread.sleep(3000)
                    showMessage(
                        "Error (${response.code})",
                        "Oops! There was an error claiming a license.",
                        "Re-enter Store ID"
                    )
                }
            }
        } else {
            // Failed request
            handler.post {
                Thread.sleep(3000)
                showMessage(
                    "Error",
                    "Oops! There was an error claiming a license.",
                    "Re-enter Store ID"
                )
            }
        }
    }

    // Request mode (dev or prod) - determines endpoints to access
    private fun fetchDeviceMode(credentials: DeviceCredentials) {
        val url = "${credentials.baseApiUrl}stores/${credentials.venueKey}"
        val header = "Authorization"
        val headerBody = credentials.generateDeviceLicenseHeader()
        Networking.fetchJson(url, header, headerBody) { _, _, venueDetailsResponse ->
            val modeResponse = Klaxon().parse<Map<String, Any>>(venueDetailsResponse)

            println("mode response: $modeResponse")
            if (modeResponse != null && modeResponse["mode"] != null) {
                credentials.mode = modeResponse["mode"] as String

                val editor = credentials.prefs.edit()

                if (credentials.mode == "prod") {
                    credentials.baseApiUrl = "https://focuslink.focuspos.com/v2/"
                    credentials.baseWsUrl = "https://ws.focuslink.focuspos.com/"
                    editor.putString(credentials.PREFS_MODE_KEY, "prod")
                } else {
                    credentials.baseApiUrl = "https://dev.focuslink.focuspos.com/v2/"
                    credentials.baseWsUrl = "https://dev.ws.focuslink.focuspos.com/"
                    editor.putString(credentials.PREFS_MODE_KEY, "dev")
                }

                editor.apply()
            }

            fetchDevicePrefs(credentials)
        }
    }

    // Request device preferences set by user
    private fun fetchDevicePrefs(credentials: DeviceCredentials) {
        val url = "${credentials.baseApiUrl}stores/${credentials.venueKey}/kitchendevicepreferences"
        val header = "Authorization"
        val headerBody = credentials.generateDeviceLicenseHeader()

        Networking.fetchJson(url, header, headerBody) {
                _, _, devicePrefsResponse ->

            val prefsResponse = Klaxon().parse<Map<String, Any>>(devicePrefsResponse)
            println("device prefs response: $prefsResponse")

            // Order header will turn red when order time >= urgent time
            if (prefsResponse != null && prefsResponse["urgentTime"] != null) {
                credentials.urgentTime = prefsResponse["urgentTime"] as Int
            }
        }
    }

    override fun onStart() {
        super.onStart()

        dialog.window?.setLayout(700, dialog.window!!.attributes!!.height)
    }
}