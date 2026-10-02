package com.mathhelper.app.ai

import android.content.Context
import com.mathhelper.app.config.Secrets

/**
 * AI 服务商配置。默认 DeepSeek，做成可配置项，key 不写死（硬约束第 3 条）。
 */
data class AiConfig(
    val apiKey: String,
    val baseUrl: String,
    val model: String
) {
    val isConfigured: Boolean get() = apiKey.isNotBlank()

    companion object {
        fun from(context: Context): AiConfig {
            val p = Secrets.load(context)
            return AiConfig(
                apiKey = p.getProperty("deepseek.api_key", "").trim(),
                baseUrl = p.getProperty("deepseek.base_url", "https://api.deepseek.com").trim(),
                model = p.getProperty("deepseek.model", "deepseek-chat").trim()
            )
        }
    }
}
