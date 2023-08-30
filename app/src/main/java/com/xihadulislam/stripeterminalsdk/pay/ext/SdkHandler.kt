package com.xihadulislam.stripeterminalsdk.pay.ext

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.models.TerminalException
import com.stripe.stripeterminal.log.LogLevel
import com.xihadulislam.stripeterminalsdk.pay.ext.DiscoverReaderHandler.Companion.discoverCancelable
import com.xihadulislam.stripeterminalsdk.pay.utils.AppPref
import org.json.JSONObject
import com.xihadulislam.stripeterminalsdk.pay.listeners.TerminalEventListener
import com.xihadulislam.stripeterminalsdk.pay.listeners.TokenProvider
import com.xihadulislam.stripeterminalsdk.pay.listeners.WisePosEventListener
import com.xihadulislam.stripeterminalsdk.pay.utils.PosTerminalUtils.isNull


class SdkHandler private constructor(
    private var pref: AppPref,
    private var collectPaymentHandler: CollectPaymentHandler,
    private var discoverReaderHandler: DiscoverReaderHandler,
) {

    companion object {

        const val WISE_POS_E = "wisepos_e"
        const val WISE_PAD_3 = "wisepad_3"
        const val TAP_TO_PAY = "tap to pay"
        const val SERVER_DRIVEN = "server driven"

        const val WISE_POS_E_REQUEST_CODE = 111
        const val WISE_PAD_3_REQUEST_CODE = 222
        const val TAP_TO_PAY_REQUEST_CODE = 333
        const val SERVER_DRIVEN_REQUEST_CODE = 444


        const val EVENT_COMMON = "EVENT_COMMON"
        const val CONNECTING_READER = "CONNECTING_READER"
        const val EVENT_DISCOVER_START = "EVENT_DISCOVER_START"
        const val EVENT_DISCOVER_SUCCESS = "EVENT_DISCOVER_SUCCESS"
        const val EVENT_CONNECT_READER = "EVENT_CONNECT_READER"
        const val EVENT_PAYMENT_START = "EVENT_PAYMENT_START"
        const val EVENT_PAYMENT_INTENT = "EVENT_PAYMENT_INTENT"
        const val EVENT_PAYMENT_INTENT_CREATED = "EVENT_PAYMENT_INTENT_CREATED"
        const val EVENT_PAYMENT_START_COLLECTING = "EVENT_PAYMENT_START_COLLECTING"
        const val EVENT_PAYMENT_COLLECTED = "EVENT_PAYMENT_COLLECTED"
        const val EVENT_PAYMENT_PROCESS_START = "EVENT_PAYMENT_PROCESS_START"
        const val EVENT_PAYMENT_PROCESSED = "EVENT_PAYMENT_PROCESSED"
        const val EVENT_PAYMENT_PROCESSED_UPDATE = "EVENT_PAYMENT_PROCESSED_UPDATE"

        private const val TAG = "WisePosPaymentHandler"

        var isProcessNextStep: Boolean = true


        @Volatile
        private var instance: SdkHandler? = null

        fun getInstance(context: Context): SdkHandler {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        val wm: WifiManager =
                            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                        val ipAddress = Formatter.formatIpAddress(wm.connectionInfo.ipAddress)
                        instance = SdkHandler(
                            AppPref.getInstance(context),
                            CollectPaymentHandler.getInstance(context, ipAddress),
                            DiscoverReaderHandler.getInstance(context, ipAddress)
                        )
                        instance?.stripeInit(context)
                    }
                }
            }
            return instance!!
        }


        class SdkBuilder {


        }


    }


    private var skipTipping: Boolean = true
    private var payload: JSONObject = JSONObject()
    private var listener: WisePosEventListener? = null


    private var requestSdkType: Int = 0


    private fun stripeInit(context: Context) {
        try {
            Log.d(TAG, "stripeInit: " + Terminal.isInitialized())
            if (!Terminal.isInitialized()) {
                Terminal.initTerminal(
                    context.applicationContext,
                    LogLevel.VERBOSE,
                    TokenProvider(context),
                    TerminalEventListener()
                )
            }
            Terminal.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    fun processSdkPayment(
        sdkType: Int,
        skip_tipping: Boolean,
        payloadBody: JSONObject,
        wisePosEventListener: WisePosEventListener? = null
    ) {
        this.requestSdkType = sdkType
        isProcessNextStep = true
        this.skipTipping = skip_tipping
        this.payload = payloadBody
        this.listener = wisePosEventListener
        discoverCancelable = null

        try {
            checkAndStartPayment()
        } catch (e: Exception) {
            e.printStackTrace()
            listener?.onFailure(
                "Location",
                "Location services must be enabled to use Terminal",
                null
            )
        }
    }

    private fun checkAndStartPayment() {
        if (pref.lastConnectedSdk == requestSdkType) {
            startSdkPayment()
        } else {
            disconnectReader {
                startSdkPayment()
            }
        }
    }

    private fun startSdkPayment() {
        if (Terminal.getInstance().connectedReader == null) {
            discoverReaders()
        } else {
            startPayment()
        }
        pref.lastConnectedSdk = requestSdkType
    }

    private fun discoverReaders() {
        discoverReaderHandler.discoverReaders(requestSdkType, listener) {
            startPayment()
        }
    }


    private fun startPayment() {
        collectPaymentHandler.startPayment(requestSdkType, payload, skipTipping, listener)
    }


    fun forceCancelClickAction() {
        isProcessNextStep = false
        cancelDiscoverAndTerminal()
        disconnectReader { }
        listener?.onCancel("Payment", "Canceled by the user")
    }


    fun cancelClickAction() {
        isProcessNextStep = false
        if (!discoverCancelable.isNull) {
            listener?.onCancel("Discover", "DiscoverReaders was canceled by the user")
            cancelDiscoverAndTerminal()
        } else if (collectPaymentHandler.cancelable.isNull) {
            listener?.onCancel("Payment", "Payment was canceled by the user")
            cancelDiscoverAndTerminal()
        } else {
            cancelDiscoverAndTerminal {
                listener?.onCancel("Payment", "Canceled by the user")
            }
        }

        Log.d(TAG, "cancel: ${collectPaymentHandler.cancelable}  --  $discoverCancelable")

    }


    fun cancelDiscoverAndTerminal(onSuccess: (msg: String?) -> Unit = { }) {
        if (discoverCancelable == null && collectPaymentHandler.cancelable == null) onSuccess(null)
        else {
            cancelDiscoverReader {
                cancelTerminal {
                    onSuccess("Success")
                }
            }
        }
    }

    private fun cancelDiscoverReader(callback: (msg: String?) -> Unit = { }) {
        discoverCancelable?.cancel(object : Callback {
            override fun onSuccess() {
                discoverCancelable = null
                callback("Success")
            }

            override fun onFailure(e: TerminalException) {
                discoverCancelable = null
                callback("Failed")
            }
        }) ?: run {
            callback(null)
        }
    }

    private fun cancelTerminal(callback: (msg: String?) -> Unit = { }) {
        collectPaymentHandler.cancelTerminal {
            callback(it)
        }
    }

    fun cancelAndDisconnectReader(callback: () -> Unit = { }) {
        cancelDiscoverAndTerminal {
            disconnectReader {
                callback()
            }
        }
    }

    private fun disconnectReader(callback: () -> Unit = { }) {
        if (Terminal.isInitialized()) {
            Terminal.getInstance().disconnectReader(object : Callback {
                override fun onSuccess() {
                    callback()
                }

                override fun onFailure(e: TerminalException) {
                    callback()
                }
            })
        }
        cacheCleanReader()
    }

    private fun cacheCleanReader() {
        if (Terminal.isInitialized()) {
            Terminal.getInstance().clearCachedCredentials()
        }
    }

}