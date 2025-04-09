package cn.example.nana.models.graph.entity

import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/10 03:32
 */
@Node("Keyword") // 节点标签为 Keyword
data class KeywordNode(
    // 使用 keyword 字符串本身作为 ID，需要保证其唯一性
    @Id val keyword: String,

    val importance: Double = 1.0,

    // 关系: Keyword --[DESCRIBES]-> Content
    // 从 Keyword 指向 Content
    // 使用 Set 防止重复关系
    @Relationship(type = "DESCRIBES", direction = Relationship.Direction.OUTGOING)
    var describes: MutableSet<ContentNode> = mutableSetOf()
) {
    // 重写 equals 和 hashCode 基于 ID (keyword)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as KeywordNode
        return keyword == other.keyword
    }

    override fun hashCode(): Int {
        return keyword.hashCode()
    }
}