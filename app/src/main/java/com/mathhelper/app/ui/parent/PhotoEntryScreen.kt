package com.mathhelper.app.ui.parent

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.mathhelper.app.ai.AttributionResult
import com.mathhelper.app.ai.AttributionService
import com.mathhelper.app.ai.DeepSeekClient
import com.mathhelper.app.data.local.AppDatabase
import com.mathhelper.app.data.local.entity.ExplanationEntity
import com.mathhelper.app.data.local.entity.KnowledgePointEntity
import com.mathhelper.app.data.local.entity.MasteryEntity
import com.mathhelper.app.data.local.entity.MistakeEntity
import com.mathhelper.app.data.local.entity.PracticeQuestionEntity
import com.mathhelper.app.ocr.MlKitOcrEngine
import com.mathhelper.app.ui.common.BackButton
import com.mathhelper.app.ui.common.BigActionButton
import com.mathhelper.app.util.ImageStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class PhotoEntryUiState(
    val processing: Boolean = false,
    val ocrText: String = "",
    val attribution: AttributionResult? = null,
    val error: String? = null,
    val aiConfigured: Boolean = false,
    val saved: Boolean = false
)

class PhotoEntryViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val knowledgePointDao = db.knowledgePointDao()
    private val mistakeDao = db.mistakeDao()
    private val explanationDao = db.explanationDao()
    private val practiceQuestionDao = db.practiceQuestionDao()
    private val masteryDao = db.masteryDao()
    private val ocr = MlKitOcrEngine()

    private val aiConfig = AiConfig.from(app)
    private val attributionService: AttributionService? =
        if (aiConfig.isConfigured) AttributionService(DeepSeekClient(aiConfig), knowledgePointDao)
        else null

    val topics = knowledgePointDao.observeTopics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _uiState = MutableStateFlow(PhotoEntryUiState(aiConfigured = aiConfig.isConfigured))
    val uiState: StateFlow<PhotoEntryUiState> = _uiState

    private var imagePath: String? = null

    fun onImage(context: Context, bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(processing = true, error = null, ocrText = "", attribution = null)
            imagePath = ImageStore.save(context, bitmap)
            val text = ocr.recognize(InputImage.fromBitmap(bitmap, 0))
            _uiState.value = _uiState.value.copy(processing = false, ocrText = text)

            if (attributionService != null && text.isNotBlank()) {
                runCatching { attributionService.attribute(text) }
                    .onSuccess { result -> _uiState.value = _uiState.value.copy(attribution = result) }
                    .onFailure { e -> _uiState.value = _uiState.value.copy(error = "AI 归因失败：${e.message}") }
            }
        }
    }

    fun updateOcrText(text: String) {
        _uiState.value = _uiState.value.copy(ocrText = text)
    }

    fun save(selectedKpId: String) {
        viewModelScope.launch {
            val attribution = _uiState.value.attribution
            val ocrText = _uiState.value.ocrText
            val kpId = selectedKpId.ifBlank { attribution?.knowledgePointId.orEmpty() }
            val mistakeId = UUID.randomUUID().toString()

            mistakeDao.insert(
                MistakeEntity(
                    id = mistakeId,
                    imagePath = imagePath,
                    ocrText = ocrText,
                    knowledgePointId = kpId.ifBlank { null },
                    mistakeType = attribution?.misconception ?: "",
                    createTime = System.currentTimeMillis()
                )
            )

            if (kpId.isNotBlank() && attribution != null) {
                if (attribution.explanation.isNotBlank()) {
                    explanationDao.insert(
                        ExplanationEntity(
                            id = "ai.$kpId",
                            knowledgePointId = kpId,
                            type = "text",
                            source = "ai",
                            title = attribution.knowledgePointName.ifBlank { kpId },
                            content = attribution.explanation,
                            mediaUrl = null,
                            durationSec = 0
                        )
                    )
                }
                val questions = attribution.practiceQuestions.mapIndexed { i, q ->
                    PracticeQuestionEntity(
                        id = "q.$kpId.$mistakeId.$i",
                        knowledgePointId = kpId,
                        content = q.question,
                        answer = q.answer,
                        options = q.options
                    )
                }
                if (questions.isNotEmpty()) practiceQuestionDao.insertAll(questions)
                masteryDao.upsert(MasteryEntity(kpId, "weak", 0, System.currentTimeMillis()))
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
    val topics by viewModel.topics.collectAsState()

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var manualKpId by remember { mutableStateOf<String?>(null) }
    var kpMenuExpanded by remember { mutableStateOf(false) }

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

    val effectiveKpId = manualKpId ?: uiState.attribution?.knowledgePointId
    val effectiveKpName = topics.firstOrNull { it.id == effectiveKpId }?.name
        ?: uiState.attribution?.knowledgePointName
        ?: "选择知识点"

    LaunchedEffect(uiState.saved) {
        if (uiState.saved) onBack()
    }

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
                        BigActionButton("拍照", onClick = { cameraLauncher.launch(null) })
                        BigActionButton(
                            "从相册选",
                            onClick = {
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                        if (!uiState.aiConfigured) {
                            Text(
                                "未配置 AI（secrets.properties），将跳过自动归因，可手动选知识点。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                    item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                }
                uiState.error?.let { err ->
                    item {
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                item {
                    OutlinedTextField(
                        value = uiState.ocrText,
                        onValueChange = { viewModel.updateOcrText(it) },
                        label = { Text("识别文字（可修改）") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Box {
                        OutlinedButton(onClick = { kpMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(effectiveKpName)
                        }
                        DropdownMenu(expanded = kpMenuExpanded, onDismissRequest = { kpMenuExpanded = false }) {
                            topics.forEach { t: KnowledgePointEntity ->
                                DropdownMenuItem(
                                    text = { Text(t.name) },
                                    onClick = { manualKpId = t.id; kpMenuExpanded = false }
                                )
                            }
                        }
                    }
                }
                uiState.attribution?.let { attr ->
                    if (attr.misconception.isNotBlank()) {
                        item {
                            Card {
                                Column(Modifier.padding(12.dp)) {
                                    Text("可能的误区", style = MaterialTheme.typography.titleMedium)
                                    Text(attr.misconception, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    if (attr.explanation.isNotBlank()) {
                        item {
                            Card {
                                Column(Modifier.padding(12.dp)) {
                                    Text("短讲解", style = MaterialTheme.typography.titleMedium)
                                    Text(attr.explanation, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
                item {
                    BigActionButton(
                        "保存错题",
                        onClick = { viewModel.save(effectiveKpId ?: "") }
                    )
                }
            }
        }
    }
}
