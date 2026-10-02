package com.mathhelper.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 典型误区：归因时用于识别孩子可能的错误想法，并给出纠正锚点。
 */
@Entity(tableName = "misconceptions")
data class MisconceptionEntity(
    @PrimaryKey val id: String,
    val concept: String,
    val knowledgeKeys: List<String>,
    val wrongIdea: String,
    val correctAnchor: String,
    val teachingTip: String
)
