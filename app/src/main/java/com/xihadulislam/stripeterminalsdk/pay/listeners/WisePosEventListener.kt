package com.xihadulislam.stripeterminalsdk.pay.listeners

import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.TerminalException

interface WisePosEventListener {
    fun onProgress(event: String, status: String)

    fun onSuccess(paymentIntent: PaymentIntent)

    fun onFailure(event: String, msg: String, e: TerminalException?)

    fun onCancel(event: String, msg: String)
}