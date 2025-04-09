package cn.example.nana.repo.neo4j

import cn.example.nana.models.graph.entity.KeywordNode
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.stereotype.Repository

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/10 02:11
 */
@Repository
interface KeywordRepository : Neo4jRepository<KeywordNode, String>