package com.mathhelper.app.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// OpenAI 兼容 Chat Completions 协议的请求/响应 DTO（DeepSeek 等都用这套）。

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class ResponseFormat(val type: String)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.3,
    @SerialName("response_format") val responseFormat: ResponseFormat = ResponseFormat("json_object")
)

@Serializable
data class ChatCompletionResponse(val choices: List<Choice> = emptyList())

@Serializable
data class Choice(val message: MessageContent = MessageContent(""))

@Serializable
data class MessageContent(val content: String)

// 归因结果：AI 返回的结构化 JSON。

@Serializable
data class AttributionResult(
    @SerialName("knowledge_point_id") val knowledgePointId: String = "",
    @SerialName("knowledge_point_name") val knowledgePointName: String = "",
    val misconception: String = "",
    val explanation: String = "",
    @SerialName("practice_questions") val practiceQuestions: List<PracticeQuestionDto> = emptyList()
)

@Serializable
data class PracticeQuestionDto(
    val question: String = "",
    val answer: String = "",
    val options: List<String> = emptyList(),
    val solution: String = ""
)

@Serializable
data class PracticeQuestionsResult(
    @SerialName("practice_questions") val practiceQuestions: List<PracticeQuestionDto> = emptyList()
)

// 整页分析：把一页作业/试卷拆成多道题，判断对错、归因错题。

@Serializable
data class PageAnalysisResult(
    @SerialName("questions") val questions: List<QuestionAnalysis> = emptyList()
)

@Serializable
data class QuestionAnalysis(
    val question: String = "",
    @SerialName("is_wrong") val isWrong: Boolean = false,
    val severity: String = "high",   // 错误等级：high=重点巩固 / low=轻微·后续加练
    val mark: String = "",           // 痕迹/原因：打叉 / 空白 / 错答案 / 涂改后对 / 对
    @SerialName("knowledge_point_id") val knowledgePointId: String = "",
    @SerialName("knowledge_point_name") val knowledgePointName: String = "",
    val misconception: String = ""
)
