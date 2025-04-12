package cn.example.nana.models.cassandra

import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.*
import java.io.Serializable
import java.time.Instant

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/12 20:18
 */
@Table("mid_term_memory_15day")
data class MidTermMemory15Day(
    @PrimaryKey
    var id: MidTermMemory15DayKey,
    @field:Column("summary_level")
    var summaryLevel: String = "15天",
    @field:Column("summary_content")
    var summaryContent: String,
    @field:Column("representative_messages")
    var representativeMessages: List<String>,
    @field:Column("weight")
    var weight: Float,
    @field:Column("embedding_id")
    var embeddingId: String?,
    @field:Column("creation_timestamp")
    var creationTimestamp: Instant = Instant.now()
)

@PrimaryKeyClass
data class MidTermMemory15DayKey(
    @PrimaryKeyColumn(name = "session_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    val sessionId: String,

    @PrimaryKeyColumn(name = "summary_timestamp", ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    val summaryTimestamp: Instant
) : Serializable