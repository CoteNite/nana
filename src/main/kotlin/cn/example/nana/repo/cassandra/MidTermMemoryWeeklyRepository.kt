package cn.example.nana.repo.cassandra

import cn.example.nana.models.cassandra.MidTermMemoryWeekly
import cn.example.nana.models.cassandra.MidTermMemoryWeeklyKey
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/12 20:23
 */
@Repository
interface MidTermMemoryWeeklyRepository : CassandraRepository<MidTermMemoryWeekly, MidTermMemoryWeeklyKey> {
    fun findById_SessionIdAndId_SummaryTimestampBetween(sessionId: String, startTime: Instant, endTime: Instant): List<MidTermMemoryWeekly>
}