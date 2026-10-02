package com.mathhelper.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 答题记录：学生每做一道同类题记一条。
 * 对应 DESIGN.md 第 9 节 Attempt。
 */
@Entity(tableName = "attempts")
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val questionId: String,
    val correct: Boolean,
    val time: Long
)
