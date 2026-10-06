package com.mathhelper.app.ui.parent

import android.app.Application
import android.content.Context
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.content.ContextCompat
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.common.InputImage
import com.mathhelper.app.ai.AiConfig
import com.mathhelper.app.ai.AttributionService
import com.mathhelper.app.ai.DeepSeekClient
import com.mathhelper.app.ai.QuestionAnalysis
import com.mathhelper.app.data.local.AppDatabase
import com.mathhelper.app.data.local.entity.MasteryEntity
import com.mathhelper.app.data.local.entity.MistakeEntity
import com.mathhelper.app.ocr.MlKitOcrEngine
import com.mathhelper.app.ui.common.BackButton
import com.mathhelper.app.ui.common.BigActionButton
import com.mathhelper.app.util.ImageStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class PhotoEntryUiState(
    val processing: Boolean = false,
    val analyzing: Boolean = false,
    val ocrText: String = "",
    val questions: List<QuestionAnalysis> = emptyList(),
    val error: String? = null,
    val aiConfigured: Boolean = false,
    val saved: Boolean = false
)

class PhotoEntryViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val mistakeDao = db.mistakeDao()
    private val masteryDao = db.masteryDao()
    private val ocr = MlKitOcrEngine()

    private val aiConfig = AiConfig.from(app)
    private val attributionService: AttributionService? =
        if (aiConfig.isConfigured) AttributionService(DeepSeekClient(aiConfig), db.knowledgePointDao())
        else null

    private val _uiState = MutableStateFlow(PhotoEntryUiState(aiConfigured = aiConfig.isConfigured))
    val uiState: StateFlow<PhotoEntryUiState> = _uiState

    private var imagePath: String? = null

    fun onImage(context: Context, bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(processing = true, analyzing = false, error = null, ocrText = "", questions = emptyList())
            imagePath = ImageStore.save(context, bitmap)
            val text = ocr.recognize(InputImage.fromBitmap(bitmap, 0))
            _uiState.value = _uiState.value.copy(processing = false, ocrText = text)

            if (attributionService != null && text.isNotBlank()) {
                _uiState.value = _uiState.value.copy(analyzing = true)
                runCatching { attributionService.analyzePage(text) }
                    .onSuccess { qs -> _uiState.value = _uiState.value.copy(questions = qs, analyzing = false) }
                    .onFailure { e -> _uiState.value = _uiState.value.copy(error = "AI 归因失败：${e.message}", analyzing = false) }
            }
        }
    }

    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(error = message)
    }

    /** 保存勾选为「做错」的题目，每题一条错题记录。 */
    fun save(wrongQuestions: List<QuestionAnalysis>) {
        viewModelScope.launch {
            val ocrText = _uiState.value.ocrText
            wrongQuestions.forEach { q ->
                val mistakeId = UUID.randomUUID().toString()
                mistakeDao.insert(
                    MistakeEntity(
                        id = mistakeId,
                        imagePath = imagePath,
                        ocrText = q.question.ifBlank { ocrText },
                        knowledgePointId = q.knowledgePointId.ifBlank { null },
                        mistakeType = q.misconception,
                        severity = q.severity.ifBlank { "high" },
                        createTime = System.currentTimeMillis()
                    )
                )
                // 高等级 → 重点巩固（weak）；低等级（涂改后对）只记错题，不强制变弱
                if (q.knowledgePointId.isNotBlank() && q.severity != "low") {
                    masteryDao.upsert(MasteryEntity(q.knowledgePointId, "weak", 0, 0, 0, 0, System.currentTimeMillis()))
                }
            }
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoEntryScreen(
    onBack: () -> Unit,
    viewModel: PhotoEntryViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var questions by remember { mutableStateOf<List<QuestionAnalysis>>(emptyList()) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            previewBitmap = bitmap
            viewModel.onImage(context, bitmap)
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val bmp = ImageStore.load(context, uri)
            if (bmp != null) {
                previewBitmap = bmp
                viewModel.onImage(context, bmp)
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            runCatching { cameraLauncher.launch(null) }
                .onFailure { viewModel.setError("无法打开相机，请改用「从相册选」") }
        } else {
            viewModel.setError("未授予相机权限，请改用「从相册选」，或在系统设置里允许相机权限")
        }
    }

    LaunchedEffect(uiState.questions) {
        questions = uiState.questions
    }
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onBack()
    }

    val wrongCount = questions.count { it.isWrong }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("录入错题") },
                navigationIcon = { BackButton(onBack) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (previewBitmap == null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BigActionButton(
                            "拍照",
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                    runCatching { cameraLauncher.launch(null) }
                                        .onFailure { viewModel.setError("无法打开相机，请改用「从相册选」") }
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                        )
                        BigActionButton(
                            "从相册选",
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                        Text(
                            "可以拍一整页作业/试卷，AI 会自动拆分成题目、判断对错，只把做错的存成错题。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!uiState.aiConfigured) {
                            Text(
                                "未配置 AI（secrets.properties），将跳过自动识别。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        uiState.error?.let { err ->
                            Text(err, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            } else {
                item {
                    previewBitmap?.asImageBitmap()?.let { bmp ->
                        Image(
                            bitmap = bmp,
                            contentDescription = "错题图片",
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                if (uiState.processing) {
                    item {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(
                            "正在识别文字…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (uiState.analyzing) {
                    item {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(
                            "AI 正在拆分题目、判断对错…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                uiState.error?.let { err ->
                    item {
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (questions.isNotEmpty()) {
                    item {
                        Text(
                            "识别出 ${questions.size} 道题，勾选做错的（$wrongCount 道）；点徽标可切「重点/轻微」",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    itemsIndexed(questions) { index, q ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = q.isWrong,
                                    onCheckedChange = {
                                        questions = questions.mapIndexed { i, item ->
                                            if (i == index) item.copy(isWrong = it) else item
                                        }
                                    }
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(q.question, style = MaterialTheme.typography.bodyLarge)
                                    if (q.isWrong) {
                                        val high = q.severity != "low"
                                        Text(
                                            text = if (high) "🔴 重点巩固 · ${q.mark.ifBlank { "做错" }}" else "🟡 轻微 · 后续加练 · ${q.mark.ifBlank { "涂改后对" }}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (high) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.clickable {
                                                questions = questions.mapIndexed { i, item ->
                                                    if (i == index) item.copy(severity = if (high) "low" else "high") else item
                                                }
                                            }
                                        )
                                    }
                                    if (q.isWrong && q.knowledgePointName.isNotBlank()) {
                                        Text(
                                            "知识点：${q.knowledgePointName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (!uiState.processing && !uiState.analyzing) {
                    item {
                        Text(
                            "没有识别出题目，请重拍或换一张更清晰的照片。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (questions.isNotEmpty()) {
                    item {
                        BigActionButton(
                            "保存 $wrongCount 道错题",
                            onClick = { viewModel.save(questions.filter { it.isWrong }) }
                        )
                    }
                }
            }
        }
    }
}
