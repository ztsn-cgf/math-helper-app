package com.mathhelper.app.ui.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * 图形的运动小演示：一个三角形动起来。
 * mode: "trans" 平移 / "rotate" 旋转 / "sym" 轴对称（对折）
 */
@Composable
fun MotionDemo(mode: String) {
    val progress = loopProgress(4.0f).value

    val (label, shapeModifier) = when (mode) {
        "rotate" -> "旋转：绕着一个点转" to
            Modifier.graphicsLayer { rotationZ = progress * 360f }
        "trans" -> "平移：直直地滑过去" to
            Modifier.graphicsLayer { translationX = (progress - 0.5f) * 220f }
        "sym" -> {
            val fold = abs(1f - progress * 2f) // 1 → 0 → 1，折进去再展开
            "轴对称：对折能重合" to
                Modifier.graphicsLayer { scaleX = fold }
        }
        else -> "" to Modifier
    }

    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
        if (mode == "sym") {
            // 对称轴（对折线）
            Box(
                Modifier
                    .width(2.dp)
                    .height(110.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            )
        }
        Text("▲", fontSize = 88.sp, color = MaterialTheme.colorScheme.primary, modifier = shapeModifier)
    }
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
