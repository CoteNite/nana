package cn.example.nana.repo.cassandra

import cn.example.nana.models.cassandra.MidTermMemory15Day
import cn.example.nana.models.cassandra.MidTermMemory15DayKey
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/12 20:23
 */
interface MidTermMemory15DayRepository : CassandraRepository<MidTermMemory15Day, MidTermMemory15DayKey> {

    @Query("SELECT * FROM mid_term_memory_15day WHERE session_id = :sessionId AND summary_timestamp >= :startTime AND summary_timestamp <= :endTime")
    fun findBySessionIdAndSummaryTimestampBetween(
        @Param("sessionId") sessionId: String,
        @Param("startTime") startTime: Instant,
        @Param("endTime") endTime: Instant
    ): List<MidTermMemory15Day>
}