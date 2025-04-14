package cn.example.nana.task

import cn.example.nana.commons.aop.Slf4j
import cn.example.nana.commons.aop.Slf4j.Companion.log
import cn.example.nana.models.cassandra.Tweet
import cn.example.nana.repo.cassandra.TweetRepository
import cn.example.nana.service.TweetService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/15 00:16
 */
@Slf4j
@Service
class TweetTask(
    private val tweetService: TweetService,
    private val tweetRepository: TweetRepository,

    ){
    @Scheduled(cron = "0 0 7 * * ?")
    fun performDailyMemoryReinforcement(){
        val tweetContent = tweetService.generateTweet()
        val entity = Tweet(
            timestamp = Instant.now(),
            content = tweetContent
        )
        tweetRepository.save(entity)
        log.info("已将推文保存到 Cassandra，时间戳：${entity.timestamp}")
    }


}