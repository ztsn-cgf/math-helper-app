package com.mathhelper.app.data.seed

import kotlinx.serialization.Serializable

// 与 assets 下三个 JSON 一一对应的解析 DTO（schema/description 等顶层字段用 ignoreUnknownKeys 忽略）。

@Serializable
data class KnowledgePointsFile(val domains: List<DomainDto>)

@Serializable
data class DomainDto(
    val id: String,
    val name: String,
    val description: String = "",
    val areas: List<AreaDto>
)

@Serializable
data class AreaDto(
    val id: String,
    val name: String,
    val gradeNote: String = "",
    val tags: List<String> = emptyList(),
    val topics: List<TopicDto>
)

@Serializable
data class TopicDto(
    val id: String,
    val name: String,
    val tags: List<String> = emptyList()
)

@Serializable
data class MisconceptionsFile(val misconceptions: List<MisconceptionDto>)

@Serializable
data class MisconceptionDto(
    val id: String,
    val concept: String,
    val knowledgeKeys: List<String> = emptyList(),
    val wrongIdea: String,
    val correctAnchor: String,
    val teachingTip: String = ""
)

@Serializable
data class ReferenceMaterialsFile(val materials: List<ReferenceMaterialDto>)

@Serializable
data class ReferenceMaterialDto(
    val id: String,
    val concept: String,
    val unit: String,
    val anchor: String,
    val detail: String = "",
    val tags: List<String> = emptyList()
)

@Serializable
data class ExplanationsFile(val explanations: List<ExplanationDto>)

@Serializable
data class ExplanationDto(
    val knowledgePointId: String,
    val illustration: String = "",
    val title: String,
    val content: String
)
