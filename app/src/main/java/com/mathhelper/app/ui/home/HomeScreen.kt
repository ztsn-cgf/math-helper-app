package com.mathhelper.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mathhelper.app.update.UpdateChecker
import com.mathhelper.app.update.UpdateInfo
import kotlinx.coroutines.launch

/**
 * 首页：家长 / 学生 模式选择。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onParent: () -> Unit,
    onStudent: () -> Unit,
    onKnowledgeTree: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<Int?>(null) }
    var checking by remember { mutableStateOf(false) }
    var updateMsg by remember { mutableStateOf<String?>(null) }

    fun checkUpdate() {
        scope.launch {
            checking = true
            updateMsg = null
            val info = UpdateChecker.latest()
            when {
                info == null -> updateMsg = "检查失败，请确认网络后重试"
                UpdateChecker.isNewer(info.version, UpdateChecker.currentVersion(context)) -> updateInfo = info
                else -> updateMsg = "已是最新版本 v${UpdateChecker.currentVersion(context)}"
            }
            checking = false
        }
    }

    LaunchedEffect(Unit) {
        val info = UpdateChecker.latest()
        if (info != null && UpdateChecker.isNewer(info.version, UpdateChecker.currentVersion(context))) {
            updateInfo = info
        }
    }

    Scaffold(
        topBar = { CenterAlignedTopAppBar(title = { Text("数学助手") }) }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = 560.dp).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    "家长拍错题 · 孩子学得会",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ModeButton("家长模式", onClick = onParent)
                ModeButton("学生模式", onClick = onStudent)
                TextButton(onClick = onKnowledgeTree) {
                    Text("查看知识点树")
                }
                TextButton(onClick = { checkUpdate() }, enabled = !checking) {
                    Text(if (checking) "检查中…" else "检查更新")
                }
                updateMsg?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { updateInfo = null },
            title = { Text("发现新版本 ${info.version}") },
            text = {
                Text(
                    when {
                        downloading && progress != null -> "正在下载… $progress%"
                        downloading -> "正在下载…"
                        else -> "是否下载并更新到最新版？"
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            downloading = true
                            progress = 0
                            val file = UpdateChecker.downloadApk(context, info.downloadUrl) { p ->
                                progress = p
                            }
                            if (file != null) {
                                UpdateChecker.installApk(context, file)
                            } else {
                                updateMsg = "下载失败，请检查网络后重试"
                            }
                            downloading = false
                            progress = null
                            updateInfo = null
                        }
                    },
                    enabled = !downloading
                ) { Text(if (downloading) "下载中…" else "更新") }
            },
            dismissButton = {
                TextButton(onClick = { updateInfo = null }) { Text("以后再说") }
            }
        )
    }
}

@Composable
private fun ModeButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
