package cn.example.nana.repo.neo4j

import cn.example.nana.models.graph.entity.ContentNode
import cn.example.nana.query.ContentNodeWithRelevance
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/10 02:10
 */
@Repository
interface ContentRepository : Neo4jRepository<ContentNode, String>{


    // 基础查询 - 直接匹配关键词并计算相关性分数
    @Query("""
    MATCH (k:Keyword)-[:DESCRIBES]->(c:Content)
    WHERE k.keyword IN ${'$'}keywords
    WITH c, k.keyword AS matchedKeyword, count(k) AS keywordMatches
    WITH c, collect(matchedKeyword) AS matchedKeywords, 
         toFloat(keywordMatches) / size(${'$'}keywords) AS relevanceScore
    RETURN c AS contentNode, relevanceScore, matchedKeywords
    ORDER BY relevanceScore DESC
    SKIP ${'$'}skipResults LIMIT ${'$'}limit
""")
    fun findContentNodesByKeywordsWithRelevance(
        @Param("keywords") keywords: List<String>,
        @Param("limit") limit: Int,
        @Param("skipResults") skipResults: Int
    ): List<ContentNodeWithRelevance>

    // 深度查询 - 使用可变深度路径查询，修复了参数使用方式
    @Query("""
    MATCH path = (k1:Keyword)-[:DESCRIBES|RELATED_TO*1..2]->(c:Content)
    WHERE k1.keyword IN ${'$'}keywords
    WITH c, collect(DISTINCT k1.keyword) AS directKeywords, 
         size(collect(DISTINCT k1.keyword)) AS directMatches
    
    // 使用APOC过程来处理可变深度路径
    CALL {
        WITH c
        MATCH (c)-[r:REFERENCES|CONTAINS*1..10]->(relatedContent:Content)<-[:DESCRIBES]-(k2:Keyword)
        WHERE k2.keyword IN ${'$'}keywords AND c <> relatedContent
        RETURN collect(DISTINCT relatedContent) AS relatedContents,
               collect(DISTINCT k2.keyword) AS indirectKeywords
    }
         
    // 计算组合相关性分数
    WITH c AS contentNode, 
         directKeywords + indirectKeywords AS matchedKeywords,
         (toFloat(directMatches) / size(${'$'}keywords)) * 0.7 + 
         (toFloat(size(relatedContents)) / (size(${'$'}keywords) * ${'$'}depth)) * 0.3 AS relevanceScore
         
    RETURN contentNode, relevanceScore, matchedKeywords
    ORDER BY relevanceScore DESC
    SKIP ${'$'}skipResults LIMIT ${'$'}limit
""")
    fun findContentNodesByKeywordsWithDepth(
        @Param("keywords") keywords: List<String>,
        @Param("depth") depth: Int,
        @Param("limit") limit: Int,
        @Param("skipResults") skipResults: Int
    ): List<ContentNodeWithRelevance>



}