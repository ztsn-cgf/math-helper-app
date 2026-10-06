package com.mathhelper.app.ui.parent

import android.app.Application
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mathhelper.app.data.local.AppDatabase
import com.mathhelper.app.data.local.entity.AttemptEntity
import com.mathhelper.app.data.local.entity.KnowledgePointEntity
import com.mathhelper.app.data.local.entity.MasteryEntity
import com.mathhelper.app.data.local.entity.MistakeEntity
import com.mathhelper.app.ui.common.BackButton
import com.mathhelper.app.util.RewardStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class DailyStat(val dayLabel: String, val correct: Int, val wrong: Int)
data class DomainMistake(val name: String, val count: Int)
data class Redemption(val emoji: String, val name: String, val count: Int)

data class ReportUiState(
    val totalAttempts: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val totalMistakes: Int = 0,
    val stars: Int = 0,
    val masteredCount: Int = 0,
    val consolidatingCount: Int = 0,
    val weakCount: Int = 0,
    val daily: List<DailyStat> = emptyList(),
    val domainMistakes: List<DomainMistake> = emptyList(),
    val redemptions: List<Redemption> = emptyList()
) {
    val accuracy: Int? get() = if (totalAttempts == 0) null else (correctCount * 100 / totalAttempts)
}

class ReportViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.getInstance(app)
    private val rewardStore = RewardStore.getInstance(app)

    private data class DbBundle(
        val attempts: List<AttemptEntity>,
        val mistakes: List<MistakeEntity>,
        val masteries: List<MasteryEntity>,
        val kps: List<KnowledgePointEntity>
    )

    val state: StateFlow<ReportUiState> = combine(
        combine(
            db.attemptDao().observeAll(),
            db.mistakeDao().observeAll(),
            db.masteryDao().observeAll(),
            db.knowledgePointDao().observeAll()
        ) { attempts, mistakes, masteries, kps -> DbBundle(attempts, mistakes, masteries, kps) },
        rewardStore.stars,
        rewardStore.redeemedCounts
    ) { bundle, stars, redeemed ->
        val correct = bundle.attempts.count { it.correct }
        ReportUiState(
            totalAttempts = bundle.attempts.size,
            correctCount = correct,
            wrongCount = bundle.attempts.size - correct,
            totalMistakes = bundle.mistakes.size,
            stars = stars,
            masteredCount = bundle.masteries.count { it.status == "mastered" },
            consolidatingCount = bundle.masteries.count { it.status == "consolidating" },
            weakCount = bundle.masteries.count { it.status == "weak" },
            daily = buildDaily(bundle.attempts),
            domainMistakes = buildDomainMistakes(bundle.mistakes, bundle.kps),
            redemptions = buildRedemptions(redeemed)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportUiState())

    private fun buildDaily(attempts: List<AttemptEntity>): List<DailyStat> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = today.minusDays(13) // 近 14 天（含今天）
        val byDate = attempts.groupBy { Instant.ofEpochMilli(it.time).atZone(zone).toLocalDate() }
        return (0..13).map { i ->
            val d = start.plusDays(i.toLong())
            val list = byDate[d].orEmpty()
            DailyStat(
                dayLabel = "${d.monthValue}/${d.dayOfMonth}",
                correct = list.count { it.correct },
                wrong = list.count { !it.correct }
            )
        }
    }

    private fun buildDomainMistakes(
        mistakes: List<MistakeEntity>,
        kps: List<KnowledgePointEntity>
    ): List<DomainMistake> {
        val byId = kps.associateBy { it.id }
        fun domainOf(topicId: String): String {
            val topic = byId[topicId] ?: return "未归因"
            val area = topic.parentId?.let { byId[it] }
            val domain = area?.parentId?.let { byId[it] }
            return domain?.name ?: area?.name ?: topic.name
        }
        return mistakes.groupingBy { it.knowledgePointId?.let { id -> domainOf(id) } ?: "未归因" }
            .eachCount()
            .map { (name, count) -> DomainMistake(name, count) }
            .sortedByDescending { it.count }
            .take(6)
    }

    private fun buildRedemptions(redeemed: Map<String, Int>): List<Redemption> {
        val emojiByName = rewardStore.rewards.associate { it.reward to it.emoji }
        return redeemed.entries
            .map { (name, count) -> Redemption(emojiByName[name] ?: "🎁", name, count) }
            .sortedByDescending { it.count }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(onBack: () -> Unit, viewModel: ReportViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("学习报告") },
                navigationIcon = { BackButton(onBack) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { SummaryCard(state) }
            item { ProgressCard(state.daily) }
            item { DomainDistributionCard(state.domainMistakes) }
            item { RedemptionCard(state.redemptions) }
        }
    }
}

