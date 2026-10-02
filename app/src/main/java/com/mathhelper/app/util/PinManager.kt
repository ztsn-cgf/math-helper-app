package com.mathhelper.app.util

import android.content.Context
import android.content.SharedPreferences

/**
 * 家长模式 PIN 管理。默认 0000，可修改。
 */
class PinManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("parent_mode", Context.MODE_PRIVATE)

    fun getPin(): String = prefs.getString("pin", "0000") ?: "0000"

    fun setPin(pin: String) {
        prefs.edit().putString("pin", pin).apply()
    }

    fun verify(pin: String): Boolean = pin == getPin()
}
