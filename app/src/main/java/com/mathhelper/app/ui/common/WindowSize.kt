package com.mathhelper.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * 屏幕宽度分档，用于手机 / 平板自适应（硬约束第 8 条）。
 * - Compact：< 600dp，手机竖屏
 * - Medium：600~840dp，小平板 / 折叠屏
 * - Expanded：>= 840dp，大平板横屏
 *
 * 说明：先按屏幕宽度 dp 简化实现，后续如需更精确可换 material3 的 WindowSizeClass。
 */
enum class WindowSize { Compact, Medium, Expanded }

@Composable
fun currentWindowSize(): WindowSize {
    val widthDp = LocalConfiguration.current.screenWidthDp
    return when {
        widthDp < 600 -> WindowSize.Compact
        widthDp < 840 -> WindowSize.Medium
        else -> WindowSize.Expanded
    }
}
