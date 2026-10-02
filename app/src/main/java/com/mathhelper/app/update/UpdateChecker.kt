package com.mathhelper.app.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val downloadUrl: String)

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String = "",
    val assets: List<GithubAsset> = emptyList()
)

@Serializable
data class GithubAsset(
    @SerialName("browser_download_url") val browserDownloadUrl: String = ""
)

/**
 * 检查更新：查 GitHub 最新 Release，比较版本，下载 APK 并触发安装。
 * 仓库已公开，无需登录/令牌。
 */
object UpdateChecker {

    private const val API = "https://api.github.com/repos/ztsn-cgf/math-helper-app/releases/latest"
    // GitHub 直连在国内较慢，下载 APK 时优先走加速镜像，逐个尝试，全失败再回退直连
    private val MIRRORS = listOf(
        "https://ghproxy.net/",
        "https://gh-proxy.com/",
        "https://ghfast.top/"
    )
    private val json = Json { ignoreUnknownKeys = true }

    fun currentVersion(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"

    /** 最新版本信息；网络失败返回 null。 */
    suspend fun latest(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(API).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "GET"
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                val release = json.decodeFromString<GithubRelease>(text)
                val url = release.assets.firstOrNull()?.browserDownloadUrl ?: return@runCatching null
                UpdateInfo(release.tagName, url)
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    /** latest 是否比 current 新（支持 "v0.5.0" / "0.4.0" 两种格式）。 */
    fun isNewer(latest: String, current: String): Boolean {
        val l = latest.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val c = current.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(l.size, c.size)) {
            val a = l.getOrElse(i) { 0 }
            val b = c.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    suspend fun downloadApk(
        context: Context,
        url: String,
        onProgress: ((Int) -> Unit)? = null
    ): File? = withContext(Dispatchers.IO) {
        val candidates = MIRRORS.map { it + url } + url
        for (candidate in candidates) {
            val file = downloadOne(context, candidate, onProgress)
            if (file != null) return@withContext file
        }
        null
    }

    private fun downloadOne(context: Context, url: String, onProgress: ((Int) -> Unit)?): File? =
        runCatching {
            val file = File(context.cacheDir, "update.apk")
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 15_000
                conn.readTimeout = 120_000
                val total = conn.contentLengthLong
                conn.inputStream.use { input ->
                    file.outputStream().use { output ->
                        val buf = ByteArray(64 * 1024)
                        var read = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n == -1) break
                            output.write(buf, 0, n)
                            read += n
                            if (total > 0) onProgress?.invoke(((read * 100) / total).toInt())
                        }
                    }
                }
                file
            } finally {
                conn.disconnect()
            }
        }.getOrNull()

    fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
