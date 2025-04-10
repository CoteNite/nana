package cn.example.nana.task

import cn.example.nana.commons.aop.Slf4j.Companion.log
import cn.example.nana.models.cassandra.MidTermMemoryDaily
import cn.example.nana.models.cassandra.MidTermMemoryDailyKey
import cn.example.nana.models.cassandra.ShortTermMemory
import cn.example.nana.repo.cassandra.MidTermMemoryDailyRepository
import cn.example.nana.repo.cassandra.ShortTermMemoryRepository
import cn.example.nana.repo.milvus.DailyMemoryRepo
import cn.example.nana.service.AiSummaryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/11 13:19
 */
@Service
class DailyMemoryReinforcementTask(
    private val shortTermMemoryRepository: ShortTermMemoryRepository,
    private val midTermMemoryDailyRepository: MidTermMemoryDailyRepository,
    private val aiSummaryService: AiSummaryService,
    private val dailyMemoryRepo: DailyMemoryRepo,
    private val embeddingModel: EmbeddingModel
){

    private val coroutineDispatcher = Dispatchers.IO // 适用于 IO 密集型任务

    @Scheduled(cron = "0 0 2 * * ?")
    fun performDailyMemoryReinforcement() = runBlocking {
        log.info("开始执行每日记忆强化任务...")
        val now = Instant.now()
        val yesterdayStart = now.minus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS)
        val yesterdayEnd = yesterdayStart.plus(1, ChronoUnit.DAYS)

        val sessions = shortTermMemoryRepository.findDistinctSessionIdsWithMessagesBetween(yesterdayStart, yesterdayEnd)
        val jobs = mutableListOf<kotlinx.coroutines.Deferred<Unit>>()

        for (sessionId in sessions) {
            val job = async(coroutineDispatcher) {
                try {
                    log.info("处理会话 (Coroutine: $sessionId): $sessionId")
                    val conversationHistory = shortTermMemoryRepository.findByPrimaryKey_SessionIdAndPrimaryKey_MessageTimeStampBetween(
                        sessionId,
                        yesterdayStart,
                        yesterdayEnd
                    )

                    if (conversationHistory.isNotEmpty()) {
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") // 定义你想要的时间格式

                        val conversationText = conversationHistory.joinToString("\n") { it ->
                            val timestamp = formatter.format(it.primaryKey.messageTimeStamp.atOffset(ZoneOffset.of("+08:00"))) // 假设你的时间是 UTC+8，请根据你的实际时区调整
                            val assistantMessage = it.assistant?.let { "$timestamp AI: $it" } ?: ""
                            val userMessage = it.user?.let { "$timestamp User: $it" } ?: ""
                            listOf(assistantMessage, userMessage).filter { it.isNotBlank() }.joinToString(" ")
                        }.trim()

                        if (conversationText.isNotBlank()) {
                            val summary = aiSummaryService.summarizeConversation(conversationText)
                            val embedding = embeddingModel.embed(summary)

                            val embeddingId = dailyMemoryRepo.storeEmbeddingInMilvus(embedding, sessionId)

                            val midTermMemory = MidTermMemoryDaily(
                                id = MidTermMemoryDailyKey(sessionId, yesterdayStart),
                                summaryContent = summary,
                                representativeMessages = conversationHistory.mapNotNull { memory: ShortTermMemory ->
                                    memory.assistant ?: memory.user
                                },
                                weight = calculateInitialWeight(conversationHistory),
                                embeddingId = embeddingId
                            )
                            midTermMemoryDailyRepository.save(midTermMemory)
                            log.info("会话 $sessionId 的每日总结已保存 (Coroutine: $sessionId).")
                        } else {
                            log.info("会话 $sessionId 在昨天没有对话记录 (Coroutine: $sessionId).")
                        }
                    } else {
                        log.info("会话 $sessionId 在昨天没有对话记录 (Coroutine: $sessionId).")
                    }
                } catch (e: Exception) {
                    log.error("处理会话 $sessionId 时发生错误 (Coroutine: $sessionId): ${e.message}", e)
                }
            }
            jobs.add(job)
        }

        // 等待所有协程完成
        jobs.awaitAll()

        log.info("每日记忆强化任务完成。")
    }

    private fun calculateInitialWeight(conversationHistory: List<ShortTermMemory>): Float {
        return conversationHistory.size.toFloat() * 0.1f
    }


}