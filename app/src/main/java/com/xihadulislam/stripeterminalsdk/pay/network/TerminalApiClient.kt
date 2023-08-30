package com.xihadulislam.stripeterminalsdk.pay.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.stripe.stripeterminal.external.models.ConnectionTokenException
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.xihadulislam.stripeterminalsdk.pay.models.ConnectionToken
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * The `ApiClient` is a singleton object used to make calls to our backend and return their results
 */
object TerminalApiClient {

    private var instance: SdkService? = null

    private fun getClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    @Synchronized
    fun getSdkInstance(): SdkService {
        if (instance == null) {

            instance = Retrofit.Builder()
                .baseUrl("PAYMENT_BASE_URL")
                .client(getClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(SdkService::class.java)
        }

        return instance as SdkService
    }


    private val service: SdkService = getSdkInstance()

    @Throws(ConnectionTokenException::class)
    internal fun createConnectionToken(rid: String): ConnectionToken {
        try {
            val result = service.getConnectionToken(rid).execute()
            if (result.isSuccessful && result.body() != null && result.body()!!.statusCode == 200) {
                return result.body()!!.body
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return ConnectionToken()
    }


    @Throws(Exception::class)
    internal fun createSdkPaymentIntent(
        body: JsonObject
    ): String? {
        try {
            val result =
                service.getPaymentIntent(body)
                    .execute()
            Log.d("TAG", "createPaymentIntent: ${Gson().toJson(result.body())}")
            if (result.isSuccessful && result.body() != null) {
                return result.body()!!.client_secret
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }


}
