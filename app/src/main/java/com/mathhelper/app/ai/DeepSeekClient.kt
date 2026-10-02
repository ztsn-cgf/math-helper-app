package com.mathhelper.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * DeepSeek（OpenAI 兼容）实现。用 HttpURLConnection，避免额外引入网络库。
 */
class DeepSeekClient(private val config: AiConfig) : AiClient {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun chat(system: String, user: String): String = withContext(Dispatchers.IO) {
        require(config.isConfigured) { "未配置 AI API key" }

        val request = ChatCompletionRequest(
            model = config.model,
            messages = listOf(
                ChatMessage(role = "system", content = system),
                ChatMessage(role = "user", content = user)
            )
        )
        val body = json.encodeToString(request)

        val url = URL(config.baseUrl.trimEnd('/') + "/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 30_000
            conn.readTimeout = 60_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Authorization", "Bearer ${config.apiKey}")

            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                throw IllegalStateException("AI 请求失败（$code）：$responseText")
            }
            val resp = json.decodeFromString<ChatCompletionResponse>(responseText)
            resp.choices.firstOrNull()?.message?.content ?: ""
        } finally {
            conn.disconnect()
        }
    }
}
