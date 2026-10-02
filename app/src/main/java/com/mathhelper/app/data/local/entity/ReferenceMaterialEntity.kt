package com.mathhelper.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 参照物 / 量感锚点：把抽象单位变成孩子脑子里的具体生活参照。
 * 用于生成「图文参照卡（card）」讲解。
 */
@Entity(tableName = "reference_materials")
data class ReferenceMaterialEntity(
    @PrimaryKey val id: String,
    val concept: String,
    val unit: String,
    val anchor: String,
    val detail: String,
    val tags: List<String>
)
