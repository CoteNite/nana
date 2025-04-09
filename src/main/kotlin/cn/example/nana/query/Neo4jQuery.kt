package cn.example.nana.query

import cn.example.nana.command.InputJsonEntry
import cn.example.nana.commons.constants.TextConstants
import cn.example.nana.commons.enums.Errors
import cn.example.nana.commons.exception.BusinessException
import cn.example.nana.models.graph.entity.ContentNode
import cn.example.nana.repo.neo4j.ContentRepository
import cn.example.nana.repo.neo4j.KeywordRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.stereotype.Service

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/10 04:46
 */
interface Neo4jQuery {
    fun findRelatedContentBySummaryUsingKeywords(
        summary: String,
        depth: Int = 2,
        limit: Int = 50,
        skipResults: Int = 0,
        minRelevanceScore: Double = 0.3
    ): List<ContentNodeWithRelevance>
}


@Service
class Neo4jQueryImpl(
    private val contentRepository: ContentRepository,
    private val openAiChatModel: OpenAiChatModel
): Neo4jQuery {

    private val objectMapper = jacksonObjectMapper()

    override fun findRelatedContentBySummaryUsingKeywords(
        summary: String,
        depth: Int,
        limit: Int,
        skipResults: Int,
        minRelevanceScore: Double
    ): List<ContentNodeWithRelevance> {
        // 1. 提取关键词
        val jsonStr = ChatClient.create(openAiChatModel)
            .prompt(TextConstants.buildKeyWorld4GraphPrompt(summary))
            .call()
            .content() ?: throw BusinessException(Errors.CHAT_ERROR)

        val keywords = objectMapper.readValue<List<String>>(jsonStr)

        println("从 Summary 提取的关键字: $keywords") // 调试输出

        if (keywords.isEmpty()) {
            println("未能从 Summary 中提取到关键字。")
            return emptyList()
        }

        // 2. 使用深度优先搜索查询相关内容
        val relatedContent = if (depth <= 1) {
            // 基础查询 - 直接关联的内容
            contentRepository.findContentNodesByKeywordsWithRelevance(keywords, limit, skipResults)
        } else {
            // 深度查询 - 使用路径查询获取更深层次的关联
            contentRepository.findContentNodesByKeywordsWithDepth(keywords, depth, limit, skipResults)
        }

        // 3. 过滤低相关性的结果
        return relatedContent.filter { it.relevanceScore >= minRelevanceScore }
    }

}


data class ContentNodeWithRelevance(
    val contentNode: ContentNode,
    val relevanceScore: Double,
    val matchedKeywords: List<String>
)
