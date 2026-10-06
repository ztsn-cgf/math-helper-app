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

    /** 家长模式解锁：默认 10 分钟内免再次输入 PIN。 */
    fun unlock(minutes: Long = 10) {
        prefs.edit().putLong("unlock_until", System.currentTimeMillis() + minutes * 60_000).apply()
    }

    fun isUnlocked(): Boolean = System.currentTimeMillis() < prefs.getLong("unlock_until", 0)

    fun clearUnlock() {
        prefs.edit().putLong("unlock_until", 0).apply()
    }
}
