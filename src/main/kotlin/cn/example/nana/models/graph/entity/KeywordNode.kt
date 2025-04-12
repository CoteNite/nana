package cn.example.nana.models.graph.entity

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import com.fasterxml.jackson.annotation.JsonManagedReference
import org.springframework.context.annotation.Lazy
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.schema.Relationship


/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/10 03:32
 */
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "content")
@Node("Keyword")
data class KeywordNode(
    @field:Id
    val keyword: String,

    val importance: Double = 1.0,

    @field:JsonIgnoreProperties("references")
    @field:Relationship(type = "DESCRIBES", direction = Relationship.Direction.OUTGOING)
    @field:Lazy
    @field:JsonManagedReference("keyword-describes")
    var describes: MutableSet<ContentNode> = mutableSetOf()
) {
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