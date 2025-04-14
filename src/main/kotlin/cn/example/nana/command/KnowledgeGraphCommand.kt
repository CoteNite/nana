package cn.example.nana.command

import cn.example.nana.commons.aop.Slf4j
import cn.example.nana.commons.aop.Slf4j.Companion.log
import cn.example.nana.commons.constants.RedisKeyBuilder
import cn.example.nana.commons.constants.TextConstants
import cn.example.nana.commons.enums.Errors
import cn.example.nana.commons.exception.BusinessException
import cn.example.nana.models.graph.entity.ContentNode
import cn.example.nana.models.graph.entity.KeywordNode
import cn.example.nana.repo.neo4j.ContentRepository
import cn.example.nana.repo.neo4j.KeywordRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.redisson.api.RedissonClient
import org.redisson.api.RSet
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/10 02:14
 */
import org.springframework.data.neo4j.core.Neo4jTemplate // 确保导入 Neo4jTemplate
@Slf4j
@Service
class KnowledgeGraphCommand(
    private val keywordRepository: KeywordRepository,
    private val contentRepository: ContentRepository,
    private val openAiChatModel: OpenAiChatModel,
    private val redissonClient: RedissonClient
) {

    private val objectMapper = jacksonObjectMapper()


    @Transactional
    fun processWebSearchSummary(summary: String) {
        val webSearchKeywordsKey = RedisKeyBuilder.buildWebSearchKeywordsKey()

        val keywordsSet: RSet<String> = redissonClient.getSet(webSearchKeywordsKey)
        val historicalKeywords = keywordsSet.readAll()
        log.info("从 Redis 获取的网络搜索历史关键词: $historicalKeywords")

        var jsonString = ChatClient.create(openAiChatModel)
            .prompt(TextConstants.buildKnowledgeGraphPrompt(summary, historicalKeywords)) // 这里不需要历史关键词
            .call()
            .content() ?: throw BusinessException(Errors.CHAT_ERROR)

        jsonString = extractJsonFromMarkdown(jsonString)

        log.info("LLM Response: $jsonString")

        try {
            val entries: List<InputJsonEntry> = objectMapper.readValue<List<InputJsonEntry>>(jsonString)
            log.info("Processing {} entries from JSON.", entries.size)

            val keywordNodeCache = mutableMapOf<String, KeywordNode>()
            val contentNodesToProcess = mutableListOf<Pair<InputJsonEntry, ContentNode>>()

            entries.forEach { entry ->
                if (entry.keyWords.isBlank() || entry.content.isBlank()) {
                    log.warn("Skipping entry with blank keyWords or content: {}", entry)
                    return@forEach // continue in Kotlin's forEach
                }

                val sourceKeywordNode = findOrCreateKeywordNode(entry.keyWords, keywordNodeCache)
                val contentNode = ContentNode(text = entry.content, sourceType = "web_search", createdTime = System.currentTimeMillis())

                sourceKeywordNode.describes.add(contentNode)
                contentNodesToProcess.add(entry to contentNode)

                entry.to.forEach { targetKeyword ->
                    if (targetKeyword.isNotBlank()) {
                        findOrCreateKeywordNode(targetKeyword, keywordNodeCache)
                    } else {
                        log.warn("Found blank target keyword in 'to' list for source keyword '{}'", entry.keyWords)
                    }
                }
            }

            val savedKeywordNodes = keywordRepository.saveAll(keywordNodeCache.values)

            log.info("Saved/Updated {} Keyword nodes.", savedKeywordNodes.size)
            savedKeywordNodes.forEach { keywordNodeCache[it.keyword] = it }

            val finalContentNodesToSave = mutableListOf<ContentNode>()
            contentNodesToProcess.forEach { (entry, contentNode) ->
                finalContentNodesToSave.add(contentNode)
            }

            contentRepository.saveAll(finalContentNodesToSave)
            log.info("Updated {} Content nodes.", finalContentNodesToSave.size) // 修改日志信息

            // 使用 repository 方法创建 REFERENCES 关系
            contentNodesToProcess.forEach { (entry, contentNode) ->
                entry.to.forEach { targetKeyword ->
                    if (targetKeyword.isNotBlank()) {
                        val targetKeywordNode = keywordNodeCache[targetKeyword]
                        targetKeywordNode?.let {
                            keywordRepository.createReferencesRelationship(contentNode.id!!, it.keyword)
                            log.debug("Created REFERENCES relationship from Content ID '{}' to Keyword '{}'.", contentNode.id, it.keyword)
                        } ?: log.warn("Target Keyword '{}' not found in cache for creating REFERENCES relationship.", targetKeyword)
                    }
                }
            }
            log.info("Finished creating REFERENCES relationships for web search summary.")

            val extractedKeywords = entries.flatMap { it.keyWords.split(",").map { s -> s.trim() } }.toSet()
            keywordsSet.addAll(extractedKeywords)
            log.info("提取并存储网络搜索关键词到 Redis: $extractedKeywords")

            log.info("Graph building process completed successfully.")

        } catch (e: Exception) {
            log.error("Error processing JSON and building graph: ${e.message}", e)
            throw e
        }
    }

    @Transactional
    fun storeWeeklyMemoryInNeo4j(summaryContent: String, memoryAge: String, createdTime: Long, sessionId: String) {
        storeMemoryInNeo4j(summaryContent, memoryAge, createdTime, sessionId, "每周")
    }

    @Transactional
    fun storeFifteenDayMemoryInNeo4j(summaryContent: String, memoryAge: String, createdTime: Long, sessionId: String) {
        storeMemoryInNeo4j(summaryContent, memoryAge, createdTime, sessionId, "15 天")
    }

    @Transactional
    fun storeThirtyDayMemoryInNeo4j(summaryContent: String, memoryAge: String, createdTime: Long, sessionId: String) {
        storeMemoryInNeo4j(summaryContent, memoryAge, createdTime, sessionId, "30 天")
    }

    private fun storeMemoryInNeo4j(summaryContent: String, memoryAge: String, createdTime: Long, sessionId: String, memoryDuration: String) {
        log.info("开始处理${memoryDuration}记忆并存储到 Neo4j (Memory Age: $memoryAge, Created Time: $createdTime, Session ID: $sessionId)...")

        val keywordsSet: RSet<String> = redissonClient.getSet(RedisKeyBuilder.buildSessionKeywordsKey(sessionId))
        val historicalKeywords = keywordsSet.readAll()
        log.info("从 Redis 获取的历史关键词 (Session ID: $sessionId): $historicalKeywords")

        var jsonString = ChatClient.create(openAiChatModel)
            .prompt(TextConstants.buildKnowledgeGraphPrompt(summaryContent, historicalKeywords)) // 包含历史关键词
            .call()
            .content() ?: throw BusinessException(Errors.CHAT_ERROR)

        jsonString = extractJsonFromMarkdown(jsonString)

        log.info("LLM Response for knowledge graph entries: $jsonString")

        try {
            val entries: List<InputJsonEntry> = objectMapper.readValue<List<InputJsonEntry>>(jsonString)
            log.info("Processing {} entries for ${memoryDuration}记忆的关键词 from JSON.", entries.size)

            val keywordNodeCache = mutableMapOf<String, KeywordNode>()
            val contentNode = ContentNode(
                text = summaryContent,
                sourceType = "memory",
                memoryAge = memoryAge,
                createdTime = createdTime
            )

            entries.forEach { entry ->
                if (entry.keyWords.isNotBlank()) {
                    val sourceKeywordNode = findOrCreateKeywordNode(entry.keyWords, keywordNodeCache)
                    sourceKeywordNode.describes.add(contentNode)
                    entry.to.forEach { targetKeyword ->
                        if (targetKeyword.isNotBlank()) {
                            findOrCreateKeywordNode(targetKeyword, keywordNodeCache)
                        }
                    }
                }
            }

            val savedKeywordNodes = keywordRepository.saveAll(keywordNodeCache.values)
            log.info("Saved/Updated {} Keyword nodes for ${memoryDuration}记忆.", savedKeywordNodes.size)
            savedKeywordNodes.forEach { keywordNodeCache[it.keyword] = it }

            contentRepository.save(contentNode) // 先保存 contentNode 获取 id
            log.info("${memoryDuration}记忆内容节点已保存到 Neo4j，ID: ${contentNode.id}.")

            // 使用 repository 方法创建 REFERENCES 关系
            entries.forEach { entry ->
                entry.to.forEach { targetKeyword ->
                    if (targetKeyword.isNotBlank()) {
                        val targetKeywordNode = keywordNodeCache[targetKeyword]
                        targetKeywordNode?.let {
                            keywordRepository.createReferencesRelationship(contentNode.id!!, it.keyword)
                            log.debug("Created REFERENCES relationship from Content ID '{}' to Keyword '{}'.", contentNode.id, it.keyword)
                        } ?: log.warn("Target Keyword '{}' not found in cache for creating REFERENCES relationship.", targetKeyword)
                    }
                }
            }
            log.info("Finished creating REFERENCES relationships for ${memoryDuration}记忆.")

            val extractedKeywords = entries.flatMap { it.keyWords.split(",").map { s -> s.trim() } }.toSet()
            keywordsSet.addAll(extractedKeywords)
            log.info("提取并存储${memoryDuration}记忆的关键词到 Redis (Session ID: $sessionId): $extractedKeywords")

        } catch (e: Exception) {
            log.error("Error processing JSON for ${memoryDuration}记忆 and building graph: ${e.message}", e)
            throw e
        }
    }


    private fun findOrCreateKeywordNode(keyword: String, cache: MutableMap<String, KeywordNode>): KeywordNode {
        return cache.computeIfAbsent(keyword) {
            keywordRepository.findByKeyword(keyword).orElseGet {
                log.debug("Creating new KeywordNode instance for: {}", keyword)
                KeywordNode(keyword = keyword) // 创建新实例，待后续 saveAll 保存
            }
        }
    }


    fun extractJsonFromMarkdown(content: String): String {

        val startMarker = "```json"
        val endMarker = "```"

        if (content.startsWith(startMarker) && content.endsWith(endMarker)) {
            val startIndex = startMarker.length
            val endIndex = content.length - endMarker.length
            return content.substring(startIndex, endIndex).trim()
        } else {
            return content
        }
    }

}

data class InputJsonEntry(
    val keyWords: String,
    val content: String,
    val to: List<String> = emptyList() // 提供默认空列表
)