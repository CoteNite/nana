package cn.example.nana.repo.cassandra

import cn.example.nana.models.cassandra.LongTermMemory30Day
import cn.example.nana.models.cassandra.LongTermMemory30DayKey
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.stereotype.Repository
import java.time.Instant

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/12 20:24
 */
@Repository
interface LongTermMemory30DayRepository : CassandraRepository<LongTermMemory30Day, LongTermMemory30DayKey> {
    fun findAllById_SessionIdAndId_SummaryTimestamp(sessionId: String, summaryTimestamp: Instant): List<LongTermMemory30Day>
}