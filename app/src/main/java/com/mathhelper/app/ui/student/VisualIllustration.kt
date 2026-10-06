package com.mathhelper.app.ui.student

import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 知识点插画：根据 explanation 的 illustration 类型画「会动的」图/动画。
 * 全部自动循环、无需交互；尽量用小点/生长/滑动等动画让概念更生动。
 */
@Composable
fun VisualIllustration(type: String, kpId: String) {
    when (type) {
        "perimeter-walk" -> PerimeterWalk()
        "rect" -> RectShape()
        "area-grid" -> AreaGrid()
        "line-vs-face" -> LineVsFace()
        "cube-views" -> CubeViews()
        "cube-net" -> CubeNet()
        "motion" -> MotionDemo(kpId.substringAfterLast('.'))
        "array" -> ArrayDemo()
        "sharing" -> SharingDemo()
        "fraction" -> FractionDemo()
        "number-line" -> NumberLine()
        "number-line-decimal" -> DecimalLine()
        "steps" -> StepsDemo()
        "bar-chart" -> BarChart()
        "anchor" -> AnchorEmoji(kpId)
        else -> {}
    }
}

private fun DrawScope.label(tm: TextMeasurer, text: String, topLeft: Offset, color: Color, size: Float = 13f) {
    drawText(tm, text, topLeft = topLeft, style = TextStyle(color = color, fontSize = size.sp))
}

/** 底部说明文字：先量高度，再贴底留 6px，避免被裁。 */
private fun DrawScope.caption(tm: TextMeasurer, text: String, x: Float, color: Color, fontSize: Float = 13f) {
    val style = TextStyle(color = color, fontSize = fontSize.sp)
    val h = tm.measure(text, style).size.height.toFloat()
    drawText(tm, text, topLeft = Offset(x, size.height - h - 6f), style = style)
}

/** 沿矩形边线走一圈：progress 0→1，返回当前点位置。 */
private fun perimeterPos(left: Float, top: Float, w: Float, h: Float, p: Float): Offset {
    val perim = 2 * (w + h)
    val d = p * perim
    return when {
        d <= w -> Offset(left + d, top)
        d <= w + h -> Offset(left + w, top + (d - w))
        d <= 2 * w + h -> Offset(left + w - (d - w - h), top + h)
        else -> Offset(left, top + h - (d - 2 * w - h))
    }
}

private fun Float.lerpTo(b: Float, t: Float) = this + (b - this) * t

/** 帧无关的循环进度：用系统 ValueAnimator 驱动（直接请求 vsync，绕过 Compose 重组器的帧时钟，兼容部分冻结动画的 ROM）。 */
@Composable
fun loopProgress(durationSeconds: Float): State<Float> {
    val p = remember { mutableStateOf(0f) }
    LaunchedEffect(durationSeconds) {
        val step = 1f / (durationSeconds * 20f) // 约 20 帧/秒
        var v = 0f
        while (true) {
            delay(50)
            v = (v + step) % 1f
            p.value = v
        }
    }
    return p
}

// ---------- 图形几何 ----------

@Composable
private fun PerimeterWalk() {
    val p = loopProgress(3.2f).value
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        val rw = size.width * 0.56f
        val rh = size.height * 0.46f
        val left = (size.width - rw) / 2f
        val top = (size.height - rh) / 2f
        drawRect(primary.copy(alpha = 0.12f), Offset(left, top), Size(rw, rh))
        drawRect(primary, Offset(left, top), Size(rw, rh), style = Stroke(3f))
        drawCircle(secondary, 9f, perimeterPos(left, top, rw, rh, p))
        caption(tm, "小圆点走的一整圈 = 周长", left, onSurface)
    }
}

