package cn.example.nana.models.graph.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import org.springframework.context.annotation.Lazy
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
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "keyword")
@Node("Content")
data class ContentNode(
    @field:Id @field:GeneratedValue(UUIDStringGenerator::class)
    var id: String? = null,
    val text: String,
    val sourceType: String? = null,
    val memoryAge: String? = null,
    val createdTime: Long? = null,


) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ContentNode
        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}