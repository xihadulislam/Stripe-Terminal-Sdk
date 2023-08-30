package com.xihadulislam.stripeterminalsdk.pay.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class AppPref {


    public static final String STORE_PREF_NAME = "sdk_pref";
    private static SharedPreferences pref = null;
    private static AppPref preferences = null;
    String TAG = "StorePref";

    private AppPref(Context context) {
        pref = context.getSharedPreferences(STORE_PREF_NAME, Context.MODE_PRIVATE);
    }

    public static AppPref getInstance(Context context) {
        if (preferences == null) {
            preferences = new AppPref(context.getApplicationContext());
        }
        return preferences;
    }

    public Integer getLastConnectedSdk() {
        return pref.getInt("tLastConnectedSdk", 0);
    }

    public void setLastConnectedSdk(Integer stripe_terminal_reader) {
        pref.edit().putInt("tLastConnectedSdk", stripe_terminal_reader).apply();
    }

    public String getStripeTerminalReaderLocation() {
        return pref.getString("STRIPE_TERMINAL_READER_LOCATION", "");
    }

    public void setStripeTerminalReaderLocation(String stripe_terminal_reader) {
        pref.edit().putString("STRIPE_TERMINAL_READER_LOCATION", stripe_terminal_reader).apply();
    }

    public String getResId() {
        return pref.getString("RES_ID", "");
    }

    public void setResId(String resId) {
        pref.edit().putString("RES_ID", resId).apply();
    }


}