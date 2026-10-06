package com.mathhelper.app.util

/**
 * 个性化鼓励语：带上孩子称呼，增强专属感。
 * 参照 abc-game 的 encourage.js。
 */
object Encouragement {

    private val phrases = listOf(
        "{name}，太棒了！🎉",
        "{name}，真厉害！⭐",
        "{name}，越来越棒了！🌟",
        "{name}，你真是数学小达人！🏆",
        "{name}，答对啦，继续加油！💪",
        "{name}，好聪明！✨",
        "{name}，又答对一道，真了不起！🎈",
        "{name}，太厉害了，给你鼓掌！👏"
    )

    fun random(name: String): String =
        phrases.random().replace("{name}", name)
}
