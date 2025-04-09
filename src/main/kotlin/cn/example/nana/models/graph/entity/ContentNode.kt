package cn.example.nana.models.graph.entity

import org.springframework.data.neo4j.core.schema.GeneratedValue
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship
import org.springframework.data.neo4j.core.support.UUIDStringGenerator

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/10 03:32
 */
@Node("Content") // 节点标签为 Content
data class ContentNode(
    // 使用自动生成的 UUID 作为 ID，因为 content 文本可能较长或不唯一
    @Id @GeneratedValue(UUIDStringGenerator::class)
    var id: String? = null,

    val text: String, // 存储 content 的内容

    // 关系: Content --[REFERENCES]-> Keyword
    // 从 Content 指向 `to` 列表中的 Keyword
    // 使用 Set 防止重复关系
    @Relationship(type = "REFERENCES", direction = Relationship.Direction.OUTGOING)
    var references: MutableSet<KeywordNode> = mutableSetOf()
) {
    // 注意：默认的 data class equals/hashCode 基于所有主构造函数属性。
    // 如果需要基于 ID 判断唯一性，可以覆盖 equals/hashCode，
    // 但对于自动生成的 ID，默认行为通常是合适的。
    // 如果 text 需要唯一，应考虑将 text 作为 ID 或添加唯一约束。
    // 按当前需求，每个 JSON 条目的 content 似乎是独立的节点，用 UUID 合适。
}