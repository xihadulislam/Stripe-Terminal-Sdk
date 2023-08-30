package com.xihadulislam.stripeterminalsdk.pay.models

/**
 * A one-field data class used to handle the connection token response from our backend
 */
data class PaymentIntentSecret(val client_secret: String)
