package cn.example.nana.repo.cassandra

import cn.example.nana.models.cassandra.MidTermMemoryDaily
import cn.example.nana.models.cassandra.MidTermMemoryDailyKey
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.stereotype.Repository

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/11 13:11
 */
@Repository
interface MidTermMemoryDailyRepository : CassandraRepository<MidTermMemoryDaily, MidTermMemoryDailyKey>
