package cn.example.nana.models.cassandra

import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.time.Instant

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/15 00:21
 */
@Table("tweet")
data class Tweet(
    @PrimaryKey
    val timestamp: Instant,
    val content:String
)
