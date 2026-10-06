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

    /** 整页分析：把一页作业/试卷拆成多道题，判断对错，归因做错的题。 */
    suspend fun analyzePage(ocrText: String): List<QuestionAnalysis> {
        val catalog = buildTopicCatalog()
        if (catalog.isBlank()) return emptyList()
        val raw = aiClient.chat(PAGE_PROMPT, "知识点列表：\n$catalog\n\n整页文本：\n$ocrText")
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return runCatching {
            json.decodeFromString<PageAnalysisResult>(cleaned).questions
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val SYSTEM_PROMPT =
            "你是深圳地区的小学三年级数学老师，熟悉北师大版教材。负责判断小学生错题对应的知识点与误区。\n" +
                "给定「知识点列表」和「错题文本」：\n" +
                "1. 从列表里选出最匹配的一个知识点 id，填入 knowledge_point_id，并把名称填入 knowledge_point_name；\n" +
                "2. 用一句话描述孩子可能的误区，填入 misconception；\n" +
                "3. 写一段 90 秒内能讲完的短讲解填入 explanation，要具体、多用生活参照物；\n" +
                "4. 出 1~2 道同类题填入 practice_questions，每道含 question、answer、options（比较大小/判断/选单位/选答案这类定性题要给出 3~4 个选项，纯计算题 options 给空数组）。\n" +
                "只输出 JSON，不要输出任何多余文字或解释。"

        private const val PAGE_PROMPT =
            "你是深圳地区的小学三年级数学老师，熟悉北师大版教材。给定一整页作业/试卷的文本，请：\n" +
                "1. 把整页拆分成一道道独立题目（question，简短保留题干）。\n" +
                "2. 判断每道题是否做错（is_wrong，true=做错、false=做对）。宁可多标、不要漏标。\n" +
                "3. 给每道题定错误等级 severity 和痕迹 mark：\n" +
                "   - 打叉（✗/×/x）、答案明显错、或题后没有任何作答（空白）→ is_wrong=true，severity=\"high\"，mark 分别写「打叉」「错答案」「空白」；\n" +
                "   - 有订正/涂改/改写痕迹但最终答案是对的 → is_wrong=true，severity=\"low\"，mark 写「涂改后对」；\n" +
                "   - 打勾且无涂改 → is_wrong=false，severity=\"\"，mark 写「对」。\n" +
                "4. 只对做错的题，从知识点列表里选最匹配的 knowledge_point_id 和 knowledge_point_name，并写一句可能的误区 misconception。\n" +
                "只输出 JSON：{\"questions\":[{\"question\":\"题目\",\"is_wrong\":true或false,\"severity\":\"high或low或空\",\"mark\":\"...\",\"knowledge_point_id\":\"...\",\"knowledge_point_name\":\"...\",\"misconception\":\"...\"}]}，不要输出多余文字。"

        private const val PRACTICE_PROMPT =
            "你是深圳地区的小学三年级数学老师，熟悉北师大版教材。根据给定知识点，出 3~4 道适合三年级小学生的应用题。要求：\n" +
                "1. 题目必须简短，用一两句话讲清生活场景（例如「小明有 12 个苹果，平均分给 3 个小朋友，每人几个？」「一支铅笔 8 角，买 3 支要多少钱？」），不要啰嗦、不要多余背景、不要长句。\n" +
                "2. 一定要有生活场景，不要出纯算式。\n" +
                "3. 比较大小、判断对错、选单位、选「谁更…」这类答案不好打字的选择题，必须输出 options：给 3~4 个选项（其中一个正确），并把正确选项的文字原样填进 answer；纯计算题（答案就是数字或数字+单位）options 给空数组 []。\n" +
                "4. 每题输出 question（简短应用题）、answer（答案）、options（数组，可空）、solution（解题思路，用 ①②③ 分步，每步一行、每步一句孩子能懂的话）。\n" +
                "只输出 JSON：{\"practice_questions\":[{\"question\":\"题目\",\"answer\":\"答案\",\"options\":[\"选项1\",\"选项2\"],\"solution\":\"解题思路\"}]}，不要输出多余文字。"
    }
}
