package com.mathhelper.app.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 奖励存储：星星当「钱」用，攒够可兑换奖励（家长 PIN 兑换，扣减星星）。
 * 可多次兑换同一类奖励。SharedPreferences 本地保存。
 */
class RewardStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("reward", Context.MODE_PRIVATE)
    private val pinManager = PinManager(context)
    private val json = Json { ignoreUnknownKeys = true }

    private val _stars = MutableStateFlow(prefs.getInt(KEY_STARS, 0))
    val stars: StateFlow<Int> = _stars

    private val _childName = MutableStateFlow(prefs.getString(KEY_NAME, DEFAULT_NAME) ?: DEFAULT_NAME)
    val childName: StateFlow<String> = _childName

    private val _redeemedCounts = MutableStateFlow(loadCounts())
    val redeemedCounts: StateFlow<Map<String, Int>> = _redeemedCounts

    data class RewardTier(val cost: Int, val emoji: String, val reward: String)

    /** 奖励列表：cost = 兑换所需星星数。 */
    val rewards = listOf(
        RewardTier(10, "🍭", "一根棒棒糖"),
        RewardTier(20, "🍦", "一个冰淇淋"),
        RewardTier(30, "🧋", "一杯奶茶"),
        RewardTier(50, "🍗", "吃一次 KFC"),
        RewardTier(80, "🎬", "看一场电影"),
        RewardTier(120, "✨", "许愿一次（自己提一个小要求）")
    )

    /** 答对一题加 1 颗星，返回最新总数。 */
    fun addStar(): Int {
        val n = _stars.value + 1
        _stars.value = n
        prefs.edit().putInt(KEY_STARS, n).apply()
        return n
    }

    /** 加 n 颗星，返回最新总数。 */
    fun addStars(count: Int): Int {
        val n = _stars.value + count
        _stars.value = n
        prefs.edit().putInt(KEY_STARS, n).apply()
        return n
    }

    /** 设置孩子称呼（空则回退默认「宝贝」）。 */
    fun setChildName(name: String) {
        val v = name.trim().ifBlank { DEFAULT_NAME }
        _childName.value = v
        prefs.edit().putString(KEY_NAME, v).apply()
    }

    /** 该奖励已兑换次数。 */
    fun redeemedCount(tier: RewardTier): Int = _redeemedCounts.value[tier.reward] ?: 0

    /** 家长 PIN 校验 + 星星足够则兑换：扣减星星、记录次数。 */
    fun redeem(tier: RewardTier, pin: String): Boolean {
        if (!pinManager.verify(pin)) return false
        if (_stars.value < tier.cost) return false
        _stars.value -= tier.cost
        prefs.edit().putInt(KEY_STARS, _stars.value).apply()
        val updated = _redeemedCounts.value.toMutableMap().apply {
            put(tier.reward, (this[tier.reward] ?: 0) + 1)
        }
        _redeemedCounts.value = updated
        prefs.edit().putString(KEY_COUNTS, json.encodeToString(updated)).apply()
        return true
    }

    private fun loadCounts(): Map<String, Int> =
        prefs.getString(KEY_COUNTS, null)?.let {
            runCatching { json.decodeFromString<Map<String, Int>>(it) }.getOrDefault(emptyMap())
        } ?: emptyMap()

    companion object {
        private const val KEY_STARS = "total_stars"
        private const val KEY_NAME = "child_name"
        private const val KEY_COUNTS = "redeemed_counts"
        const val DEFAULT_NAME = "宝贝"
    }
}