@Composable
private fun RectShape() {
    val p = loopProgress(3.4f).value
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        val h = size.height
        // 长方形：小点沿边线走，走到哪条边就念那条边（长/宽）
        val rw = size.width * 0.34f
        val rh = h * 0.5f
        val rl = size.width * 0.10f
        val rt = (h - rh) / 2f
        drawRect(primary.copy(alpha = 0.12f), Offset(rl, rt), Size(rw, rh))
        drawRect(primary, Offset(rl, rt), Size(rw, rh), style = Stroke(3f))
        drawCircle(secondary, 8f, perimeterPos(rl, rt, rw, rh, p))
        label(tm, "长", Offset(rl + rw / 2 - 8f, rt - 18f), primary)
        label(tm, "宽", Offset(rl - 24f, rt + rh / 2 - 6f), primary)
        caption(tm, "长方形", rl, primary)
        // 正方形：小点沿边线走，四条边一样长
        val s = h * 0.5f
        val sl = size.width * 0.62f
        val st = (h - s) / 2f
        drawRect(primary.copy(alpha = 0.12f), Offset(sl, st), Size(s, s))
        drawRect(primary, Offset(sl, st), Size(s, s), style = Stroke(3f))
        drawCircle(secondary, 8f, perimeterPos(sl, st, s, s, (p + 0.5f) % 1f))
        label(tm, "边长", Offset(sl + s / 2 - 14f, st - 18f), primary)
        caption(tm, "正方形", sl, primary)
    }
}

@Composable
private fun AreaGrid() {
    val p = loopProgress(3.4f).value
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        val cols = 4
        val rows = 3
        val gw = size.width * 0.5f
        val gh = size.height * 0.55f
        val left = (size.width - gw) / 2f
        val top = (size.height - gh) / 2f
        val cw = gw / cols
        val ch = gh / rows
        val filled = ceil(p * cols * rows).toInt()
        for (r in 0 until rows) for (c in 0 until cols) {
            val idx = r * cols + c
            val x = left + c * cw
            val y = top + r * ch
            if (idx < filled) drawRect(tertiary.copy(alpha = 0.6f), Offset(x, y), Size(cw - 2f, ch - 2f))
            drawRect(primary, Offset(x, y), Size(cw, ch), style = Stroke(1.5f))
        }
        caption(tm, "一格一格铺满 = 面积", left, onSurface)
    }
}

@Composable
private fun LineVsFace() {
    val p = loopProgress(3.6f).value
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        val h = size.height
        val rw = size.width * 0.30f
        val rh = h * 0.5f
        val ll = size.width * 0.08f
        val lt = (h - rh) / 2f
        drawRect(primary, Offset(ll, lt), Size(rw, rh), style = Stroke(3f))
        drawCircle(secondary, 8f, perimeterPos(ll, lt, rw, rh, p))
        caption(tm, "周长·线", ll, primary)

        val gl = size.width * 0.62f
        val gt = lt
        val filled = ceil(p * 12).toInt()
        for (r in 0 until 3) for (c in 0 until 4) {
            val idx = r * 4 + c
            val x = gl + c * (rw / 4)
            val y = gt + r * (rh / 3)
            if (idx < filled) drawRect(tertiary.copy(alpha = 0.6f), Offset(x, y), Size(rw / 4 - 2f, rh / 3 - 2f))
            drawRect(primary, Offset(x, y), Size(rw / 4, rh / 3), style = Stroke(1.5f))
        }
        caption(tm, "面积·面", gl, tertiary)
    }
}

private data class ViewSpec(val name: String, val rows: Int, val cols: Int, val filled: List<Pair<Int, Int>>)

/** 画一个 rows×cols 的「视图」小方格，filled 里的格子填色。 */
private fun DrawScope.viewGrid(x: Float, y: Float, cs: Float, rows: Int, cols: Int, filled: List<Pair<Int, Int>>, fill: Color, stroke: Color, active: Boolean) {
    for (r in 0 until rows) for (c in 0 until cols) {
        val cx = x + c * cs
        val cy = y + r * cs
        if (filled.contains(r to c)) drawRect(fill, Offset(cx, cy), Size(cs, cs))
        drawRect(stroke, Offset(cx, cy), Size(cs, cs), style = Stroke(if (active) 3.5f else 2f))
    }
}

@Composable
private fun CubeViews() {
    val p = loopProgress(3.6f).value
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant
    val tm = rememberTextMeasurer()
    // 同一个「4 个小方块搭成的形状」：前面看是 T、上面看是一排、侧面看是一列
    val views = listOf(
        ViewSpec("前面", 2, 3, listOf(0 to 0, 0 to 1, 0 to 2, 1 to 1)),
        ViewSpec("上面", 1, 3, listOf(0 to 0, 0 to 1, 0 to 2)),
        ViewSpec("侧面", 2, 1, listOf(0 to 0, 1 to 0))
    )
    Canvas(Modifier.fillMaxWidth().height(190.dp)) {
        val cs = size.width * 0.085f
        val current = (p * 3).toInt() % 3
        views.forEachIndexed { i, spec ->
            val active = i == current
            val vx = size.width * (0.08f + i * 0.32f)
            val vy = size.height * 0.20f
            viewGrid(vx, vy, cs, spec.rows, spec.cols, spec.filled,
                if (active) secondary.copy(alpha = 0.55f) else primary.copy(alpha = 0.35f),
                primary, active)
            label(tm, spec.name, Offset(vx + cs, vy + spec.rows * cs + 6f), if (active) secondary else primary, 14f)
        }
        caption(tm, "同一个立体，三个方向看不一样", size.width * 0.08f, onSurface)
    }
}

