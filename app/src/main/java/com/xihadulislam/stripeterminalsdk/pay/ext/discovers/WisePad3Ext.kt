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
import com.xihadulislam.stripeterminalsdk.pay.ext.DiscoverReaderHandler.Companion.discoverCancelable
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.CONNECTING_READER
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.isProcessNextStep
import com.xihadulislam.stripeterminalsdk.pay.listeners.TerminalBluetoothReaderListener
import com.xihadulislam.stripeterminalsdk.pay.listeners.WisePosEventListener

class WisePad3Ext private constructor(
    private var pref: AppPref,
) {

    companion object {

        private const val TAG = "WisePosPaymentHandler"

        @Volatile
        private var instance: WisePad3Ext? = null

        fun getInstance(context: Context): WisePad3Ext {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = WisePad3Ext(AppPref.getInstance(context))
                    }
                }
            }
            return instance!!
        }
    }

    private var listener: WisePosEventListener? = null

    fun discoverWisePad3Readers(
        wisePosEventListener: WisePosEventListener? = null,
        callback: () -> Unit = { }
    ) {

        this.listener = wisePosEventListener


        val config = DiscoveryConfiguration(
            timeout = 0, discoveryMethod = DiscoveryMethod.BLUETOOTH_SCAN, isSimulated = false
        )

        discoverCancelable =
            Terminal.getInstance().discoverReaders(config, object : DiscoveryListener {
                override fun onUpdateDiscoveredReaders(readers: List<Reader>) {
                    listener?.onProgress(
                        SdkHandler.EVENT_DISCOVER_SUCCESS,
                        "Processing Readers"
                    )
                    verifyAndConnectBluetoothReader(readers, callback)
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

        listener?.onProgress(SdkHandler.EVENT_DISCOVER_START, "Discover Readers")

    }


    private fun verifyAndConnectBluetoothReader(readers: List<Reader>, callback: () -> Unit = { }) {
        if (isProcessNextStep) {
            if (readers.isNotEmpty()) {
                connectBluetoothReader(readers[0], callback)
            } else {
                listener?.onFailure("ValidReader", "No physical reader found", null)
            }
        } else {
            listener?.onCancel("Discover", "DiscoverReaders was canceled by the user")
        }
    }


    private fun connectBluetoothReader(firstReader: Reader, callback: () -> Unit = { }) {
        if (isProcessNextStep) {

            listener?.onProgress(CONNECTING_READER, "Connecting Reader")
            val config = ConnectionConfiguration.BluetoothConnectionConfiguration(
                pref.stripeTerminalReaderLocation
            )

            Terminal.getInstance()
                .connectBluetoothReader(
                    firstReader,
                    config,
                    TerminalBluetoothReaderListener(),
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