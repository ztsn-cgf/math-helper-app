package com.mathhelper.app.ui.parent

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mathhelper.app.data.local.AppDatabase
import com.mathhelper.app.ui.common.BackButton
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class MasteryRow(val name: String, val status: String, val correct: Int, val wrong: Int)

class MasteryMapViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)

    val rows: StateFlow<List<MasteryRow>> = combine(
        db.knowledgePointDao().observeTopics(),
        db.masteryDao().observeAll()
    ) { topics, masteries ->
        val byKp = masteries.associateBy { it.knowledgePointId }
        topics.map { t ->
            val m = byKp[t.id]
            MasteryRow(t.name, m?.status ?: "new", m?.correctCount ?: 0, m?.wrongCount ?: 0)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasteryMapScreen(
    onBack: () -> Unit,
    viewModel: MasteryMapViewModel = viewModel()
) {
    val rows by viewModel.rows.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("掌握度地图") },
                navigationIcon = { BackButton(onBack) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().widthIn(max = 840.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(rows) { row ->
                    MasteryRowCard(row)
                }
            }
        }
    }
}

@Composable
private fun MasteryRowCard(row: MasteryRow) {
    val (label, color) = statusUi(row.status)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(row.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            if (row.correct > 0 || row.wrong > 0) {
                Text(
                    "✓ ${row.correct}  ✗ ${row.wrong}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
            Box(
                Modifier
                    .background(color, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
    }
}

private fun statusUi(status: String): Pair<String, Color> = when (status) {
    "weak" -> "薄弱" to Color(0xFFE53935)
    "consolidating" -> "巩固中" to Color(0xFFFB8C00)
    "mastered" -> "已掌握" to Color(0xFF43A047)
    else -> "新学" to Color(0xFF9E9E9E)
}