@Composable
private fun CubeNet() {
    val p = loopProgress(3.6f).value
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().height(200.dp)) {
        val s = min(size.width, size.height) * 0.20f
        val startX = (size.width - 4 * s) / 2f
        val startY = (size.height - 3 * s) / 2f
        val cells = listOf(
            Triple(0, 1, true),
            Triple(1, 0, false), Triple(1, 1, true), Triple(1, 2, true), Triple(1, 3, false),
            Triple(2, 1, false)
        )
        val shown = ceil(p * cells.size).toInt()
        cells.forEachIndexed { idx, (r, c, accent) ->
            if (idx < shown) {
                val x = startX + c * s
                val y = startY + r * s
                drawRect(if (accent) secondary.copy(alpha = 0.25f) else primary.copy(alpha = 0.12f), Offset(x, y), Size(s, s))
                drawRect(primary, Offset(x, y), Size(s, s), style = Stroke(2f))
            }
        }
        caption(tm, "剪开摊平 = 展开图（6 个面）", startX, primary)
    }
}

// ---------- 数与代数 ----------

@Composable
private fun ArrayDemo() {
    val p = loopProgress(3.4f).value
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        val cols = 4
        val rows = 3
        val gw = size.width * 0.5f
        val gh = size.height * 0.5f
        val left = (size.width - gw) / 2f
        val top = (size.height - gh) / 2f
        val cw = gw / (cols + 1)
        val ch = gh / (rows + 1)
        val shown = ceil(p * cols * rows).toInt()
        for (r in 0 until rows) for (c in 0 until cols) {
            val idx = r * cols + c
            if (idx < shown) {
                drawCircle(secondary, 7f, Offset(left + (c + 1) * cw, top + (r + 1) * ch))
            }
        }
        caption(tm, "3 组 × 每行 4 个 = 12", left, primary)
    }
}

@Composable
private fun SharingDemo() {
    val p = loopProgress(3.2f).value
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        val groups = 3
        val per = 4
        val bw = size.width * 0.24f
        val bh = size.height * 0.55f
        val gap = (size.width - bw * groups) / (groups + 1)
        val top = (size.height - bh) / 2f
        for (g in 0 until groups) {
            val bx = gap + g * (bw + gap)
            drawRect(primary.copy(alpha = 0.10f), Offset(bx, top), Size(bw, bh))
            drawRect(primary, Offset(bx, top), Size(bw, bh), style = Stroke(2f))
        }
        val shown = ceil(p * groups * per).toInt()
        for (k in 0 until shown) {
            val g = k % groups
            val slot = k / groups
            val bx = gap + g * (bw + gap)
            val cx = bx + bw * (0.3f + (slot % 2) * 0.4f)
            val cy = top + bh * (0.25f + (slot / 2) * 0.5f)
            drawCircle(tertiary, 8f, Offset(cx, cy))
        }
        caption(tm, "12 平均分成 3 份，每份 4 个", size.width * 0.05f, primary)
    }
}

@Composable
private fun FractionDemo() {
    val p = loopProgress(3.6f).value
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        val r = min(size.width, size.height) * 0.34f
        val cx = size.width * 0.5f
        val cy = size.height * 0.46f
        val topLeft = Offset(cx - r, cy - r)
        val sz = Size(r * 2, r * 2)
        val pulse = if (p < 0.5f) 0.55f + 0.3f * (p * 2f) else 0.85f - 0.3f * ((p - 0.5f) * 2f)
        for (i in 0 until 4) {
            val start = -90f + i * 90f
            val color = if (i == 0) tertiary.copy(alpha = pulse) else primary.copy(alpha = 0.10f)
            drawArc(color, start, 90f, true, topLeft, sz)
        }
        drawCircle(primary, r, Offset(cx, cy), style = Stroke(3f))
        caption(tm, "平均分 4 份，取 1 份 = 1/4", size.width * 0.15f, primary)
    }
}

