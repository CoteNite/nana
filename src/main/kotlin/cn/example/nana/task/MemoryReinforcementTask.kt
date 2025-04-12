package cn.example.nana.task

import cn.example.nana.command.KnowledgeGraphCommand
import cn.example.nana.commons.aop.Slf4j
import cn.example.nana.commons.aop.Slf4j.Companion.log
import cn.example.nana.models.cassandra.*
import cn.example.nana.repo.cassandra.*
import cn.example.nana.repo.milvus.MemoryStoreRepo
import cn.example.nana.service.AiSummaryService
import kotlinx.coroutines.*
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
@Slf4j
@Service
class MemoryReinforcementTask(
    private val shortTermMemoryRepository: ShortTermMemoryRepository,
    private val midTermMemoryDailyRepository: MidTermMemoryDailyRepository,
    private val midTermMemoryWeeklyRepository: MidTermMemoryWeeklyRepository,
    private val midTermMemory15DayRepository: MidTermMemory15DayRepository,
    private val longTermMemory30DayRepository: LongTermMemory30DayRepository,
    private val aiSummaryService: AiSummaryService,
    private val memoryStoreRepo: MemoryStoreRepo,
    private val embeddingModel: EmbeddingModel,
    private val knowledgeGraphCommand: KnowledgeGraphCommand
){

    private val coroutineDispatcher = Dispatchers.IO


    @Scheduled(cron = "0 0 2 * * ?")
    fun performDailyMemoryReinforcement() = runBlocking {
        log.info("开始执行每日记忆强化任务...")
        val now = Instant.now()
        val zoneOffset = ZoneOffset.of("+08:00")
        val nowDate = now.atOffset(zoneOffset).toLocalDate()

        val yesterday = nowDate.minusDays(1)

        val yesterdayStartInstant = yesterday.atStartOfDay(zoneOffset).toInstant()
        val yesterdayEndInstant = yesterday.atTime(23, 59, 59).toInstant(zoneOffset)

        log.info("处理昨天的会话总结，起始时间：$yesterdayStartInstant，结束时间：$yesterdayEndInstant")

        // 先获取所有 ShortTermMemory 记录，然后提取不同的 sessionId
        val sessions = shortTermMemoryRepository.findByPrimaryKey_MessageTimeStampBetween(yesterdayStartInstant, yesterdayEndInstant)
        val distinctSessionIds = sessions.map { it.primaryKey.sessionId }.distinct()

        val jobs = mutableListOf<Deferred<Unit>>()

        for (sessionId in distinctSessionIds) {
            val job = async(coroutineDispatcher) {
                try {
                    log.info("处理会话 (Coroutine: $sessionId): $sessionId")
                    val conversationHistory = shortTermMemoryRepository.findByPrimaryKey_SessionIdAndPrimaryKey_MessageTimeStampBetween(
                        sessionId,
                        yesterdayStartInstant,
                        yesterdayEndInstant
                    )

                    if (conversationHistory.isNotEmpty()) {
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss") // 定义你想要的时间格式

                        val conversationText = conversationHistory.joinToString("\n") { it ->
                            val timestamp = formatter.format(it.primaryKey.messageTimeStamp.atOffset(zoneOffset))
                            val assistantMessage = it.assistant?.let { "$timestamp AI: $it" } ?: ""
                            val userMessage = it.user?.let { "$timestamp User: $it" } ?: ""
                            listOf(assistantMessage, userMessage).filter { it.isNotBlank() }.joinToString(" ")
                        }.trim()

                        if (conversationText.isNotBlank()) {
                            val summary = aiSummaryService.summarizeConversation(conversationText)
                            val embedding = embeddingModel.embed(summary)

                            // 修改 storeEmbeddingInMilvus 的调用，添加 memoryAge 和 timestamp
                            val embeddingId = memoryStoreRepo.storeEmbeddingInMilvus(
                                embedding = embedding,
                                sessionId = sessionId,
                                memoryAge = "1day",
                                timestamp = yesterdayStartInstant.toEpochMilli()
                            )

                            val midTermMemory = MidTermMemoryDaily(
                                id = MidTermMemoryDailyKey(sessionId, yesterdayStartInstant),
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



    @Scheduled(cron = "0 0 3 ? * MON")
    fun performWeeklyMemoryReinforcement() = runBlocking {
        log.info("开始执行每周记忆强化任务 (从每日记忆总结)...")
        val now = Instant.now()

        val nowDate = now.atOffset(ZoneOffset.of("+08:00")).toLocalDate()
        val dayOfWeek = nowDate.dayOfWeek.value // Monday = 1, Sunday = 7

        val daysToSubtract = if (dayOfWeek == 1) 7 else (dayOfWeek - 1).toLong()

        val lastWeekStart = nowDate.minusDays(daysToSubtract)

        val lastWeekEnd = lastWeekStart.plusDays(6)

        val lastWeekStartInstant = lastWeekStart.atStartOfDay(ZoneOffset.of("+08:00")).toInstant()
        val lastWeekEndInstant = lastWeekEnd.atTime(23, 59, 59).toInstant(ZoneOffset.of("+08:00"))

        log.info("处理过去 7 天的每日会话总结，起始时间：$lastWeekStartInstant，结束时间：$lastWeekEndInstant")


        log.info("处理过去 7 天的每日会话总结，起始时间：$lastWeekStartInstant，结束时间：$lastWeekEndInstant")

        val messagesLastWeek = shortTermMemoryRepository.findByPrimaryKey_MessageTimeStampBetween(lastWeekStartInstant, lastWeekEndInstant)

        // 从所有记录中提取不同的 sessionId
        val sessionsWithMessagesLastWeek = messagesLastWeek.map { it.primaryKey.sessionId }.toSet()

        val jobs = mutableListOf<Deferred<Unit>>()

        for (sessionId in sessionsWithMessagesLastWeek) {
            val job = async(coroutineDispatcher) {
                try {
                    log.info("处理会话 (Coroutine: $sessionId): $sessionId")
                    val dailyMemories = midTermMemoryDailyRepository.findById_SessionIdAndId_SummaryTimestampBetween(
                        sessionId,
                        lastWeekStartInstant,
                        lastWeekEndInstant.plus(1, ChronoUnit.DAYS).minusMillis(1) // 包含上周日
                    )

                    if (dailyMemories.isNotEmpty()) {
                        val conversationText = dailyMemories.joinToString("\n\n") { it.summaryContent }.trim()

                        if (conversationText.isNotBlank()) {
                            val summary = aiSummaryService.summarizeConversation(conversationText)
                            val embedding = embeddingModel.embed(summary)

                            // 使用上周一的日期作为 summaryTimestamp
                            val weekStartForSummary = lastWeekStartInstant.truncatedTo(ChronoUnit.DAYS)
                            val embeddingId = memoryStoreRepo.storeEmbeddingInMilvus(embedding, sessionId, "7days", weekStartForSummary.toEpochMilli())

                            val representativeMessages = dailyMemories.flatMap { it.representativeMessages }.distinct() // 合并代表性消息

                            val midTermMemoryWeekly = MidTermMemoryWeekly(
                                id = MidTermMemoryWeeklyKey(sessionId, weekStartForSummary),
                                summaryContent = summary,
                                representativeMessages = representativeMessages,
                                weight = calculateInitialWeightForWeekly(dailyMemories), // 可以根据每日记忆计算权重
                                embeddingId = embeddingId
                            )
                            midTermMemoryWeeklyRepository.save(midTermMemoryWeekly)
                            log.info("会话 $sessionId 的每周总结已保存到 Cassandra (Coroutine: $sessionId).")

                            knowledgeGraphCommand.storeWeeklyMemoryInNeo4j(
                                summaryContent = summary,
                                memoryAge = "7days",
                                createdTime = weekStartForSummary.toEpochMilli(),
                                sessionId=sessionId
                            )
                            log.info("会话 $sessionId 的每周总结已发送到 Neo4j 进行存储 (Coroutine: $sessionId).")

                        } else {
                            log.info("会话 $sessionId 在过去 7 天没有有效的每日总结 (Coroutine: $sessionId).")
                        }
                    } else {
                        log.info("会话 $sessionId 在过去 7 天没有每日总结记录 (Coroutine: $sessionId).")
                    }
                } catch (e: Exception) {
                    log.error("处理会话 $sessionId 时发生错误 (Coroutine: $sessionId): ${e.message}", e)
                }
            }
            jobs.add(job)
        }

        jobs.awaitAll()
        log.info("每周记忆强化任务完成 (从每日记忆总结).")
    }




    // 计划每月 1 号和 16 号凌晨 4 点执行
    @Scheduled(cron = "0 0 4 1,16 * ?")
    fun performFifteenDayMemoryReinforcement() = runBlocking {
        log.info("开始执行 15 天记忆强化任务 (从每日记忆总结)...")
        val now = Instant.now()

        val nowDate = now.atOffset(ZoneOffset.of("+08:00")).toLocalDate()

        val fifteenDaysAgoEnd = nowDate.minusDays(1) // 昨天
        val fifteenDaysAgoStart = fifteenDaysAgoEnd.minusDays(14) // 15 天前

        val fifteenDaysAgoStartInstant = fifteenDaysAgoStart.atStartOfDay(ZoneOffset.of("+08:00")).toInstant()
        val fifteenDaysAgoEndInstant = fifteenDaysAgoEnd.atTime(23, 59, 59).toInstant(ZoneOffset.of("+08:00"))

        log.info("处理过去 15 天的每日会话总结，起始时间：$fifteenDaysAgoStartInstant，结束时间：$fifteenDaysAgoEndInstant")

        // Fetch all daily memories within the 15-day range
        val allDailyMemories = midTermMemoryDailyRepository.findById_SummaryTimestampBetween(fifteenDaysAgoStartInstant, fifteenDaysAgoEndInstant)

        // Extract distinct session IDs in the application
        val sessionsWithDailyMemories = allDailyMemories.map { it.id.sessionId }.distinct()

        val jobs = mutableListOf<Deferred<Unit>>()

        for (sessionId in sessionsWithDailyMemories) {
            val job = async(coroutineDispatcher) {
                try {
                    log.info("处理会话 (Coroutine: $sessionId): $sessionId")
                    val dailyMemories = midTermMemoryDailyRepository.findById_SessionIdAndId_SummaryTimestampBetween(
                        sessionId,
                        fifteenDaysAgoStartInstant,
                        fifteenDaysAgoEndInstant.plus(1, ChronoUnit.DAYS).minusMillis(1) // 包含结束日
                    )

                    if (dailyMemories.isNotEmpty()) {
                        val conversationText = dailyMemories.joinToString("\n\n") { it.summaryContent }.trim()

                        if (conversationText.isNotBlank()) {
                            val summary = aiSummaryService.summarizeConversation(conversationText)
                            val embedding = embeddingModel.embed(summary)

                            // 计算 15 天的起始时间戳作为 summaryTimestamp
                            val fifteenDayStartForSummary = fifteenDaysAgoStartInstant.truncatedTo(ChronoUnit.DAYS)
                            val embeddingId = memoryStoreRepo.storeEmbeddingInMilvus(embedding, sessionId, "15days", fifteenDayStartForSummary.toEpochMilli())

                            val representativeMessages = dailyMemories.flatMap { it.representativeMessages }.distinct() // 合并代表性消息

                            val midTermMemoryFifteenDay = MidTermMemory15Day( // 假设你已经创建了这个实体类
                                id = MidTermMemory15DayKey(sessionId, fifteenDayStartForSummary), // 假设你已经创建了这个 Key 类
                                summaryContent = summary,
                                representativeMessages = representativeMessages,
                                weight = calculateInitialWeightForFifteenDay(dailyMemories), // 需要实现这个权重计算方法
                                embeddingId = embeddingId
                            )
                            midTermMemory15DayRepository.save(midTermMemoryFifteenDay)
                            log.info("会话 $sessionId 的 15 天总结已保存到 Cassandra (Coroutine: $sessionId).")

                            knowledgeGraphCommand.storeFifteenDayMemoryInNeo4j(
                                summaryContent = summary,
                                memoryAge = "15days",
                                createdTime = fifteenDayStartForSummary.toEpochMilli(),
                                sessionId = sessionId // 补上了 sessionId 参数
                            )
                            log.info("会话 $sessionId 的 15 天总结已发送到 Neo4j 进行存储 (Coroutine: $sessionId).")

                        } else {
                            log.info("会话 $sessionId 在过去 15 天没有有效的每日总结 (Coroutine: $sessionId).")
                        }
                    } else {
                        log.info("会话 $sessionId 在过去 15 天没有每日总结记录 (Coroutine: $sessionId).")
                    }
                } catch (e: Exception) {
                    log.error("处理会话 $sessionId 时发生错误 (Coroutine: $sessionId): ${e.message}", e)
                }
            }
            jobs.add(job)
        }
        jobs.awaitAll()
        log.info("15 天记忆强化任务完成 (从每日记忆总结).")
    }

    @Scheduled(cron = "0 0 4 1 * ?")
    fun performThirtyDayMemoryReinforcement() = runBlocking {
        log.info("开始执行 30 天记忆强化任务 (从每日记忆总结)...")
        val now = Instant.now()
        val nowDate = now.atOffset(ZoneOffset.of("+08:00")).toLocalDate()

        val thirtyDaysAgoEnd = nowDate.minusDays(1) // Yesterday
        val thirtyDaysAgoStart = thirtyDaysAgoEnd.minusDays(29) // 30 days ago

        val thirtyDaysAgoStartInstant = thirtyDaysAgoStart.atStartOfDay(ZoneOffset.of("+08:00")).toInstant()
        val thirtyDaysAgoEndInstant = thirtyDaysAgoEnd.atTime(23, 59, 59).toInstant(ZoneOffset.of("+08:00"))

        log.info("处理过去 30 天的每日会话总结，起始时间：$thirtyDaysAgoStartInstant，结束时间：$thirtyDaysAgoEndInstant")

        // Fetch all daily memories within the 30-day range
        val allDailyMemories = midTermMemoryDailyRepository.findById_SummaryTimestampBetween(thirtyDaysAgoStartInstant, thirtyDaysAgoEndInstant)

        // Extract distinct session IDs in the application
        val sessionsWithDailyMemories = allDailyMemories.map { it.id.sessionId }.distinct()

        val jobs = mutableListOf<Deferred<Unit>>()

        for (sessionId in sessionsWithDailyMemories) {
            val job = async(coroutineDispatcher) {
                try {
                    log.info("处理会话 (Coroutine: $sessionId): $sessionId")
                    val dailyMemoriesForSession = midTermMemoryDailyRepository.findById_SessionIdAndId_SummaryTimestampBetween(
                        sessionId,
                        thirtyDaysAgoStartInstant,
                        thirtyDaysAgoEndInstant.plus(1, ChronoUnit.DAYS).minusMillis(1) // Include end day
                    )

                    if (dailyMemoriesForSession.isNotEmpty()) {
                        val conversationText = dailyMemoriesForSession.joinToString("\n\n") { it.summaryContent }.trim()

                        if (conversationText.isNotBlank()) {
                            val summary = aiSummaryService.summarizeConversation(conversationText)
                            val embedding = embeddingModel.embed(summary)

                            val thirtyDayStartForSummary = thirtyDaysAgoStartInstant.truncatedTo(ChronoUnit.DAYS)
                            val embeddingId = memoryStoreRepo.storeEmbeddingInMilvus(embedding, sessionId, "30days", thirtyDayStartForSummary.toEpochMilli())

                            val representativeMessages = dailyMemoriesForSession.flatMap { it.representativeMessages }.distinct()

                            // Assuming you have a LongTermMemory30Day entity and key
                            val midTermMemoryThirtyDay = LongTermMemory30Day(
                                id = LongTermMemory30DayKey(sessionId, thirtyDayStartForSummary),
                                summaryContent = summary,
                                representativeMessages = representativeMessages,
                                weight = calculateInitialWeightForThirtyDay(dailyMemoriesForSession),
                                embeddingId = embeddingId
                            )
                            longTermMemory30DayRepository.save(midTermMemoryThirtyDay)
                            log.info("会话 $sessionId 的 30 天总结已保存到 Cassandra (Coroutine: $sessionId).")

                            knowledgeGraphCommand.storeThirtyDayMemoryInNeo4j(
                                summaryContent = summary,
                                memoryAge = "30days",
                                createdTime = thirtyDayStartForSummary.toEpochMilli(),
                                sessionId = sessionId
                            )
                            log.info("会话 $sessionId 的 30 天总结已发送到 Neo4j 进行存储 (Coroutine: $sessionId).")

                        } else {
                            log.info("会话 $sessionId 在过去 30 天没有有效的每日总结 (Coroutine: $sessionId).")
                        }
                    } else {
                        log.info("会话 $sessionId 在过去 30 天没有每日总结记录 (Coroutine: $sessionId).")
                    }
                } catch (e: Exception) {
                    log.error("处理会话 $sessionId 时发生错误 (Coroutine: $sessionId): ${e.message}", e)
                }
            }
            jobs.add(job)
        }
        jobs.awaitAll()
        log.info("30 天记忆强化任务完成 (从每日记忆总结).")
    }


    private fun calculateInitialWeight(conversationHistory: List<ShortTermMemory>): Float {
        val totalMessages = conversationHistory.size
        val userMessages = conversationHistory.count { it.user != null }
        val aiMessages = totalMessages - userMessages

        var weight = 0f

        weight += totalMessages * 0.03f
        weight += userMessages * 0.1f
        weight += aiMessages * 0.05f


        conversationHistory.forEach { memory ->
            weight += (memory.user?.length ?: 0) * 0.0008f
            weight += (memory.assistant?.length ?: 0) * 0.0004f
        }

        return weight.coerceAtLeast(0.1f)
    }




    private fun calculateInitialWeightForWeekly(dailyMemories: List<MidTermMemoryDaily>): Float {
        var weight = 0f
        dailyMemories.forEach {
            weight += it.weight
        }
        return weight.coerceAtLeast(0.1f)
    }


    private fun calculateInitialWeightForFifteenDay(dailyMemories: List<MidTermMemoryDaily>): Float {
        var weight = 0f
        dailyMemories.forEach {
            weight += it.weight
        }
        return weight.coerceAtLeast(0.1f)
    }

    private fun calculateInitialWeightForThirtyDay(dailyMemories: List<MidTermMemoryDaily>): Float {
        return if (dailyMemories.isNotEmpty()) {
            dailyMemories.sumOf { it.weight.toDouble() }.toFloat() / dailyMemories.size
        } else {
            0.5f
        }
    }


}