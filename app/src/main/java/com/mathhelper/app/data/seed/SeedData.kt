package com.mathhelper.app.data.seed

import android.content.Context
import com.mathhelper.app.data.local.AppDatabase
import com.mathhelper.app.data.local.entity.KnowledgePointEntity
import com.mathhelper.app.data.local.entity.MisconceptionEntity
import com.mathhelper.app.data.local.entity.ReferenceMaterialEntity
import kotlinx.serialization.json.Json

/**
 * 把 assets 里的三份地基 JSON 解析成 Room 实体，首次启动时灌入数据库。
 * 知识点树从「嵌套 JSON」拍平成「domain → area → topic」三级、用 parentId 连接的扁平行。
 */
object SeedData {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun seedIfEmpty(context: Context, db: AppDatabase) {
        if (db.knowledgePointDao().count() > 0) return
        db.knowledgePointDao().insertAll(loadKnowledgePoints(context))
        db.misconceptionDao().insertAll(loadMisconceptions(context))
        db.referenceMaterialDao().insertAll(loadReferenceMaterials(context))
    }

    private fun loadKnowledgePoints(context: Context): List<KnowledgePointEntity> {
        val file = json.decodeFromString<KnowledgePointsFile>(
            readAsset(context, "knowledge-points.json")
        )
        val result = mutableListOf<KnowledgePointEntity>()
        var order = 0
        file.domains.forEach { domain ->
            result += KnowledgePointEntity(
                id = domain.id, name = domain.name, parentId = null,
                type = "domain", subject = "math", gradeNote = "", tags = emptyList(),
                orderIndex = order++
            )
            domain.areas.forEach { area ->
                result += KnowledgePointEntity(
                    id = area.id, name = area.name, parentId = domain.id,
                    type = "area", subject = "math", gradeNote = area.gradeNote, tags = area.tags,
                    orderIndex = order++
                )
                area.topics.forEach { topic ->
                    result += KnowledgePointEntity(
                        id = topic.id, name = topic.name, parentId = area.id,
                        type = "topic", subject = "math", gradeNote = area.gradeNote, tags = topic.tags,
                        orderIndex = order++
                    )
                }
            }
        }
        return result
    }

    private fun loadMisconceptions(context: Context): List<MisconceptionEntity> {
        val file = json.decodeFromString<MisconceptionsFile>(
            readAsset(context, "misconceptions.json")
        )
        return file.misconceptions.map {
            MisconceptionEntity(
                id = it.id, concept = it.concept, knowledgeKeys = it.knowledgeKeys,
                wrongIdea = it.wrongIdea, correctAnchor = it.correctAnchor, teachingTip = it.teachingTip
            )
        }
    }

    private fun loadReferenceMaterials(context: Context): List<ReferenceMaterialEntity> {
        val file = json.decodeFromString<ReferenceMaterialsFile>(
            readAsset(context, "reference-materials.json")
        )
        return file.materials.map {
            ReferenceMaterialEntity(
                id = it.id, concept = it.concept, unit = it.unit,
                anchor = it.anchor, detail = it.detail, tags = it.tags
            )
        }
    }

    private fun readAsset(context: Context, name: String): String =
        context.assets.open(name).bufferedReader().use { it.readText() }
}
