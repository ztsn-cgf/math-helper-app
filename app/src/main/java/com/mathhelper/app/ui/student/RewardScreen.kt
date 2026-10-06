package com.mathhelper.app.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mathhelper.app.ui.common.BackButton
import com.mathhelper.app.ui.theme.StudentTheme
import com.mathhelper.app.util.RewardStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { RewardStore(context) }
    val stars by store.stars.collectAsState()
    val name by store.childName.collectAsState()
    val redeemedCounts by store.redeemedCounts.collectAsState()

    var redeemTarget by remember { mutableStateOf<RewardStore.RewardTier?>(null) }
    var pin by remember { mutableStateOf("") }
    var redeemError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("我的奖励") },
                navigationIcon = { BackButton(onBack) }
            )
        }
    ) { padding ->
        StudentTheme {
            Column(Modifier.fillMaxSize().padding(padding)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎁", fontSize = 56.sp)
                    Text("$name 的星星", style = MaterialTheme.typography.titleLarge)
                    Text("⭐ $stars", fontSize = 44.sp, color = MaterialTheme.colorScheme.secondary)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(store.rewards, key = { it.reward }) { tier ->
                        RewardTierCard(
                            stars = stars,
                            redeemedCount = redeemedCounts[tier.reward] ?: 0,
                            tier = tier,
                            onClickRedeem = {
                                redeemTarget = tier
                                pin = ""
                                redeemError = false
                            }
                        )
                    }
                }
            }
        }
    }

    redeemTarget?.let { tier ->
        AlertDialog(
            onDismissRequest = { redeemTarget = null },
            title = { Text("兑换「${tier.reward}」") },
            text = {
                Column {
                    Text(
                        "需要 ${tier.cost} 颗星。请输入家长 PIN 码兑换",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { pin = it; redeemError = false },
                        label = { Text("家长 PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                    if (redeemError) {
                        Text(
                            "PIN 不对哦，再试一次",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (store.redeem(tier, pin)) {
                        redeemTarget = null
                    } else {
                        redeemError = true
                    }
                }) { Text("兑换") }
            },
            dismissButton = {
                TextButton(onClick = { redeemTarget = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun RewardTierCard(
    stars: Int,
    redeemedCount: Int,
    tier: RewardStore.RewardTier,
    onClickRedeem: () -> Unit
) {
    val enough = stars >= tier.cost
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(tier.emoji, fontSize = 36.sp)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(tier.reward, style = MaterialTheme.typography.titleMedium)
                if (redeemedCount > 0) {
                    Text(
                        "已兑换 $redeemedCount 次",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                if (!enough) {
                    Text(
                        "还差 ${tier.cost - stars} 颗星",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (enough) {
                Button(onClick = onClickRedeem) { Text("兑换") }
            } else {
                Text("🔒", fontSize = 20.sp)
            }
        }
    }
}
