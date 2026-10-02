package com.mathhelper.app.ai

import com.mathhelper.app.data.local.dao.KnowledgePointDao
import kotlinx.serialization.json.Json

/**
 * 归因编排：把 OCR 文本 + 知识点目录发给 AI，解析回结构化归因结果。
 */
class AttributionService(
    private val aiClient: AiClient,
    private val knowledgePointDao: KnowledgePointDao
) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun attribute(ocrText: String): AttributionResult? {
        val catalog = buildTopicCatalog()
        if (catalog.isBlank()) return null
        val raw = aiClient.chat(SYSTEM_PROMPT, "知识点列表：\n$catalog\n\n错题文本：\n$ocrText")
        return parse(raw)
    }

    private suspend fun buildTopicCatalog(): String {
        val topics = knowledgePointDao.getTopics()
        return topics.joinToString("\n") { "${it.id} | ${it.name}" }
    }

    private fun parse(raw: String): AttributionResult? {
        val cleaned = raw.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        return runCatching {
            json.decodeFromString<AttributionResult>(cleaned)
        }.getOrNull()
    }

    suspend fun generatePracticeQuestions(knowledgePointName: String): List<PracticeQuestionDto> {
        val raw = aiClient.chat(PRACTICE_PROMPT, "知识点：$knowledgePointName")
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return runCatching {
            json.decodeFromString<PracticeQuestionsResult>(cleaned).practiceQuestions
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val SYSTEM_PROMPT =
            "你是小学数学老师，负责判断小学生错题对应的知识点与误区。\n" +
                "给定「知识点列表」和「错题文本」：\n" +
                "1. 从列表里选出最匹配的一个知识点 id，填入 knowledge_point_id，并把名称填入 knowledge_point_name；\n" +
                "2. 用一句话描述孩子可能的误区，填入 misconception；\n" +
                "3. 写一段 90 秒内能讲完的短讲解填入 explanation，要具体、多用生活参照物；\n" +
                "4. 出 1~2 道同类题填入 practice_questions，每道含 question 和 answer。\n" +
                "只输出 JSON，不要输出任何多余文字或解释。"

        private const val PRACTICE_PROMPT =
            "你是小学数学老师。根据给定知识点，出 2~3 道适合三年级小学生的练习题。只输出 JSON：{\"practice_questions\":[{\"question\":\"题目\",\"answer\":\"答案\"}]}，不要输出多余文字。"
    }
}
