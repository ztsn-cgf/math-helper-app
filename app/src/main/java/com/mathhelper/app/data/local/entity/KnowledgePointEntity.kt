package com.mathhelper.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 知识点（按领域组织为三级：domain → area → topic，用 parentId 串起来）。
 * 对应 DESIGN.md 第 9 节 KnowledgePoint，额外补了 type/tags/orderIndex 便于树形展示与排序。
 */
@Entity(tableName = "knowledge_points")
data class KnowledgePointEntity(
    @PrimaryKey val id: String,
    val name: String,
    val parentId: String?,
    val type: String,           // "domain" | "area" | "topic"
    val subject: String,
    val gradeNote: String,
    val tags: List<String>,
    val orderIndex: Int
)
