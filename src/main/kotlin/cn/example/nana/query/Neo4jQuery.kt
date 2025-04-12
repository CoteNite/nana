package cn.example.nana.query

import cn.example.nana.models.graph.entity.ContentNode
import cn.example.nana.repo.neo4j.ContentRepository
import org.springframework.stereotype.Service

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/10 04:46
 */
interface Neo4jQuery {
    fun findRelatedContentBySummaryUsingKeywords(
        summary: String,
        keywords: List<String>, // 添加 keywords 参数
        depth: Int = 2,
        limit: Int = 50,
        skipResults: Int = 0,
        minRelevanceScore: Double = 0.3
    ): List<ContentNodeWithRelevance>
}


@Service
class Neo4jQueryImpl(
    private val contentRepository: ContentRepository,
): Neo4jQuery {

    override fun findRelatedContentBySummaryUsingKeywords(
        summary: String,
        keywords: List<String>, // 接收关键词列表作为参数
        depth: Int,
        limit: Int,
        skipResults: Int,
        minRelevanceScore: Double
    ): List<ContentNodeWithRelevance> {
        println("接收到的关键字: $keywords") // 调试输出

        if (keywords.isEmpty()) {
            println("接收到的关键字列表为空。")
            return emptyList()
        }

        val relatedContent = if (depth <= 1) {
            contentRepository.findContentNodesByKeywordsWithRelevance(keywords, limit, skipResults)
        } else {
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
