package com.mathhelper.app.ui.home

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mathhelper.app.data.local.AppDatabase
import com.mathhelper.app.data.local.entity.KnowledgePointEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class DomainUi(
    val id: String,
    val name: String,
    val areas: List<AreaUi>
)

data class AreaUi(
    val id: String,
    val name: String,
    val gradeNote: String,
    val topics: List<TopicUi>
)

data class TopicUi(
    val id: String,
    val name: String,
    val tags: List<String>
)

class KnowledgeTreeViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)

    val domains: StateFlow<List<DomainUi>> = db.knowledgePointDao().observeAll()
        .map { buildTree(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val misconceptionCount: StateFlow<Int> = db.misconceptionDao().observeAll()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val referenceCount: StateFlow<Int> = db.referenceMaterialDao().observeAll()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private fun buildTree(points: List<KnowledgePointEntity>): List<DomainUi> {
        val domains = points.filter { it.type == "domain" }
        val areas = points.filter { it.type == "area" }
        val topics = points.filter { it.type == "topic" }
        return domains.map { domain ->
            val domainAreas = areas.filter { it.parentId == domain.id }.map { area ->
                val areaTopics = topics.filter { it.parentId == area.id }.map { topic ->
                    TopicUi(topic.id, topic.name, topic.tags)
                }
                AreaUi(area.id, area.name, area.gradeNote, areaTopics)
            }
            DomainUi(domain.id, domain.name, domainAreas)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeTreeScreen(
    onBack: () -> Unit,
    viewModel: KnowledgeTreeViewModel = viewModel()
) {
    val domains by viewModel.domains.collectAsState()
    val miscCount by viewModel.misconceptionCount.collectAsState()
    val refCount by viewModel.referenceCount.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("知识点树") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().widthIn(max = 840.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { SummaryCard(domains, miscCount, refCount) }
                items(domains, key = { it.id }) { domain ->
                    DomainCard(domain)
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    domains: List<DomainUi>,
    miscCount: Int,
    refCount: Int
) {
    val topicCount = domains.sumOf { d -> d.areas.sumOf { it.topics.size } }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("地基数据已就绪", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "领域 ${domains.size} · 主题 ${domains.sumOf { it.areas.size }} · " +
                    "知识点 $topicCount · 误区 $miscCount · 参照物 $refCount",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun DomainCard(domain: DomainUi) {
    var expanded by remember { mutableStateOf(true) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    domain.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (expanded) "收起" else "展开",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                domain.areas.forEach { area -> AreaItem(area) }
            }
        }
    }
}

@Composable
private fun AreaItem(area: AreaUi) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(
            area.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
        if (area.gradeNote.isNotBlank()) {
            Text(
                area.gradeNote,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        area.topics.forEach { topic ->
            Text(
                "· ${topic.name}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp)
            )
        }
    }
}
