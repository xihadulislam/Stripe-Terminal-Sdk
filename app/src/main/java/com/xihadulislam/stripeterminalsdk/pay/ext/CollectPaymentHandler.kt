package com.xihadulislam.stripeterminalsdk.pay.ext

import android.content.Context
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.Cancelable
import com.stripe.stripeterminal.external.callable.PaymentIntentCallback
import com.stripe.stripeterminal.external.models.CollectConfiguration
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.TerminalException

import org.json.JSONObject
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.EVENT_PAYMENT_COLLECTED
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.EVENT_PAYMENT_PROCESSED
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.EVENT_PAYMENT_PROCESS_START
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.EVENT_PAYMENT_START
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.EVENT_PAYMENT_START_COLLECTING
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.isProcessNextStep
import needle.Needle
import com.xihadulislam.stripeterminalsdk.pay.network.TerminalApiClient
import com.xihadulislam.stripeterminalsdk.pay.utils.PosTerminalUtils.isNull
import com.xihadulislam.stripeterminalsdk.pay.listeners.WisePosEventListener
import com.xihadulislam.stripeterminalsdk.pay.utils.PosTerminalUtils.toJsonObject

class CollectPaymentHandler private constructor(
    private var discoverReaderHandler: DiscoverReaderHandler
) {

    companion object {

        private const val TAG = "WisePosPaymentHandler"

        @Volatile
        private var instance: CollectPaymentHandler? = null

        fun getInstance(context: Context, ipAddress: String): CollectPaymentHandler {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = CollectPaymentHandler(
                            DiscoverReaderHandler.getInstance(context, ipAddress)
                        )
                    }
                }
            }
            return instance!!
        }
    }

    private var skipTipping: Boolean = true
    private var listener: WisePosEventListener? = null
    var cancelable: Cancelable? = null
    var retryCount = 0
    private var requestSdkType: Int = 0
    private var payload: JSONObject = JSONObject()

    var secret: String? = null

    fun startPayment(
        sdkType: Int,
        payload: JSONObject,
        skipTipping: Boolean,
        wisePosEventListener: WisePosEventListener? = null
    ) {
        this.payload = payload
        this.requestSdkType = sdkType
        this.retryCount = 0
        this.skipTipping = skipTipping
        this.listener = wisePosEventListener
        this.secret = null

        processedPayment()
    }

    private fun processedPayment() {

        if (isProcessNextStep) {

            listener?.onProgress("PAYMENT_INIT", "Generating payment intent")

            Needle.onBackgroundThread().execute {

                if (secret.isNull) {
                    secret = TerminalApiClient.createSdkPaymentIntent(payload.toJsonObject())
                }

                listener?.onProgress(
                    EVENT_PAYMENT_START,
                    "Payment has been initiated"
                )

                secret?.let {
                    Terminal.getInstance().retrievePaymentIntent(it, retrievePaymentIntentCallback)
                }
                if (secret == null) {
                    listener?.onFailure(
                        "retrievePaymentIntent",
                        "Payment Intent Couldn't started (timeout) due to internet issue.",
                        null
                    )
                }
            }
        } else {
            listener?.onCancel("Payment", "Payment was Canceled")
        }
    }


    private val retrievePaymentIntentCallback by lazy {
        object : PaymentIntentCallback {
            override fun onSuccess(paymentIntent: PaymentIntent) {

                if (isProcessNextStep) {
                    val collectConfig = CollectConfiguration.Builder().updatePaymentIntent(true)
                        .skipTipping(skipTipping).build()

                    cancelable = Terminal.getInstance().collectPaymentMethod(
                        paymentIntent, collectPaymentMethodCallback, collectConfig
                    )
                    listener?.onProgress(
                        EVENT_PAYMENT_START_COLLECTING,
                        "Payment started on Reader"
                    )
                } else {
                    listener?.onCancel("Payment", "Payment was Canceled")
                }


            }

            override fun onFailure(e: TerminalException) {
                triggerOnFailure("createPaymentIntentCallback", e.errorMessage, e)
            }
        }
    }

    private val collectPaymentMethodCallback by lazy {
        object : PaymentIntentCallback {
            override fun onSuccess(paymentIntent: PaymentIntent) {
                cancelable = null
                if (isProcessNextStep) {
                    listener?.onProgress(
                        EVENT_PAYMENT_COLLECTED,
                        " Payment Collected"
                    )
                    Terminal.getInstance().processPayment(paymentIntent, processPaymentCallback)
                    listener?.onProgress(
                        EVENT_PAYMENT_PROCESS_START,
                        "Payment Processing"
                    )

                } else {
                    listener?.onCancel("Payment", "Payment was Canceled")
                }

            }

            override fun onFailure(e: TerminalException) {
                if (e.errorMessage.contains("no longer authenticated")) {
                    retryPayment(e)
                } else {
                    triggerOnFailure("collectPaymentMethodCallback", e.errorMessage, e)
                }

            }
        }
    }


    private val processPaymentCallback by lazy {
        object : PaymentIntentCallback {
            override fun onSuccess(paymentIntent: PaymentIntent) {
                if (isProcessNextStep) {
                    listener?.onProgress(
                        EVENT_PAYMENT_PROCESSED,
                        "Payment Successful"
                    )
                    listener?.onSuccess(paymentIntent)
                } else {
                    listener?.onCancel("Payment", "Payment was Canceled")
                }

            }

            override fun onFailure(e: TerminalException) {
                triggerOnFailure("processPaymentCallback", e.errorMessage, e)
            }
        }
    }

    private fun retryPayment(e: TerminalException?) {
        Terminal.getInstance().disconnectReader(object : Callback {
            override fun onSuccess() {
                if (retryCount < 3) {
                    isProcessNextStep = true
                    discoverReaderHandler.discoverReaders(requestSdkType, listener) {
                        processedPayment()
                    }
                } else {
                    triggerOnFailure(
                        "collectPaymentMethodCallback",
                        e?.errorMessage ?: "Payment was declined",
                        e
                    )
                }
                retryCount++
            }

            override fun onFailure(e: TerminalException) {

            }
        })
    }

    fun cancelTerminal(callback: (msg: String?) -> Unit = { }) {
        cancelable?.cancel(object : Callback {
            override fun onSuccess() {
                cancelable = null
                callback("Success")
            }

            override fun onFailure(e: TerminalException) {
                cancelable = null
                callback("Failed")
            }
        }) ?: run {
            callback(null)
        }
    }

    private fun triggerOnFailure(event: String, msg: String, e: TerminalException?) {
        listener?.onFailure(event, msg, e)
    }

}