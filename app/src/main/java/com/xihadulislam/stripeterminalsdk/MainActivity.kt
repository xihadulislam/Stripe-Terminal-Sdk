package com.xihadulislam.stripeterminalsdk

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.TerminalException
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler
import com.xihadulislam.stripeterminalsdk.pay.ext.SdkHandler.Companion.WISE_POS_E_REQUEST_CODE
import com.xihadulislam.stripeterminalsdk.pay.listeners.WisePosEventListener
import com.xihadulislam.stripeterminalsdk.pay.models.TerminalRequest
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    lateinit var btnPay: Button

    private var sdkHandler: SdkHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnPay = findViewById(R.id.btnPay)

        btnPay.setOnClickListener {
            startPayment()
        }
        checkAllPermission()
    }

    private fun startPayment() {
        sdkHandler?.processSdkPayment(WISE_POS_E_REQUEST_CODE, true, getPayload(), object : WisePosEventListener {
            override fun onProgress(event: String, status: String) {
                Log.d(TAG, "onProgress: $event  -> $status")
            }

            override fun onSuccess(paymentIntent: PaymentIntent) {
                Log.d(TAG, "onSuccess: call")
            }

            override fun onFailure(event: String, msg: String, e: TerminalException?) {
                Log.d(TAG, "onFailure: $event -> $msg")
                e?.let { }
            }

            override fun onCancel(event: String, msg: String) {
                Log.d(TAG, "onCancel: $event  -> $msg")
            }
        })
    }


    var cnt = 0
    private fun checkAllPermission() {
        Log.d("TAG", "checkAllPermission: call ")

        if (cnt > 15) {
            return
        } else cnt++

        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 111
            )
        } else {
            sdkHandler = SdkHandler.getInstance(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        checkAllPermission()
    }


    private fun getPayload(): JSONObject {
        return JSONObject(Gson().toJson(TerminalRequest().apply {
            amount = 2000
        }))
    }

}