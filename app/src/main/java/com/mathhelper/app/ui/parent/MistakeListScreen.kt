package com.mathhelper.app.ui.parent

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
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MistakeUi(
    val id: String,
    val knowledgePointName: String,
    val ocrText: String,
    val mistakeType: String,
    val createTime: Long
)

class MistakeListViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)

    val mistakes: StateFlow<List<MistakeUi>> = combine(
        db.mistakeDao().observeAll(),
        db.knowledgePointDao().observeTopics()
    ) { ms, topics ->
        val nameById = topics.associateBy { it.id }
        ms.map { m ->
            MistakeUi(
                id = m.id,
                knowledgePointName = m.knowledgePointId?.let { nameById[it]?.name } ?: "未归类",
                ocrText = m.ocrText,
                mistakeType = m.mistakeType,
                createTime = m.createTime
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(id: String) {
        viewModelScope.launch { db.mistakeDao().deleteById(id) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MistakeListScreen(
    onBack: () -> Unit,
    viewModel: MistakeListViewModel = viewModel()
) {
    val mistakes by viewModel.mistakes.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("错题列表") },
                navigationIcon = { BackButton(onBack) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (mistakes.isEmpty()) {
                Text(
                    "还没有错题，去「录入错题」拍一张吧",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().widthIn(max = 840.dp),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(mistakes, key = { it.id }) { mistake ->
                        MistakeCard(mistake, onDelete = { viewModel.delete(mistake.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MistakeCard(mistake: MistakeUi, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    mistake.knowledgePointName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    formatTime(mistake.createTime),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (mistake.mistakeType.isNotBlank()) {
                Text(
                    "误区：${mistake.mistakeType}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (mistake.ocrText.isNotBlank()) {
                Text(
                    mistake.ocrText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            TextButton(onClick = onDelete) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
