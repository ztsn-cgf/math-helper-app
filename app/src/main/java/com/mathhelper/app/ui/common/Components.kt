package com.mathhelper.app.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BigActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(64.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun BackButton(onBack: () -> Unit) {
    TextButton(onClick = onBack) { Text("返回") }
}
