package com.mathhelper.app.ui.parent

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mathhelper.app.ai.AiConfig
import com.mathhelper.app.ui.common.BackButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf(AiConfig.currentApiKey(context)) }
    var saved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AI 设置") },
                navigationIcon = { BackButton(onBack) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                "填好 DeepSeek API Key 后，录入错题时会自动识别知识点、自动出同类题，不用再手动选。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it; saved = false },
                label = { Text("DeepSeek API Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )
            Button(
                onClick = { AiConfig.saveApiKey(context, apiKey); saved = true },
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("保存")
            }
            if (saved) {
                Text(
                    "已保存 ✅",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "怎么获取 Key：浏览器打开 platform.deepseek.com → 注册 → 充值（几块钱就够）→ 左侧「API Keys」→ 创建，复制 sk- 开头的一串填进来。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
