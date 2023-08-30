package com.xihadulislam.stripeterminalsdk.pay.ext.discovers

import android.content.Context
import com.stripe.stripeterminal.Terminal
import com.stripe.stripeterminal.external.callable.Callback
import com.stripe.stripeterminal.external.callable.DiscoveryListener
import com.stripe.stripeterminal.external.callable.ReaderCallback
import com.stripe.stripeterminal.external.models.ConnectionConfiguration
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration
import com.stripe.stripeterminal.external.models.DiscoveryMethod
import com.stripe.stripeterminal.external.models.Reader
import com.stripe.stripeterminal.external.models.TerminalException
import com.xihadulislam.stripeterminalsdk.pay.ext.DiscoverReaderHandler.Companion.discoverCancelable
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.CONNECTING_READER
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.isProcessNextStep
import com.xihadulislam.stripeterminalsdk.pay.listeners.WisePosEventListener
import com.xihadulislam.stripeterminalsdk.pay.utils.AppPref
import com.xihadulislam.stripeterminalsdk.pay.utils.WisePosValidator
import com.xihadulislam.stripeterminalsdk.pay.utils.WisePosValidator.getValidReader

class WisePosExt private constructor(
    private var pref: AppPref,
    private var ipAddress: String,
) {

    companion object {

        private const val TAG = "WisePosPaymentHandler"

        @Volatile
        private var instance: WisePosExt? = null

        fun getInstance(context: Context, ipAddress: String): WisePosExt {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = WisePosExt(AppPref.getInstance(context), ipAddress)
                    }
                }
            }
            return instance!!
        }
    }

    private var listener: WisePosEventListener? = null


    fun discoverWisePosEReaders(
        wisePosEventListener: WisePosEventListener? = null,
        callback: () -> Unit = { }
    ) {

        this.listener = wisePosEventListener


        val config = DiscoveryConfiguration(
            timeout = 0, discoveryMethod = DiscoveryMethod.INTERNET, isSimulated = false,
        )

        listener?.onProgress(SdkHandler.EVENT_DISCOVER_START, "Discover Readers")

        discoverCancelable =
            Terminal.getInstance().discoverReaders(config, object : DiscoveryListener {
                override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                    listener?.onProgress(
                        SdkHandler.EVENT_DISCOVER_SUCCESS,
                        "Processing Readers"
                    )
                    verifyAndConnectInternetReader(readers, callback)
                }
            }, object : Callback {
                override fun onSuccess() {
                    discoverCancelable = null
                }

                override fun onFailure(e: TerminalException) {
                    discoverCancelable = null
                    listener?.onFailure("discoverReaders", e.errorMessage, e)
                }
            })



    }

    private fun verifyAndConnectInternetReader(readers: List<Reader>, callback: () -> Unit = { }) {
        if (isProcessNextStep) {

            readers.getValidReader( ipAddress)
                .let {
                    if (it.status == WisePosValidator.SUCCESS) {
                        it.reader?.let { red -> connectInternetReader(red, callback) }
                    } else {
                        listener?.onFailure("ValidReader", it.msg, null)
                    }
                }

        } else {
            listener?.onCancel("Discover", "DiscoverReaders was canceled by the user")
        }
    }


    private fun connectInternetReader(firstReader: Reader, callback: () -> Unit = { }) {
        if (isProcessNextStep) {

            listener?.onProgress(CONNECTING_READER, "Connecting Reader")
            val config = ConnectionConfiguration.InternetConnectionConfiguration(true)
            Terminal.getInstance()
                .connectInternetReader(firstReader, config, object : ReaderCallback {
                    override fun onSuccess(reader: Reader) {
                        listener?.onProgress(SdkHandler.EVENT_COMMON, " Reader Connected")
                        callback()
                    }

                    override fun onFailure(e: TerminalException) {
                        listener?.onFailure("connectInternetReader", e.errorMessage, e)
                    }
                })
        } else {
            listener?.onCancel("Discover", "DiscoverReaders was canceled by the user")
        }
    }

}