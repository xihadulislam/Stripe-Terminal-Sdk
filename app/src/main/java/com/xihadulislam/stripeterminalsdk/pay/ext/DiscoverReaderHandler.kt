package com.xihadulislam.stripeterminalsdk.pay.ext

import android.content.Context
import com.stripe.stripeterminal.external.callable.Cancelable
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.TAP_TO_PAY_REQUEST_CODE
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.WISE_PAD_3_REQUEST_CODE
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.WISE_POS_E_REQUEST_CODE
import com.xihadulislam.stripeterminalsdk.pay.ext.discovers.TapToPayExt
import com.xihadulislam.stripeterminalsdk.pay.ext.discovers.WisePad3Ext
import com.xihadulislam.stripeterminalsdk.pay.ext.discovers.WisePosExt
import com.xihadulislam.stripeterminalsdk.pay.listeners.WisePosEventListener

class DiscoverReaderHandler(
    private var wisePosEHandler: WisePosExt,
    private var wisePad3Handler: WisePad3Ext,
    private var tapToPayExt: TapToPayExt
) {

    companion object {

        var discoverCancelable: Cancelable? = null

        private const val TAG = "WisePosPaymentHandler"

        @Volatile
        private var instance: DiscoverReaderHandler? = null

        fun getInstance(context: Context, ipAddress: String): DiscoverReaderHandler {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = DiscoverReaderHandler(
                            WisePosExt.getInstance(context, ipAddress),
                            WisePad3Ext.getInstance(context),
                            TapToPayExt.getInstance(context)
                        )
                    }
                }
            }
            return instance!!
        }
    }


    fun discoverReaders(
        requestSdkType: Int,
        wisePosEventListener: WisePosEventListener? = null,
        callback: () -> Unit = { }
    ) {
        when (requestSdkType) {
            WISE_POS_E_REQUEST_CODE -> {
                wisePosEHandler.discoverWisePosEReaders(wisePosEventListener) {
                    callback()
                }
            }

            WISE_PAD_3_REQUEST_CODE -> {
                wisePad3Handler.discoverWisePad3Readers(wisePosEventListener) {
                    callback()
                }
            }

            TAP_TO_PAY_REQUEST_CODE -> {
                tapToPayExt.discoverTapToPayReaders(wisePosEventListener) {
                    callback()
                }
            }
            else -> {
                callback()
            }
        }
    }


}