@Composable
private fun NumberLine() {
    val p = loopProgress(3.2f).value
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val y = size.height * 0.55f
        val left = size.width * 0.10f
        val right = size.width * 0.90f
        drawLine(primary, Offset(left, y), Offset(right, y), 3f)
        for (i in 0..5) {
            val x = left + (right - left) * i / 5f
            drawLine(primary, Offset(x, y - 8f), Offset(x, y + 8f), 2.5f)
            label(tm, "$i", Offset(x - 4f, y + 12f), primary, 13f)
        }
        val x2 = left + (right - left) * 2 / 5f
        val x5 = left + (right - left) * 5 / 5f
        drawCircle(secondary, 8f, Offset(x2.lerpTo(x5, p), y))
        caption(tm, "2 + 3 = 5（从 2 跳到 5）", left, secondary)
    }
}

@Composable
private fun DecimalLine() {
    val p = loopProgress(3.6f).value
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val y = size.height * 0.55f
        val left = size.width * 0.12f
        val right = size.width * 0.88f
        drawLine(primary, Offset(left, y), Offset(right, y), 3f)
        for (i in 0..10) {
            val x = left + (right - left) * i / 10f
            val tall = i % 5 == 0
            drawLine(primary, Offset(x, y - if (tall) 12f else 7f), Offset(x, y + if (tall) 12f else 7f), 2f)
            if (tall) {
                val text = when (i) { 0 -> "0"; 10 -> "1"; else -> "0.5" }
                label(tm, text, Offset(x - 7f, y + 14f), primary, 11f)
            }
        }
        drawCircle(secondary, 7f, Offset(left.lerpTo(right, p), y))
        caption(tm, "小点从 0 滑到 1，中间是 0.5", size.width * 0.15f, secondary)
    }
}

@Composable
private fun StepsDemo() {
    val p = loopProgress(3.6f).value
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val dim = MaterialTheme.colorScheme.outline
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().height(130.dp)) {
        val left = size.width * 0.12f
        val y1 = size.height * 0.30f
        val y2 = size.height * 0.66f
        val current = (p * 2).toInt() % 2
        label(tm, "① 先算：4 × 3 = 12", Offset(left, y1), if (current == 0) tertiary else dim, 15f)
        label(tm, "② 再算：8 + 12 = 20", Offset(left, y2), if (current == 1) primary else dim, 15f)
    }
}

@Composable
private fun BarChart() {
    val p = loopProgress(3.4f).value
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        val baseY = size.height * 0.78f
        val left = size.width * 0.12f
        val right = size.width * 0.88f
        val n = 4
        val bw = (right - left) / (n * 2)
        val heights = listOf(0.35f, 0.6f, 0.45f, 0.8f)
        drawLine(primary, Offset(left, baseY), Offset(right, baseY), 2.5f)
        heights.forEachIndexed { i, hf ->
            val local = ((p * n) - i).coerceIn(0f, 1f)
            val x = left + bw * (2 * i + 1)
            val hh = (baseY - size.height * 0.12f) * hf * local
            val top = baseY - hh
            drawRect(if (i == 3) tertiary.copy(alpha = 0.8f) else primary.copy(alpha = 0.7f), Offset(x, top), Size(bw, hh))
        }
        caption(tm, "一根一根长起来，谁高谁低", left, primary)
    }
}

// ---------- 量感 ----------

@Composable
private fun AnchorEmoji(kpId: String) {
    when {
        kpId.startsWith("kp.len") -> RulerDemo()
        kpId.startsWith("kp.mass") -> BalanceDemo()
        kpId.startsWith("kp.time") -> ClockDemo()
        else -> Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
            Text("🧮", fontSize = 64.sp)
        }
    }
}

