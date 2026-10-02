package com.mathhelper.app.ai

import android.content.Context
import com.mathhelper.app.config.Secrets

/**
 * AI 服务商配置。优先读用户在本机「AI 设置」里填的 key（SharedPreferences），
 * 否则回退到 assets/secrets.properties。默认 DeepSeek，key 不写死（硬约束第 3 条）。
 */
data class AiConfig(
    val apiKey: String,
    val baseUrl: String,
    val model: String
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()

    companion object {
        private const val PREFS = "ai_config"
        private const val KEY_API = "api_key"

        fun from(context: Context): AiConfig {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val prefKey = prefs.getString(KEY_API, "")?.trim().orEmpty()
            val p = Secrets.load(context)
            return AiConfig(
                apiKey = prefKey.ifBlank { p.getProperty("deepseek.api_key", "").trim() },
                baseUrl = p.getProperty("deepseek.base_url", "https://api.deepseek.com").trim(),
                model = p.getProperty("deepseek.model", "deepseek-chat").trim()
            )
        }

        fun currentApiKey(context: Context): String =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_API, "") ?: ""

        fun saveApiKey(context: Context, apiKey: String) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_API, apiKey.trim()).apply()
        }
    }
}
