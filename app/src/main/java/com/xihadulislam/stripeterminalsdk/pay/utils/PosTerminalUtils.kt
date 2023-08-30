package com.xihadulislam.stripeterminalsdk.pay.utils

import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.gson.JsonParser
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.SERVER_DRIVEN
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.SERVER_DRIVEN_REQUEST_CODE
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.TAP_TO_PAY
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.TAP_TO_PAY_REQUEST_CODE
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.WISE_PAD_3
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.WISE_PAD_3_REQUEST_CODE
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.WISE_POS_E
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.WISE_POS_E_REQUEST_CODE
import org.json.JSONObject


object PosTerminalUtils {

    fun JSONObject.toJsonObject() = JsonParser().parse(this.toString()).asJsonObject


    val Any?.isNull get() = this == null


    fun Any?.ifNull(block: () -> Unit) = run {
        if (this == null) {
            block()
        }
    }

    fun Boolean?.ifTrue(block: () -> Unit) = run {
        if (this == true) {
            block()
        }
    }

    fun deleteCache(context: Context) {
        context.cacheDir.deleteRecursively()
    }

    fun isApplicationDebuggable(context: Context) =
        0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE

    fun getRequestTypeLabel(requestSdkType: Int): String {
        return when (requestSdkType) {
            WISE_POS_E_REQUEST_CODE -> {
                return WISE_POS_E
            }

            WISE_PAD_3_REQUEST_CODE -> {
                return WISE_PAD_3
            }

            TAP_TO_PAY_REQUEST_CODE -> {
                return TAP_TO_PAY
            }

            SERVER_DRIVEN_REQUEST_CODE -> {
                return SERVER_DRIVEN
            }

            else -> {
                ""
            }
        }
    }

    fun getRequestType(terminalUsType: String): Int {
        return when (terminalUsType) {
            "wisepos_e" -> {
                return WISE_POS_E_REQUEST_CODE
            }

            "wisepad_3" -> {
                return WISE_PAD_3_REQUEST_CODE
            }

            else -> {
                0
            }
        }
    }


}