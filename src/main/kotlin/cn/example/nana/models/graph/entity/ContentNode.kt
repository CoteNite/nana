package cn.example.nana.models.graph.entity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
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
    @JsonIgnoreProperties("references")
    @Relationship(type = "REFERENCES", direction = Relationship.Direction.OUTGOING)
    var references: MutableSet<KeywordNode> = mutableSetOf()
)