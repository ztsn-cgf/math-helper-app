package com.mathhelper.app.ui.student

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mathhelper.app.data.local.AppDatabase
import com.mathhelper.app.ui.common.BackButton
import com.mathhelper.app.ui.theme.StudentTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LevelRow(
    val id: String,
    val name: String,
    val passed: Boolean,
    val unlocked: Boolean
)

class LevelViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)

    /** 关卡列表：按知识点顺序，前一关过关（巩固中/已掌握）才解锁下一关。 */
    val levels: StateFlow<List<LevelRow>> = combine(
        db.knowledgePointDao().observeTopics(),
        db.masteryDao().observeAll()
    ) { topics, masteries ->
        val byKp = masteries.associateBy { it.knowledgePointId }
        var prevPassed = true
        topics.map { topic ->
            val m = byKp[topic.id]
            val passed = m?.status == "consolidating" || m?.status == "mastered"
            val unlocked = prevPassed
            prevPassed = passed
            LevelRow(topic.id, topic.name, passed, unlocked)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelScreen(
    onBack: () -> Unit,
    onLearn: (String) -> Unit,
    viewModel: LevelViewModel = viewModel()
) {
    val levels by viewModel.levels.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("闯关") },
                navigationIcon = { BackButton(onBack) }
            )
        }
    ) { padding ->
        StudentTheme {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(levels) { index, level ->
                    LevelCard(
                        index = index,
                        level = level,
                        onClick = { if (level.unlocked) onLearn(level.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LevelCard(index: Int, level: LevelRow, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${index + 1}",
                style = MaterialTheme.typography.titleLarge,
                color = when {
                    level.passed -> MaterialTheme.colorScheme.tertiary
                    level.unlocked -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline
                },
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(level.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            when {
                level.passed -> Text(
                    "✓ 已过关",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                level.unlocked -> Button(onClick = onClick) { Text("挑战") }
                else -> Text("🔒", fontSize = 20.sp)
            }
        }
    }
}
