package com.mathhelper.app.ai

interface AiClient {
    /** 发送 system + user 两条消息，返回助手的文本内容。 */
    suspend fun chat(system: String, user: String): String
}
