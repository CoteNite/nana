package cn.example.nana.repo.cassandra

import cn.example.nana.models.cassandra.ShortTermMemory
import cn.example.nana.models.cassandra.ShortTermMemoryKey
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/11 13:09
 */
@Repository
interface ShortTermMemoryRepository : CassandraRepository<ShortTermMemory, ShortTermMemoryKey> {
    fun findByPrimaryKey_SessionIdAndPrimaryKey_MessageTimeStampBetween(
        sessionId: String,
        startTime: Instant,
        endTime: Instant
    ): List<ShortTermMemory>


    // 使用 @Query 注解显式查询指定时间范围内的所有记录
    @Query("SELECT * FROM chat_history WHERE message_timestamp >= :startTime AND message_timestamp < :endTime  ALLOW FILTERING")
    fun findByPrimaryKey_MessageTimeStampBetween(
        @Param("startTime") startTime: Instant,
        @Param("endTime") endTime: Instant
    ): List<ShortTermMemory>
}
