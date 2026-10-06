package com.mathhelper.app.ui.student

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mathhelper.app.data.local.AppDatabase
import com.mathhelper.app.ui.common.BackButton
import com.mathhelper.app.ui.common.BigActionButton
import com.mathhelper.app.ui.theme.StudentTheme
import com.mathhelper.app.util.RewardStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class StudentHomeViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    private val rewardStore = RewardStore.getInstance(app)

    /** 建议学习知识点：优先最近错题，其次待复习，最后最薄弱的知识点。 */
    val suggestedKpId: StateFlow<String?> = combine(
        db.mistakeDao().observeAll(),
        db.masteryDao().observeAll()
    ) { mistakes, masteries ->
        val now = System.currentTimeMillis()
        // 优先高等级（重点巩固）的最近错题，其次低等级（涂改后对）
        mistakes.sortedWith(compareByDescending { it.severity == "high" }).firstOrNull()?.knowledgePointId
            ?: masteries
                .filter { it.status != "mastered" && it.nextReviewTime in 0..now }
                .minByOrNull { it.nextReviewTime }?.knowledgePointId
            ?: masteries
                .filter { it.status == "weak" || it.status == "consolidating" }
                .minByOrNull { it.lastTestTime }?.knowledgePointId
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** 待复习（间隔复习到期）的知识点数。 */
    val dueReviewCount: StateFlow<Int> = db.masteryDao().observeAll()
        .map { masteries ->
            val now = System.currentTimeMillis()
            masteries.count { it.status != "mastered" && it.nextReviewTime in 0..now }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val hasContent: StateFlow<Boolean> = db.mistakeDao().observeAll()
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val stars: StateFlow<Int> = rewardStore.stars
    val childName: StateFlow<String> = rewardStore.childName
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentHomeScreen(
    onBack: () -> Unit,
    onLearn: (String) -> Unit,
    onBrowse: () -> Unit,
    onReward: () -> Unit,
    onLevel: () -> Unit,
    viewModel: StudentHomeViewModel = viewModel()
) {
    val suggestedKpId by viewModel.suggestedKpId.collectAsState()
    val hasContent by viewModel.hasContent.collectAsState()
    val stars by viewModel.stars.collectAsState()
    val name by viewModel.childName.collectAsState()
    val dueReviewCount by viewModel.dueReviewCount.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("学生模式") },
                navigationIcon = { BackButton(onBack) }
            )
        }
    ) { padding ->
        StudentTheme {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.widthIn(max = 560.dp).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Text(
                        "⭐ $stars 颗星星",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    if (dueReviewCount > 0) {
                        Text(
                            "📚 有 $dueReviewCount 个知识点待复习",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Text(
                        if (hasContent) "$name，准备好了吗？点一下开始！" else "还没有错题，请让家长先录入哦",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BigActionButton(
                        text = "开始学习",
                        onClick = { suggestedKpId?.let(onLearn) }
                    )
                    BigActionButton(text = "闯关 🎮", onClick = onLevel)
                    BigActionButton(text = "自己学（按知识点）", onClick = onBrowse)
                    BigActionButton(text = "我的奖励 ⭐", onClick = onReward)
                }
            }
        }
    }
}
