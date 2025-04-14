package cn.example.nana.repo.cassandra

import cn.example.nana.models.cassandra.Tweet
import org.springframework.data.cassandra.repository.CassandraRepository
import java.time.Instant

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/15 00:23
 */
interface TweetRepository:CassandraRepository<Tweet, Instant> {



}