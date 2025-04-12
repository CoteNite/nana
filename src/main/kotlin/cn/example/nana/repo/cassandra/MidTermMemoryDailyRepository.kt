package cn.example.nana.repo.cassandra

import cn.example.nana.models.cassandra.MidTermMemoryDaily
import cn.example.nana.models.cassandra.MidTermMemoryDailyKey
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/11 13:11
 */
@Repository
interface MidTermMemoryDailyRepository : CassandraRepository<MidTermMemoryDaily, MidTermMemoryDailyKey>{

    fun findById_SessionIdAndId_SummaryTimestampBetween(sessionId: String, startTime: Instant, endTime: Instant): List<MidTermMemoryDaily>


    @Query("SELECT DISTINCT session_id FROM mid_term_memory_daily WHERE summary_timestamp >= ?0 AND summary_timestamp <= ?1 ALLOW FILTERING")
    fun findDistinctSessionIdsBySummaryTimestampBetween(startDate: Instant, endDate: Instant): List<String>


    @Query("SELECT * FROM mid_term_memory_daily WHERE summary_timestamp >= ?0 AND summary_timestamp <= ?1 ALLOW FILTERING")
    fun findById_SummaryTimestampBetween(startTime: Instant, endTime: Instant): List<MidTermMemoryDaily>

}