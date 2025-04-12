package cn.example.nana.tools

import cn.example.nana.commons.aop.Slf4j
import cn.example.nana.commons.aop.Slf4j.Companion.log
import cn.example.nana.commons.constants.TextConstants
import cn.example.nana.models.milvus.MemoryStoreIndex
import cn.example.nana.repo.cassandra.MidTermMemoryDailyRepository
import cn.example.nana.repo.milvus.MemoryStoreRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit


/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/11 19:42
 */
@Slf4j
@Service
class MemoryTools(
    private val embeddingModel: EmbeddingModel,
    private val midTermMemoryDailyRepository: MidTermMemoryDailyRepository,
    private val memoryStoreRepo: MemoryStoreRepo
){

    @Tool(description = """
    根据用户当前输入中的关键词检索相关的历史对话片段。
    当用户提出一个你怀疑之前讨论过的话题时使用此工具。
    需要一个名为 'keywords' 的字符串参数，该参数应该从用户的当前输入中提取,用于提取关键信息或关键字。
    返回一个包含相关对话片段的列表。
""")
    fun retrieveRelevantPastConversations(keywords: String): List<String> {
        val embeddings = embeddingModel.embed(keywords)
        if (embeddings.isEmpty()) {
            return emptyList()
        }

        val milvusSearchResults = memoryStoreRepo.searchEmbeddings(embeddings)

        val relevantSummaries = ArrayList<String>()
        relevantSummaries.add(TextConstants.MID_MEMORY_PROMPT)

        val scope = CoroutineScope(Dispatchers.IO)

        for (result in milvusSearchResults) {
            val sessionId = result[MemoryStoreIndex.SESSION_ID] as String
            val memoryAge = result[MemoryStoreIndex.MEMORY_AGE] as String
            val timestampMillis = result[MemoryStoreIndex.TIMESTAMP] as Long
            val summaryTimestamp = Instant.ofEpochMilli(timestampMillis)

            when (memoryAge) {
                "1day" -> {
                    val timeWindowStart = summaryTimestamp.truncatedTo(ChronoUnit.DAYS)
                    val timeWindowEnd = timeWindowStart.plus(1, ChronoUnit.DAYS)
                    val memoriesForSession = midTermMemoryDailyRepository.findById_SessionIdAndId_SummaryTimestampBetween(
                        sessionId,
                        timeWindowStart,
                        timeWindowEnd.minusMillis(1)
                    )
                    memoriesForSession.forEach { memory ->
                        // 异步增加权重并保存
                        scope.launch {
                            try {
                                memory.weight += 0.05f // 可以根据你的需求调整增加的权重值
                                midTermMemoryDailyRepository.save(memory)
                                log.info("异步更新 1 天记忆权重，Session ID: $sessionId, Timestamp: $summaryTimestamp")
                            } catch (e: Exception) {
                                log.error("异步更新 1 天记忆权重失败，Session ID: $sessionId, Timestamp: $summaryTimestamp: ${e.message}", e)
                            }
                        }
                        relevantSummaries.add("[1 Day Memory] ${memory.summaryContent}")
                    }
                }
                "7days" -> {
                    // TODO: 7 天记忆的代码尚未完成，暂时注释掉
                    /*
                    val weekStart = summaryTimestamp.truncatedTo(ChronoUnit.DAYS).minus(summaryTimestamp.dayOfWeek.value.toLong() - 1, ChronoUnit.DAYS)
                    val weekEnd = weekStart.plus(7, ChronoUnit.DAYS).minusMillis(1)
                    midTermMemoryWeeklyRepository.findBySessionIdAndSummaryTimestampBetween(
                        sessionId,
                        weekStart,
                        weekEnd
                    ).forEach { memory ->
                        scope.launch {
                            try {
                                memory.weight += 0.03f // 调整权重增加值
                                midTermMemoryWeeklyRepository.save(memory)
                                log.info("异步更新 7 天记忆权重，Session ID: $sessionId, Timestamp: $summaryTimestamp")
                            } catch (e: Exception) {
                                log.error("异步更新 7 天记忆权重失败，Session ID: $sessionId, Timestamp: $summaryTimestamp: ${e.message}", e)
                            }
                        }
                        relevantSummaries.add("[7 Days Memory] ${memory.summaryContent}")
                    }
                     */
                    log.info("检索到 7 天记忆，但代码尚未完成，已注释")
                }
                "15days" -> {
                    // TODO: 15 天记忆的代码尚未完成，暂时注释掉
                    /*
                    val periodStart = summaryTimestamp.truncatedTo(ChronoUnit.DAYS).minus((summaryTimestamp.dayOfMonth - 1).toLong() % 15, ChronoUnit.DAYS)
                    val periodEnd = periodStart.plus(15, ChronoUnit.DAYS).minusMillis(1)
                    midTermMemory15DayRepository.findBySessionIdAndSummaryTimestampBetween(sessionId, periodStart, periodEnd)
                        .forEach { memory ->
                            scope.launch {
                                try {
                                    memory.weight += 0.02f // 调整权重增加值
                                    midTermMemory15DayRepository.save(memory)
                                    log.info("异步更新 15 天记忆权重，Session ID: $sessionId, Timestamp: $summaryTimestamp")
                                } catch (e: Exception) {
                                    log.error("异步更新 15 天记忆权重失败，Session ID: $sessionId, Timestamp: $summaryTimestamp: ${e.message}", e)
                                }
                            }
                            relevantSummaries.add("[15 Days Memory] ${memory.summaryContent}")
                        }
                     */
                    log.info("检索到 15 天记忆，但代码尚未完成，已注释")
                    relevantSummaries.add("[15 Days Memory Placeholder for Session: $sessionId]")
                }
                "30days" -> {
                    // TODO: 30 天记忆的代码尚未完成，暂时注释掉
                    /*
                    val monthStart = summaryTimestamp.truncatedTo(ChronoUnit.DAYS).withDayOfMonth(1)
                    val monthEnd = monthStart.plus(1, ChronoUnit.MONTH).minusMillis(1)
                    longTermMemory30DayRepository.findBySessionIdAndSummaryTimestampBetween(sessionId, monthStart, monthEnd)
                        .forEach { memory ->
                            scope.launch {
                                try {
                                    memory.weight += 0.01f // 调整权重增加值
                                    longTermMemory30DayRepository.save(memory)
                                    log.info("异步更新 30 天记忆权重，Session ID: $sessionId, Timestamp: $summaryTimestamp")
                                } catch (e: Exception) {
                                    log.error("异步更新 30 天记忆权重失败，Session ID: $sessionId, Timestamp: $summaryTimestamp: ${e.message}", e)
                                }
                            }
                            relevantSummaries.add("[30 Days Memory] ${memory.summaryContent}")
                        }
                     */
                    log.info("检索到 30 天记忆，但代码尚未完成，已注释")
                    relevantSummaries.add("[30 Days Memory Placeholder for Session: $sessionId]")
                }
                else -> {
                    log.warn("未知的 memoryAge: $memoryAge")
                }
            }
        }

        return relevantSummaries.distinct()
    }
}