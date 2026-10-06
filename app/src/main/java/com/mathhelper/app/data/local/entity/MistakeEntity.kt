package com.mathhelper.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 错题：家长拍照录入的一页/一道错题。
 * 对应 DESIGN.md 第 9 节 Mistake。
 */
@Entity(tableName = "mistakes")
data class MistakeEntity(
    @PrimaryKey val id: String,
    val imagePath: String?,        // 本地图片路径（可空）
    val ocrText: String,
    val knowledgePointId: String?, // 归因到的知识点 id（可空，未归因时手动选）
    val mistakeType: String,       // AI 判断的误区描述
    val severity: String = "high", // 错误等级：high=重点巩固 / low=轻微·后续加练
    val createTime: Long
)
