package com.mathhelper.app.ui.student

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import kotlin.random.Random

private val EMOJIS = listOf("⭐", "✨", "🌟", "💛", "🎈", "🏆")

private data class Particle(
    val emoji: String,
    val x: Int,
    val y: Int,
    val size: Int,
    val delay: Float
)

/**
 * 答对时的星星撒花动画（对应 abc-game 的 FxBurst.vue）。
 * [trigger] 每次变化（且 > 0）时播放一次约 1s 的粒子飞散。
 */
@Composable
fun StarBurst(trigger: Int) {
    if (trigger <= 0) return
    val density = LocalDensity.current.density

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = constraints.maxWidth
        val h = constraints.maxHeight
        val particles = remember(trigger) {
            val rnd = Random(trigger * 31L + 7L)
            List(18) {
                Particle(
                    emoji = EMOJIS[rnd.nextInt(EMOJIS.size)],
                    x = (w * (0.26f + rnd.nextFloat() * 0.48f)).roundToInt(),
                    y = (h * (0.20f + rnd.nextFloat() * 0.42f)).roundToInt(),
                    size = (30 + rnd.nextFloat() * 34).roundToInt(),
                    delay = rnd.nextFloat() * 0.18f
                )
            }
        }

        val progress = remember { Animatable(0f) }
        LaunchedEffect(trigger) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(1000, easing = FastOutSlowInEasing))
        }

        val t = progress.value
        particles.forEach { p ->
            val lt = ((t - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            val scale = if (lt < 0.25f) (lt / 0.25f) * 1.4f else 1.4f - ((lt - 0.25f) / 0.75f) * 0.9f
            val alpha = if (lt < 0.25f) lt / 0.25f else 1f - (lt - 0.25f) / 0.75f
            val ty = -lt * 140f * density
            Text(
                text = p.emoji,
                fontSize = p.size.sp,
                modifier = Modifier
                    .offset { IntOffset(p.x, p.y) }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                        translationY = ty
                    }
            )
        }
    }
}