/** 长度：一把直尺，标出厘米/毫米刻度，小点沿尺子走。 */
@Composable
private fun RulerDemo() {
    val p = loopProgress(3.4f).value
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val left = size.width * 0.06f
        val right = size.width * 0.94f
        val top = size.height * 0.18f
        val h = size.height * 0.36f
        // 尺子主体
        drawRect(primary.copy(alpha = 0.10f), Offset(left, top), Size(right - left, h))
        drawRect(primary, Offset(left, top), Size(right - left, h), style = Stroke(2f))
        // 5 大格 = 5 厘米，每厘米 10 小格（毫米）
        val cmCount = 5
        val cmW = (right - left) / cmCount
        for (cm in 0..cmCount) {
            val x = left + cm * cmW
            drawLine(primary, Offset(x, top), Offset(x, top + h * 0.55f), 3f)
            label(tm, "$cm", Offset(x - 4f, top + h * 0.62f), primary, 12f)
            if (cm < cmCount) {
                for (mm in 1..9) {
                    val mx = x + mm * cmW / 10f
                    drawLine(primary.copy(alpha = 0.5f), Offset(mx, top), Offset(mx, top + h * 0.3f), 1.5f)
                }
            }
        }
        // 移动小点
        val dotX = left + (right - left) * p
        drawCircle(secondary, 8f, Offset(dotX, top + h * 0.78f))
        caption(tm, "1 厘米里有 10 个小格（毫米）", left, onSurface)
    }
}

/** 质量：一架天平，重的一边（1 千克）往下沉。 */
@Composable
private fun BalanceDemo() {
    val p = loopProgress(3.6f).value
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().height(190.dp)) {
        val cx = size.width / 2f
        val beamY = size.height * 0.42f
        val half = size.width * 0.30f
        val rock = (sin(p * 2.0 * PI) * 0.05).toFloat()
        val drop = 0.16f + rock // 右边始终更沉，轻微摇摆
        val leftX = cx - half
        val rightX = cx + half
        val leftY = beamY - drop * half
        val rightY = beamY + drop * half
        // 支点（三角）
        val fulcrum = Path().apply {
            moveTo(cx, beamY); lineTo(cx - 22f, beamY + 44f); lineTo(cx + 22f, beamY + 44f); close()
        }
        drawPath(fulcrum, primary.copy(alpha = 0.25f))
        drawPath(fulcrum, primary, style = Stroke(2f))
        // 横梁
        drawLine(primary, Offset(leftX, leftY), Offset(rightX, rightY), 4f)
        // 两条吊绳 + 托盘
        drawLine(primary, Offset(leftX, leftY), Offset(leftX, leftY + 42f), 2f)
        drawLine(primary, Offset(rightX, rightY), Offset(rightX, rightY + 42f), 2f)
        drawArc(primary, 0f, 180f, false, Offset(leftX - 30f, leftY + 32f), Size(60f, 30f), style = Stroke(2f))
        drawArc(primary, 0f, 180f, false, Offset(rightX - 30f, rightY + 32f), Size(60f, 30f), style = Stroke(2f))
        // 物品：左边小（1 克回形针），右边大（1 千克）
        drawCircle(secondary, 10f, Offset(leftX, leftY + 20f))
        drawCircle(tertiary, 22f, Offset(rightX, rightY + 16f))
        label(tm, "1 克", Offset(leftX - 16f, leftY + 78f), secondary, 13f)
        label(tm, "1 千克", Offset(rightX - 26f, rightY + 78f), tertiary, 13f)
        caption(tm, "越重的一边越往下沉", size.width * 0.08f, onSurface)
    }
}

/** 时间：一个钟面，秒针在走。 */
@Composable
private fun ClockDemo() {
    val p = loopProgress(3.6f).value
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurfaceVariant
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().height(170.dp)) {
        val cx = size.width / 2f
        val cy = size.height * 0.46f
        val r = size.height * 0.36f
        drawCircle(primary, r, Offset(cx, cy), style = Stroke(3f))
        // 12 个刻度
        for (i in 0 until 12) {
            val a = i * PI / 6.0
            val x1 = (cx + cos(a) * (r - 10f)).toFloat()
            val y1 = (cy + sin(a) * (r - 10f)).toFloat()
            val x2 = (cx + cos(a) * r).toFloat()
            val y2 = (cy + sin(a) * r).toFloat()
            drawLine(primary, Offset(x1, y1), Offset(x2, y2), 2.5f)
        }
        // 时针（指向 3 点）
        drawLine(primary, Offset(cx, cy), Offset(cx + r * 0.42f, cy), 5f)
        // 秒针（转一圈）
        val a = p * 2.0 * PI
        val sx = (cx + cos(a) * r * 0.7f).toFloat()
        val sy = (cy + sin(a) * r * 0.7f).toFloat()
        drawLine(secondary, Offset(cx, cy), Offset(sx, sy), 3f)
        drawCircle(primary, 4f, Offset(cx, cy))
        caption(tm, "秒针走一圈 = 1 分钟", size.width * 0.22f, onSurface)
    }
}
