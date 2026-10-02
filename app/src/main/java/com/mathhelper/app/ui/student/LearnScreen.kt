package com.mathhelper.app.ui.student

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mathhelper.app.data.local.AppDatabase
import com.mathhelper.app.data.local.entity.AttemptEntity
import com.mathhelper.app.data.local.entity.ExplanationEntity
import com.mathhelper.app.data.local.entity.MasteryEntity
import com.mathhelper.app.data.local.entity.MisconceptionEntity
import com.mathhelper.app.data.local.entity.PracticeQuestionEntity
import com.mathhelper.app.data.local.entity.ReferenceMaterialEntity
import com.mathhelper.app.ui.common.BackButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LearnViewModel(app: Application, val knowledgePointId: String) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val kpId = knowledgePointId

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

    fun submitAnswer(question: PracticeQuestionEntity, userAnswer: String) {
        viewModelScope.launch {
            val correct = isCorrect(question.answer, userAnswer)
            db.attemptDao().insert(
                AttemptEntity(questionId = question.id, correct = correct, time = System.currentTimeMillis())
            )
            val prev = db.masteryDao().get(kpId)
            val streak = if (correct) (prev?.correctStreak ?: 0) + 1 else 0
            val status = when {
                streak >= 3 -> "mastered"
                streak >= 2 -> "consolidating"
                else -> "weak"
            }
            db.masteryDao().upsert(MasteryEntity(kpId, status, streak, System.currentTimeMillis()))
            val msg = if (correct) "✅ 答对啦！" else "❌ 再想想～正确答案：${question.answer}"
            _feedbackMap.value = _feedbackMap.value + (question.id to msg)
        }
    }

    private fun isCorrect(expected: String, actual: String): Boolean {
        val e = expected.trim().replace(" ", "")
        val a = actual.trim().replace(" ", "")
        if (e.isEmpty()) return false
        return e.equals(a, ignoreCase = true) || a.contains(e) || e.contains(a)
    }
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

    var answers by remember { mutableStateOf(mapOf<String, String>()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(kpName.ifBlank { "学习" }) },
                navigationIcon = { BackButton(onBack) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                explanation?.let { ex ->
                    item {
                        ExplanationCard(ex)
                    }
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
                    Text("练一练", style = MaterialTheme.typography.titleMedium)
                }

                if (questions.isEmpty()) {
                    item {
                        Text(
                            "还没有同类题，让家长再录入几道错题吧",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(questions, key = { it.id }) { q ->
                    QuestionCard(
                        question = q,
                        answer = answers[q.id] ?: "",
                        onAnswerChange = { answers = answers + (q.id to it) },
                        feedback = feedbackMap[q.id],
                        onSubmit = { vm.submitAnswer(q, answers[q.id] ?: "") }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExplanationCard(explanation: ExplanationEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("先听讲解", style = MaterialTheme.typography.titleMedium)
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
        Column(Modifier.padding(16.dp)) {
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
        Column(Modifier.padding(16.dp)) {
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
    feedback: String?,
    onSubmit: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(question.content, style = MaterialTheme.typography.bodyLarge)
            if (question.options.isNotEmpty()) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    question.options.forEach { opt ->
                        OutlinedButton(onClick = { onAnswerChange(opt) }, modifier = Modifier.fillMaxWidth()) {
                            Text(opt)
                        }
                    }
                }
            }
            OutlinedTextField(
                value = answer,
                onValueChange = onAnswerChange,
                label = { Text("答案") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            Button(onClick = onSubmit, modifier = Modifier.padding(top = 8.dp)) {
                Text("检查")
            }
            feedback?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (it.startsWith("✅")) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}
