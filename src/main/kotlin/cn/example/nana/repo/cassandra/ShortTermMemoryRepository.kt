package cn.example.nana.repo.cassandra

import cn.example.nana.models.cassandra.ShortTermMemory
import cn.example.nana.models.cassandra.ShortTermMemoryKey
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query
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

    @Query("SELECT DISTINCT primaryKey.sessionId FROM ShortTermMemory WHERE messageTimeStamp >= :startTime AND messageTimeStamp < :endTime")
    fun findDistinctSessionIdsWithMessagesBetween(startTime: Instant, endTime: Instant): List<String>
}
