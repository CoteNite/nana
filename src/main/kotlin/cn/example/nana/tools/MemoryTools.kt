package cn.example.nana.tools

import cn.example.nana.commons.aop.Slf4j
import cn.example.nana.commons.aop.Slf4j.Companion.log
import cn.example.nana.commons.constants.RedisKeyBuilder
import cn.example.nana.commons.constants.TextConstants
import cn.example.nana.models.milvus.MemoryStoreIndex
import cn.example.nana.query.Neo4jQuery
import cn.example.nana.repo.cassandra.LongTermMemory30DayRepository
import cn.example.nana.repo.cassandra.MidTermMemory15DayRepository
import cn.example.nana.repo.cassandra.MidTermMemoryDailyRepository
import cn.example.nana.repo.cassandra.MidTermMemoryWeeklyRepository
import cn.example.nana.repo.milvus.MemoryStoreRepo
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.redisson.api.RedissonClient
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.tool.annotation.Tool
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import com.fasterxml.jackson.core.type.TypeReference


/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/11 19:42
 */
@Slf4j
@Service
class MemoryTools(
    private val embeddingModel: EmbeddingModel,
    private val memoryStoreRepo: MemoryStoreRepo,
    private val neo4jQuery: Neo4jQuery,
    private val redissonClient: RedissonClient, // 注入 RedissonClient
    private val openAiChatModel: OpenAiChatModel, // 注入 OpenAiChatModel
    private val midTermMemoryDailyRepository: MidTermMemoryDailyRepository,
    private val midTermMemoryWeeklyRepository: MidTermMemoryWeeklyRepository,
    private val midTermMemory15DayRepository: MidTermMemory15DayRepository,
    private val longTermMemory30DayRepository: LongTermMemory30DayRepository,
){

    private val objectMapper = jacksonObjectMapper()



    @Tool(description = """
        当用户提到过去的事件或词汇(之前，昨天等),
        或者和记忆有关的事件和词汇(你还记得，你记得吗等)时， 
        根据用户当前输入中的关键词检索相关的历史对话片段。
        当用户提出一个你怀疑之前讨论过的话题时使用此工具。
        需要一个名为 'keywords' 的字符串参数，该参数应该从用户的当前输入中提取，可以是一段包含关键信息的文本或关键字。
        返回一个包含相关对话片段的列表，每个片段包含内容和权重。
    """)
    fun retrieveRelevantPastConversations(keywords: String): List<MemoryWithWeight> {
        val embeddings = embeddingModel.embed(keywords)
        if (embeddings.isEmpty()) {
            return emptyList()
        }

        val milvusSearchResults = memoryStoreRepo.searchEmbeddings(embeddings)

        val relevantMemoriesWithWeight = ArrayList<MemoryWithWeight>()
        relevantMemoriesWithWeight.add(MemoryWithWeight(TextConstants.MID_MEMORY_PROMPT, 1.0f))

        val scope = CoroutineScope(Dispatchers.IO)

        for (result in milvusSearchResults) {
            val sessionId = result[MemoryStoreIndex.SESSION_ID] as String
            val memoryAge = result[MemoryStoreIndex.MEMORY_AGE] as String
            val timestampMillis = result[MemoryStoreIndex.TIMESTAMP] as Long
            val summaryTimestamp = Instant.ofEpochMilli(timestampMillis)

            var baseMemoryContent: String? = null

            when (memoryAge) {
                "1day" -> {
                    val timeWindowStart = summaryTimestamp.truncatedTo(ChronoUnit.DAYS)
                    val timeWindowEnd = timeWindowStart.plus(1, ChronoUnit.DAYS)
                    midTermMemoryDailyRepository.findById_SessionIdAndId_SummaryTimestampBetween(
                        sessionId,
                        timeWindowStart,
                        timeWindowEnd.minusMillis(1)
                    ).firstOrNull()?.let { memory ->
                        baseMemoryContent = memory.summaryContent
                        scope.launch {
                            try {
                                memory.weight += 0.05f
                                midTermMemoryDailyRepository.save(memory)
                                log.info("异步更新 1 天记忆权重，Session ID: $sessionId, Timestamp: $summaryTimestamp")
                            } catch (e: Exception) {
                                log.error("异步更新 1 天记忆权重失败，Session ID: $sessionId, Timestamp: $summaryTimestamp: ${e.message}", e)
                            }
                        }
                        relevantMemoriesWithWeight.add(MemoryWithWeight(memory.summaryContent, memory.weight + 0.2f))
                    }
                }
                "7days" -> {
                    val zonedDateTime = summaryTimestamp.atOffset(ZoneOffset.of("+08:00")).toZonedDateTime()
                    val weekStart = zonedDateTime.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toInstant()
                    val weekEnd = weekStart.plus(7, ChronoUnit.DAYS).minusMillis(1)

                    midTermMemoryWeeklyRepository.findById_SessionIdAndId_SummaryTimestampBetween(
                        sessionId,
                        weekStart,
                        weekEnd
                    ).firstOrNull()?.let { memory ->
                        baseMemoryContent = memory.summaryContent
                        scope.launch {
                            try {
                                memory.weight += 0.03f
                                midTermMemoryWeeklyRepository.save(memory)
                                log.info("异步更新 7 天记忆权重，Session ID: $sessionId, Timestamp: $summaryTimestamp")
                            } catch (e: Exception) {
                                log.error("异步更新 7 天记忆权重失败，Session ID: $sessionId, Timestamp: $summaryTimestamp: ${e.message}", e)
                            }
                        }
                        relevantMemoriesWithWeight.add(MemoryWithWeight(memory.summaryContent, memory.weight + 0.1f))
                    }
                }
                "15days" -> {
                    val fifteenDayStartForSummary = summaryTimestamp.truncatedTo(ChronoUnit.DAYS)
                    val fifteenDayEndForSummary = fifteenDayStartForSummary.plus(15, ChronoUnit.DAYS).minusMillis(1)

                    midTermMemory15DayRepository.findBySessionIdAndSummaryTimestampBetween(
                        sessionId,
                        fifteenDayStartForSummary,
                        fifteenDayEndForSummary
                    ).firstOrNull()?.let { memory ->
                        baseMemoryContent = memory.summaryContent
                        scope.launch {
                            try {
                                memory.weight += 0.02f
                                midTermMemory15DayRepository.save(memory)
                                log.info("异步更新 15 天记忆权重，Session ID: $sessionId, Timestamp: $summaryTimestamp")
                            } catch (e: Exception) {
                                log.error("异步更新 15 天记忆权重失败，Session ID: $sessionId, Timestamp: $summaryTimestamp: ${e.message}", e)
                            }
                        }
                        relevantMemoriesWithWeight.add(MemoryWithWeight(memory.summaryContent, memory.weight + 0.05f))
                    }
                }
                "30days" -> {
                    val thirtyDayStartForSummary = summaryTimestamp.truncatedTo(ChronoUnit.DAYS)
                    longTermMemory30DayRepository.findAllById_SessionIdAndId_SummaryTimestamp(
                        sessionId,
                        thirtyDayStartForSummary
                    ).firstOrNull()?.let { memory ->
                        baseMemoryContent = memory.summaryContent
                        scope.launch {
                            try {
                                memory.weight += 0.01f // 可以根据需要调整权重增加值
                                longTermMemory30DayRepository.save(memory)
                                log.info("异步更新 30 天记忆权重，Session ID: $sessionId, Timestamp: $summaryTimestamp")
                            } catch (e: Exception) {
                                log.error("异步更新 30 天记忆权重失败，Session ID: $sessionId, Timestamp: $summaryTimestamp: ${e.message}", e)
                            }
                        }
                        relevantMemoriesWithWeight.add(MemoryWithWeight(memory.summaryContent, memory.weight + 0.02f)) // 可以根据需要调整基础权重增加值
                    } ?: run {
                        log.info("未找到 Session ID: $sessionId 在 $thirtyDayStartForSummary 的 30 天记忆")
                        relevantMemoriesWithWeight.add(MemoryWithWeight("[30 Days Memory Not Found for Session: $sessionId]", 0.5f + 0.02f))
                    }
                }
                else -> {
                    log.warn("未知的 memoryAge: $memoryAge")
                }
            }

            baseMemoryContent?.let { content ->
                // 从 Redis 获取当前会话的历史关键词 Set
                val historicalKeywordsSet = redissonClient.getSet<String>(RedisKeyBuilder.buildSessionKeywordsKey(sessionId))

                // 将初始关键词和历史关键词 Set 合并
                val combinedKeywords = mutableSetOf<String>()
                combinedKeywords.addAll(keywords.split(Regex("\\s+"))) // 将初始关键词按空格分割
                combinedKeywords.addAll(historicalKeywordsSet)
                val combinedKeywordsString = combinedKeywords.joinToString(", ") // 将合并后的关键词转换为字符串

                // 使用 LLM 优化关键词
                val keywordRefinementPrompt = TextConstants.buildKeywordRefinementPrompt(keywords, combinedKeywordsString) // 你需要在 TextConstants 中定义这个 Prompt

                val refinedKeywordsJson = ChatClient.create(openAiChatModel)
                    .prompt(keywordRefinementPrompt)
                    .call()
                    .content() ?: keywords // 如果优化失败，则使用原始关键词

                val refinedKeywordsList = try {
                    objectMapper.readValue(refinedKeywordsJson, object : TypeReference<List<String>>() {})
                } catch (e: Exception) {
                    log.error("解析优化后的关键词失败: ${e.message}", e)
                    keywords.split(Regex("\\s+")) // 解析失败也使用原始关键词
                }

                // 使用优化后的关键词查询 Neo4j
                val relatedContentFromNeo4j = neo4jQuery.findRelatedContentBySummaryUsingKeywords(content, refinedKeywordsList, depth = 20)

                val enrichedContent = if (relatedContentFromNeo4j.isNotEmpty()) {
                    "$content\n\n**相关知识：**\n${relatedContentFromNeo4j.joinToString("\n") { "- ${it.contentNode.text}" }}"
                } else {
                    content
                }
                relevantMemoriesWithWeight.find { it.content.contains(content) }?.let {
                    val index = relevantMemoriesWithWeight.indexOf(it)
                    relevantMemoriesWithWeight[index] = it.copy(content = enrichedContent)
                }
            }
        }

        return relevantMemoriesWithWeight.distinct()
    }

}

data class MemoryWithWeight(
    val content: String,
    val weight: Float
)