@Composable
private fun SummaryCard(state: ReportUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("总览", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                StatTile("累计答题", "${state.totalAttempts}", Modifier.weight(1f))
                StatTile("正确率", state.accuracy?.let { "$it%" } ?: "—", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                StatTile("错题数", "${state.totalMistakes}", Modifier.weight(1f))
                StatTile("星星", "⭐ ${state.stars}", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "掌握度：已掌握 ${state.masteredCount} · 巩固中 ${state.consolidatingCount} · 薄弱 ${state.weakCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProgressCard(daily: List<DailyStat>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("近 14 天答题趋势", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "绿色=做对 · 红色=做错",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (daily.all { it.correct == 0 && it.wrong == 0 }) {
                Text(
                    "还没有答题记录，让孩子先去练几道题吧～",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                ProgressChart(daily)
            }
        }
    }
}

@Composable
private fun ProgressChart(daily: List<DailyStat>) {
    val correctColor = MaterialTheme.colorScheme.tertiary
    val wrongColor = MaterialTheme.colorScheme.error
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        val n = daily.size
        val slot = size.width / n
        val barWidth = slot * 0.55f
        val maxTotal = (daily.maxOfOrNull { it.correct + it.wrong } ?: 1).coerceAtLeast(1)
        val chartH = size.height - 22f
        val baseY = chartH
        daily.forEachIndexed { i, d ->
            val cx = slot * i + slot / 2f
            val x0 = cx - barWidth / 2f
            val correctH = chartH * d.correct / maxTotal
            val wrongH = chartH * d.wrong / maxTotal
            if (d.correct > 0) {
                drawRect(correctColor, Offset(x0, baseY - correctH), Size(barWidth, correctH))
            }
            if (d.wrong > 0) {
                drawRect(wrongColor, Offset(x0, baseY - correctH - wrongH), Size(barWidth, wrongH))
            }
        }
        // 每 3 天标一个日期
        daily.forEachIndexed { i, d ->
            if (i % 3 == 0) {
                label(tm, d.dayLabel, slot * i + slot / 2f, size.height - 4f, labelColor, 11f)
            }
        }
    }
}

private fun DrawScope.label(
    tm: TextMeasurer,
    text: String,
    centerX: Float,
    baselineY: Float,
    color: Color,
    fontSize: Float
) {
    val layout = tm.measure(text, TextStyle(color = color, fontSize = fontSize.sp))
    drawText(layout, topLeft = Offset(centerX - layout.size.width / 2f, baselineY - layout.size.height))
}

@Composable
private fun DomainDistributionCard(list: List<DomainMistake>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("错题分布（按领域）", style = MaterialTheme.typography.titleMedium)
            if (list.isEmpty()) {
                Text(
                    "还没有错题记录。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else {
                val max = list.maxOf { it.count }
                Spacer(Modifier.height(12.dp))
                list.forEach { d ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            d.name,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(88.dp)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(14.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(7.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(d.count.toFloat() / max)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(7.dp))
                            )
                        }
                        Text(
                            "${d.count}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RedemptionCard(list: List<Redemption>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("兑换记录", style = MaterialTheme.typography.titleMedium)
            if (list.isEmpty()) {
                Text(
                    "还没有兑换过奖励。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            } else {
                Spacer(Modifier.height(8.dp))
                list.forEach { r ->
                    Text(
                        "${r.emoji} ${r.name} × ${r.count}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}
