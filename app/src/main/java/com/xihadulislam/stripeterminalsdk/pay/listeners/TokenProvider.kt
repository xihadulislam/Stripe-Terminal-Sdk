package com.xihadulislam.stripeterminalsdk.pay.listeners

import android.content.Context
import android.util.Log
import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider
import com.stripe.stripeterminal.external.models.ConnectionTokenException
import com.xihadulislam.stripeterminalsdk.pay.utils.AppPref
import com.xihadulislam.stripeterminalsdk.pay.network.TerminalApiClient


class TokenProvider(context: Context) : ConnectionTokenProvider {

    private val pref: AppPref = AppPref.getInstance(context)

    override fun fetchConnectionToken(callback: ConnectionTokenCallback) {
        try {
            val body = TerminalApiClient.createConnectionToken(pref.resId)
            pref.stripeTerminalReaderLocation = body.location
            //   pref.stripe_acc = body.stripe_acc
            callback.onSuccess(body.secret)
            Log.d("WisePosPaymentHandler", "fetchConnectionToken: ${body}")
        } catch (e: ConnectionTokenException) {
            callback.onFailure(e)
        }
    }
}
