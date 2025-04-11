package cn.example.nana.models.cassandra

import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import org.springframework.data.cassandra.core.mapping.Table
import java.io.Serializable
import java.time.Instant

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/11 13:01
 */
@Table("chat_history")
data class ShortTermMemory(
    @PrimaryKey
    val primaryKey: ShortTermMemoryKey,
    val assistant: String?,
    val user: String?
)

@PrimaryKeyClass
data class ShortTermMemoryKey(
    @PrimaryKeyColumn(name = "session_id",  type = PrimaryKeyType.PARTITIONED)
    val sessionId: String,

    @PrimaryKeyColumn(name = "message_timestamp", type = PrimaryKeyType.CLUSTERED)
    val messageTimeStamp: Instant
) : Serializable

