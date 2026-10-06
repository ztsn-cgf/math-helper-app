package com.mathhelper.app.ui.parent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mathhelper.app.ui.common.BackButton
import com.mathhelper.app.ui.common.BigActionButton
import com.mathhelper.app.util.PinManager

/**
 * 家长模式：先过 PIN，再进入录入 / 错题列表 / 掌握度地图。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentHomeScreen(
    onBack: () -> Unit,
    onPhotoEntry: () -> Unit,
    onMistakeList: () -> Unit,
    onMasteryMap: () -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    val pinManager = remember { PinManager(context) }
    var unlocked by remember { mutableStateOf(pinManager.isUnlocked()) }
    if (!unlocked) {
        PinGate(onUnlock = { pinManager.unlock(10); unlocked = true }, onBack = onBack)
    } else {
        ParentContent(
            onBack = onBack,
            onPhotoEntry = onPhotoEntry,
            onMistakeList = onMistakeList,
            onMasteryMap = onMasteryMap,
            onSettings = onSettings
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentContent(
    onBack: () -> Unit,
    onPhotoEntry: () -> Unit,
    onMistakeList: () -> Unit,
    onMasteryMap: () -> Unit,
    onSettings: () -> Unit
) {
    var showChangePin by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("家长模式") },
                navigationIcon = { BackButton(onBack) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = 560.dp).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                BigActionButton("录入错题", onClick = onPhotoEntry)
                BigActionButton("错题列表", onClick = onMistakeList)
                BigActionButton("掌握度地图", onClick = onMasteryMap)
                BigActionButton("AI 设置", onClick = onSettings)
                TextButton(onClick = { showChangePin = true }) { Text("修改 PIN") }
            }
        }
    }

    if (showChangePin) {
        PinChangeDialog(onDismiss = { showChangePin = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinGate(onUnlock: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val pinManager = remember { PinManager(context) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("家长模式") },
                navigationIcon = { BackButton(onBack) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("请输入家长 PIN", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.take(4); error = false },
                label = { Text("4 位 PIN") },
                singleLine = true,
                isError = error,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.padding(top = 16.dp)
            )
            if (error) {
                Text(
                    "PIN 错误，请重试",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Button(
                onClick = {
                    if (pinManager.verify(pin)) onUnlock()
                    else { error = true; pin = "" }
                },
                enabled = pin.length == 4,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("进入")
            }
        }
    }
}

@Composable
private fun PinChangeDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val pinManager = remember { PinManager(context) }
    var newPin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改 PIN") },
        text = {
            OutlinedTextField(
                value = newPin,
                onValueChange = { newPin = it.take(4) },
                label = { Text("新 4 位 PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newPin.length == 4) {
                        pinManager.setPin(newPin)
                        onDismiss()
                    }
                }
            ) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
