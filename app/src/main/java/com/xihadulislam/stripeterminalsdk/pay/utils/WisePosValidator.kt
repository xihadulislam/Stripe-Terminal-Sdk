package com.xihadulislam.stripeterminalsdk.pay.utils

import android.util.Log
import com.stripe.stripeterminal.external.models.Reader
import com.xihadulislam.stripeterminalsdk.pay.utils.WisePosValidator.FAILED
import com.xihadulislam.stripeterminalsdk.pay.models.Readers

object WisePosValidator {

    const val SUCCESS = "SUCCESS"
    const val FAILED = "FAILED"


    fun List<Reader>.getValidReader(
        ipAddress: String
    ): ValidatorState {

        Log.d("WisePosPaymentHandler", "getValidReader: device ip $ipAddress")

        val state = ValidatorState()

        if (this.isEmpty()) {
            state.apply { msg = "No physical reader found" }
        } else {
            return getState(this[0], state, ipAddress)
        }

        return state
    }


    private fun getState(
        der: Reader,
        state: ValidatorState,
        ipAddress: String,
    ): ValidatorState {

        state.apply {
            reader = der
            status = SUCCESS
        }

        return state
    }

    private fun isIpMatch(ipDevice: List<String>, ipReader: List<String>): Boolean {
        if (ipDevice.size == 4 && ipReader.size == 4) {
            if (ipDevice[0] == ipReader[0] && ipDevice[1] == ipReader[1] && ipDevice[2] == ipReader[2]) return true
        }
        return false
    }


}

data class ValidatorState(
    var status: String = FAILED, var msg: String = "", var reader: Reader? = null

)