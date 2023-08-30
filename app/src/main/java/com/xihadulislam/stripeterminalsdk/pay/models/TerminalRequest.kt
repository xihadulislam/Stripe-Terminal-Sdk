package com.xihadulislam.stripeterminalsdk.pay.models

import com.google.gson.annotations.SerializedName


data class TerminalRequest(
    @SerializedName("amount") var amount: Int = 0,
    @SerializedName("currency") var currency: String = "BDT",
    @SerializedName("statement") var statement: String = "",
    @SerializedName("reader") var reader: String = "",
    @SerializedName("deposit") var deposit: Boolean = true,
    @SerializedName("skip_tipping") var skipTipping: Boolean = false,
    @SerializedName("device_id") var deviceId: String = "",
)
