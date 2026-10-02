package com.mathhelper.app.config

import android.content.Context
import java.util.Properties

/**
 * 从 assets/secrets.properties 读取本地配置（该文件已 gitignore）。
 * 内容示例：
 *   deepseek.api_key=sk-xxx
 *   deepseek.base_url=https://api.deepseek.com
 *   deepseek.model=deepseek-chat
 */
object Secrets {

    fun load(context: Context): Properties {
        val props = Properties()
        runCatching {
            context.assets.open("secrets.properties").use { input ->
                props.load(input)
            }
        }
        return props
    }
}
