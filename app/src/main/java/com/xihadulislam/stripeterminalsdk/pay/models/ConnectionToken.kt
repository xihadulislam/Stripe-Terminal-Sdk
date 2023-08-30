package com.xihadulislam.stripeterminalsdk.pay.models

import com.google.gson.annotations.SerializedName

/**
 * A one-field data class used to handle the connection token response from our backend
 */
data class ConnectionToken(
    val secret: String = "",
    val location: String = "",
    val stripe_acc: String = ""
)



data class ConnectionTokenResponse(
    val statusCode: Int, val msg: String, val body: ConnectionToken
)
