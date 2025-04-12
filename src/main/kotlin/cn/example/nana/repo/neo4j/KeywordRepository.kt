package cn.example.nana.repo.neo4j

import cn.example.nana.models.graph.entity.KeywordNode
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.*

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/10 02:11
 */
@Repository
interface KeywordRepository : Neo4jRepository<KeywordNode, String> {
    fun findByKeyword(keyword: String): Optional<KeywordNode>


    @Query("""
        MATCH (c:Content {id: ${'$'}contentId}), (k:Keyword {keyword: ${'$'}targetKeyword})
        MERGE (c)-[:REFERENCES]->(k)
    """)
    fun createReferencesRelationship(contentId: String, targetKeyword: String)
}


