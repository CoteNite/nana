package cn.example.nana.repo.neo4j

import cn.example.nana.models.graph.entity.ContentNode
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.stereotype.Repository

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/10 02:10
 */
@Repository
interface ContentRepository : Neo4jRepository<ContentNode, String> // ID 类型是 String (UUID)