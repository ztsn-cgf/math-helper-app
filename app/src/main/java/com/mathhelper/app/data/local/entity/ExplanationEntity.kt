package com.mathhelper.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 讲解内容：按知识点复用，一条短讲解推给学生。
 * 对应 DESIGN.md 第 9 节 Explanation；type = video | card | text。
 */
@Entity(tableName = "explanations")
data class ExplanationEntity(
    @PrimaryKey val id: String,
    val knowledgePointId: String,
    val type: String,        // video | card | text
    val source: String,      // external | ai | builtin
    val title: String,
    val content: String,
    val mediaUrl: String?,
    val durationSec: Int
)
