package com.xihadulislam.stripeterminalsdk.pay.utils

import android.content.Context
import android.net.ConnectivityManager
import needle.Needle
import java.io.IOException

object NetworkUtils {

    private fun isNetworkConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
    }

    fun isOnline(context: Context, block: (isConnected: Boolean) -> Unit) {

        Needle.onBackgroundThread().execute {

            if (isNetworkConnected(context)) {
                try {
                    block(true)
                 //   block(Runtime.getRuntime().exec("ping -c 1 google.com").waitFor() == 0)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                    block(false)
                } catch (e: IOException) {
                    e.printStackTrace()
                    block(false)
                }
            } else {
                block(false)
            }
        }
    }


}