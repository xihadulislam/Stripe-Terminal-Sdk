package com.xihadulislam.stripeterminalsdk.pay.network

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import com.xihadulislam.stripeterminalsdk.pay.models.ConnectionTokenResponse
import com.xihadulislam.stripeterminalsdk.pay.models.PaymentIntentSecret


interface SdkService {

    /**
     * Get a connection token string from the backend
     */
    @GET("stripe/terminal/connection/token")
    fun getConnectionToken(@Query("rid") rid: String): Call<ConnectionTokenResponse>

    @POST("stripe/payment/terminal")
    fun getPaymentIntent(@Body `object`: JsonObject?): Call<PaymentIntentSecret>



}
