package com.mathhelper.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 掌握度：「学生 × 知识点」的掌握状态。
 * 对应 DESIGN.md 第 9 节 Mastery。MVP 单学生，故以知识点 id 为主键。
 * status: new | weak | consolidating | mastered
 */
@Entity(tableName = "mastery")
data class MasteryEntity(
    @PrimaryKey val knowledgePointId: String,
    val status: String,
    val correctStreak: Int,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val nextReviewTime: Long = 0,
    val lastTestTime: Long
)
