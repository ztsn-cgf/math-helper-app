package com.mathhelper.app.ui.student

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mathhelper.app.ai.AiConfig
import com.mathhelper.app.ai.AttributionService
import com.mathhelper.app.ai.DeepSeekClient
import com.mathhelper.app.data.local.AppDatabase
import com.mathhelper.app.data.local.entity.AttemptEntity
import com.mathhelper.app.data.local.entity.ExplanationEntity
import com.mathhelper.app.data.local.entity.MasteryEntity
import com.mathhelper.app.data.local.entity.MisconceptionEntity
import com.mathhelper.app.data.local.entity.PracticeQuestionEntity
import com.mathhelper.app.data.local.entity.ReferenceMaterialEntity
import com.mathhelper.app.ui.common.BackButton
import com.mathhelper.app.ui.common.BigActionButton
import com.mathhelper.app.ui.theme.StudentTheme
import com.mathhelper.app.util.Encouragement
import com.mathhelper.app.util.RewardStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** 答对时的一次奖励事件，用于 UI 展示鼓励语 + 触发撒花。 */
data class RewardEvent(val stars: Int, val phrase: String, val nonce: Int)

private enum class LearnStep { LEARN, PRACTICE }

class LearnViewModel(app: Application, val knowledgePointId: String) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val kpId = knowledgePointId
    private val rewardStore = RewardStore.getInstance(app)

    val kpName = db.knowledgePointDao().observeById(kpId)
        .map { it?.name ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val explanation = db.explanationDao().observeForKnowledgePoint(kpId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val referenceMaterials = combine(
        db.knowledgePointDao().observeById(kpId),
        db.referenceMaterialDao().observeAll()
    ) { kp, all ->
        if (kp == null) emptyList()
        else all.filter { kp.name.contains(it.concept) || it.concept.contains(kp.name) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val misconceptions = combine(
        db.knowledgePointDao().observeById(kpId),
        db.misconceptionDao().observeAll()
    ) { kp, all ->
        if (kp == null) emptyList()
        else all.filter { m ->
            (kp.name.contains(m.concept) || m.concept.contains(kp.name)) ||
                m.knowledgeKeys.any { kp.name.contains(it) || it.contains(kp.name) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val questions = db.practiceQuestionDao().observeForKnowledgePoint(kpId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _feedbackMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val feedbackMap: StateFlow<Map<String, String>> = _feedbackMap

    private val _generating = MutableStateFlow<String?>(null)
    val generating: StateFlow<String?> = _generating

    private val _rewardEvent = MutableStateFlow<RewardEvent?>(null)
    val rewardEvent: StateFlow<RewardEvent?> = _rewardEvent
    private var nonce = 0

    fun clearReward() {
        _rewardEvent.value = null
    }

    fun generateQuestions(clearFirst: Boolean = false) {
        viewModelScope.launch {
            if (clearFirst) db.practiceQuestionDao().deleteForKnowledgePoint(kpId)
            val config = AiConfig.from(getApplication())
            if (!config.isConfigured) {
                _generating.value = "请先在「家长模式 → AI 设置」填 DeepSeek key"
                return@launch
            }
            _generating.value = "正在出题…"
            val name = db.knowledgePointDao().getById(kpId)?.name ?: kpId
            val service = AttributionService(DeepSeekClient(config), db.knowledgePointDao())
            val list = runCatching { service.generatePracticeQuestions(name) }
                .getOrDefault(emptyList())
            if (list.isEmpty()) {
                _generating.value = "出题失败，请重试"
                return@launch
            }
            db.practiceQuestionDao().insertAll(
                list.mapIndexed { i, q ->
                    PracticeQuestionEntity(
                        id = "q.$kpId.${UUID.randomUUID()}",
                        knowledgePointId = kpId,
                        content = q.question,
                        answer = q.answer,
                        options = q.options,
                        solution = q.solution
                    )
                }
            )
            _generating.value = null
        }
    }

    fun submitAnswer(question: PracticeQuestionEntity, userAnswer: String) {
        viewModelScope.launch {
            val correct = isCorrect(question.answer, userAnswer)
            db.attemptDao().insert(
                AttemptEntity(questionId = question.id, correct = correct, time = System.currentTimeMillis())
            )
            val prev = db.masteryDao().get(kpId)
            val wasWrongBefore = (prev?.wrongCount ?: 0) > 0
            val streak = if (correct) (prev?.correctStreak ?: 0) + 1 else 0
            val correctCount = (prev?.correctCount ?: 0) + if (correct) 1 else 0
            val wrongCount = (prev?.wrongCount ?: 0) + if (correct) 0 else 1
            val status = when {
                streak >= 3 -> "mastered"
                streak >= 2 -> "consolidating"
                else -> "weak"
            }
            // 间隔复习：错了马上复习；做对按 3 天、7 天逐步拉长；掌握后不再复习
            val day = 24L * 60 * 60 * 1000
            val nextReview = when {
                !correct -> 0L
                streak == 1 -> System.currentTimeMillis() + 3 * day
                streak == 2 -> System.currentTimeMillis() + 7 * day
                else -> Long.MAX_VALUE
            }
            db.masteryDao().upsert(MasteryEntity(kpId, status, streak, correctCount, wrongCount, nextReview, System.currentTimeMillis()))
            val msg = if (correct) {
                if (wasWrongBefore) {
                    val stars = rewardStore.addStars(2)
                    nonce++
                    _rewardEvent.value = RewardEvent(stars, "${rewardStore.childName.value}，以前错过的知识点这次做对了，太棒了！🎉", nonce)
                    "✅ 做对了！以前错过的，这次掌握啦！"
                } else {
                    val stars = rewardStore.addStar()
                    nonce++
                    _rewardEvent.value = RewardEvent(stars, Encouragement.random(rewardStore.childName.value), nonce)
                    "✅ 答对啦！"
                }
            } else {
                "❌ 再想想～看看下面的解题思路，再试一次"
            }
            _feedbackMap.value = _feedbackMap.value + (question.id to msg)
        }
    }

    private fun isCorrect(expected: String, actual: String): Boolean {
        val e = expected.trim().replace(" ", "")
        val a = actual.trim().replace(" ", "")
        if (e.isEmpty() || a.isEmpty()) return false
        // 完全一致（忽略大小写、空格）
        if (e.equals(a, ignoreCase = true)) return true
        // 数值比对：答案常是「数字 + 单位」形式（如 35 厘米），允许漏写单位；
        // 双方提取到的数值序列必须完全一致，避免空串/乱填被误判为对。
        val en = numbers(e)
        val an = numbers(a)
        return en.isNotEmpty() && en.size == an.size &&
            en.zip(an).all { (x, y) -> kotlin.math.abs(x - y) < 1e-6 }
    }

    private fun numbers(s: String): List<Double> =
        Regex("""-?\d+(?:\.\d+)?""").findAll(s).mapNotNull { it.value.toDoubleOrNull() }.toList()
}

class LearnViewModelFactory(
    private val app: Application,
    private val knowledgePointId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        LearnViewModel(app, knowledgePointId) as T
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    knowledgePointId: String,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as Application
    val vm: LearnViewModel = viewModel(factory = LearnViewModelFactory(app, knowledgePointId))

    val kpName by vm.kpName.collectAsState()
    val explanation by vm.explanation.collectAsState()
    val referenceMaterials by vm.referenceMaterials.collectAsState()
    val misconceptions by vm.misconceptions.collectAsState()
    val questions by vm.questions.collectAsState()
    val feedbackMap by vm.feedbackMap.collectAsState()
    val generating by vm.generating.collectAsState()
    val rewardEvent by vm.rewardEvent.collectAsState()

    var step by remember { mutableStateOf(LearnStep.LEARN) }
    var index by remember { mutableStateOf(0) }
    var answers by remember { mutableStateOf(mapOf<String, String>()) }

    val motionMode = when {
        knowledgePointId.endsWith(".trans") -> "trans"
        knowledgePointId.endsWith(".rotate") -> "rotate"
        knowledgePointId.endsWith(".sym") -> "sym"
        else -> null
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(kpName.ifBlank { "学习" }) },
                navigationIcon = { BackButton(onBack) }
            )
        }
    ) { padding ->
        StudentTheme {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                when (step) {
                    LearnStep.LEARN -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                            contentPadding = PaddingValues(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            motionMode?.let { mode ->
                                item {
                                    Card {
                                        Column(Modifier.padding(20.dp)) {
                                            Text("动一动看看", style = MaterialTheme.typography.titleMedium)
                                            MotionDemo(mode)
                                        }
                                    }
                                }
                            }

                            explanation?.let { ex ->
                                item { ExplanationCard(ex) }
                            }

                            if (referenceMaterials.isNotEmpty()) {
                                item {
                                    Text("量感锚点", style = MaterialTheme.typography.titleMedium)
                                }
                                items(referenceMaterials, key = { it.id }) { mat ->
                                    ReferenceMaterialCard(mat)
                                }
                            }

                            if (misconceptions.isNotEmpty()) {
                                item {
                                    Text("容易搞错的地方", style = MaterialTheme.typography.titleMedium)
                                }
                                items(misconceptions, key = { it.id }) { m ->
                                    MisconceptionCard(m)
                                }
                            }

                            item {
                                if (questions.isNotEmpty()) {
                                    BigActionButton(
                                        text = "开始练习 →",
                                        onClick = {
                                            index = 0
                                            step = LearnStep.PRACTICE
                                        }
                                    )
                                    TextButton(
                                        onClick = { vm.generateQuestions(clearFirst = true) },
                                        enabled = generating == null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (generating != null) "正在出题…" else "换一批新题")
                                    }
                                } else {
                                    val gen = generating
                                    if (gen != null) {
                                        Text(
                                            gen,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            "这里还没有练习题，先自动出几道：",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        BigActionButton(
                                            text = "出几道题练练",
                                            onClick = { vm.generateQuestions() }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    LearnStep.PRACTICE -> {
                        val q = questions.getOrNull(index)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 720.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (q == null) {
                                Text(
                                    "这里还没有练习题哦",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(onClick = { step = LearnStep.LEARN }) {
                                    Text("返回")
                                }
                            } else {
                                Text(
                                    "第 ${index + 1} / ${questions.size} 题",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                QuestionCard(
                                    question = q,
                                    answer = answers[q.id] ?: "",
                                    onAnswerChange = { answers = answers + (q.id to it) },
                                    onOptionSelect = { opt -> vm.submitAnswer(q, opt) },
                                    feedback = feedbackMap[q.id],
                                    onSubmit = { vm.submitAnswer(q, answers[q.id] ?: "") }
                                )
                                rewardEvent?.let { ev ->
                                    Text(
                                        ev.phrase,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.secondary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            if (index > 0) {
                                                index--
                                                vm.clearReward()
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.weight(1f).height(56.dp)
                                    ) { Text("上一题") }

                                    if (index < questions.size - 1) {
                                        Button(
                                            onClick = {
                                                index++
                                                vm.clearReward()
                                            },
                                            modifier = Modifier.weight(1f).height(56.dp)
                                        ) { Text("下一题") }
                                    } else {
                                        Button(
                                            onClick = {
                                                step = LearnStep.LEARN
                                                vm.clearReward()
                                            },
                                            modifier = Modifier.weight(1f).height(56.dp)
                                        ) { Text("完成 🎉") }
                                    }
                                }
                            }
                        }
                    }
                }

                rewardEvent?.let { ev -> StarBurst(trigger = ev.nonce) }
            }
        }
    }
}

@Composable
private fun ExplanationCard(explanation: ExplanationEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                explanation.title.ifBlank { "先听讲解" },
                style = MaterialTheme.typography.titleMedium
            )
            if (explanation.illustration.isNotBlank()) {
                VisualIllustration(explanation.illustration, explanation.knowledgePointId)
            }
            Text(
                explanation.content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ReferenceMaterialCard(material: ReferenceMaterialEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(material.unit, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(material.anchor, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
            if (material.detail.isNotBlank()) {
                Text(
                    material.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MisconceptionCard(misconception: MisconceptionEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(
                misconception.concept,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                "✗ 常见错误：${misconception.wrongIdea}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                "✓ 正确理解：${misconception.correctAnchor}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun QuestionCard(
    question: PracticeQuestionEntity,
    answer: String,
    onAnswerChange: (String) -> Unit,
    onOptionSelect: (String) -> Unit,
    feedback: String?,
    onSubmit: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(question.content, style = MaterialTheme.typography.titleMedium)
            if (question.options.isNotEmpty()) {
                // 选择题：点选项即作答，不显示自由输入框
                Column(
                    Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    question.options.forEach { opt ->
                        OutlinedButton(
                            onClick = { onOptionSelect(opt) },
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Text(opt, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = answer,
                    onValueChange = onAnswerChange,
                    label = { Text("答案", style = MaterialTheme.typography.bodyLarge) },
                    textStyle = MaterialTheme.typography.titleMedium,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                )
                Button(
                    onClick = onSubmit,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(56.dp)
                ) {
                    Text("检查", style = MaterialTheme.typography.titleMedium)
                }
            }
            feedback?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (it.startsWith("✅")) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            if (feedback != null && question.solution.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("解题思路", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Text(
                            question.solution,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
