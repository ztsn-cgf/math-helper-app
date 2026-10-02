package com.mathhelper.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 同类题：按知识点存储，学生做错题后推送练习。
 * 对应 DESIGN.md 第 9 节 PracticeQuestion。
 */
@Entity(tableName = "practice_questions")
data class PracticeQuestionEntity(
    @PrimaryKey val id: String,
    val knowledgePointId: String,
    val content: String,
    val answer: String,
    val options: List<String>
)
