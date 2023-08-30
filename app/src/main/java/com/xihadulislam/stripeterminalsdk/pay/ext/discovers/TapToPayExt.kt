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
import com.xihadulislam.stripeterminalsdk.pay.utils.AppPref
import com.xihadulislam.stripeterminalsdk.pay.ext.DiscoverReaderHandler
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.CONNECTING_READER
import com.xihadulislam.stripeterminalsdk.pay.listeners.WisePosEventListener
import com.xihadulislam.stripeterminalsdk.pay.utils.PosTerminalUtils

class TapToPayExt private constructor(
    private var pref: AppPref,
    private var isApplicationDebuggable: Boolean
) {

    companion object {

        private const val TAG = "WisePosPaymentHandler"

        @Volatile
        private var instance: TapToPayExt? = null

        fun getInstance(context: Context): TapToPayExt {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = TapToPayExt(
                            AppPref.getInstance(context),
                            PosTerminalUtils.isApplicationDebuggable(context)
                        )
                    }
                }
            }
            return instance!!
        }
    }

    private var listener: WisePosEventListener? = null

    fun discoverTapToPayReaders(
        wisePosEventListener: WisePosEventListener? = null,
        callback: () -> Unit = { }
    ) {

        this.listener = wisePosEventListener


        val config = DiscoveryConfiguration(
            timeout = 0,
            discoveryMethod = DiscoveryMethod.LOCAL_MOBILE,
            isSimulated = isApplicationDebuggable,
            location = pref.stripeTerminalReaderLocation
        )

        DiscoverReaderHandler.discoverCancelable =
            Terminal.getInstance().discoverReaders(config, object : DiscoveryListener {
                override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                    listener?.onProgress(
                        SdkHandler.EVENT_DISCOVER_SUCCESS,
                        "Processing Readers"
                    )
                    verifyAndConnectLocalMobileReader(readers, callback)
                }
            }, object : Callback {
                override fun onSuccess() {
                    DiscoverReaderHandler.discoverCancelable = null
                }

                override fun onFailure(e: TerminalException) {
                    DiscoverReaderHandler.discoverCancelable = null
                    listener?.onFailure("discoverReaders", e.errorMessage, e)
                }
            })

        listener?.onProgress(SdkHandler.EVENT_DISCOVER_START, "Discover Readers")

    }


    private fun verifyAndConnectLocalMobileReader(
        readers: List<Reader>,
        callback: () -> Unit = { }
    ) {
        if (SdkHandler.isProcessNextStep) {
            if (readers.isNotEmpty()) {
                connectLocalMobileReader(readers[0], callback)
            } else {
                listener?.onFailure("ValidReader", "No physical reader found", null)
            }
        } else {
            listener?.onCancel("Discover", "DiscoverReaders was canceled by the user")
        }
    }


    private fun connectLocalMobileReader(firstReader: Reader, callback: () -> Unit = { }) {
        if (SdkHandler.isProcessNextStep) {

            listener?.onProgress(CONNECTING_READER, "Connecting Reader")
            val config = ConnectionConfiguration.LocalMobileConnectionConfiguration(
                pref.stripeTerminalReaderLocation
            )

            Terminal.getInstance()
                .connectLocalMobileReader(
                    firstReader,
                    config,
                    object : ReaderCallback {
                        override fun onSuccess(reader: Reader) {
                            listener?.onProgress(
                                SdkHandler.EVENT_COMMON,
                                " Reader Connected"
                            )
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