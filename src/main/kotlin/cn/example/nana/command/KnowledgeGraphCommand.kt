package cn.example.nana.command

import cn.example.nana.commons.aop.Slf4j
import cn.example.nana.commons.aop.Slf4j.Companion.log
import cn.example.nana.commons.constants.TextConstants
import cn.example.nana.commons.enums.Errors
import cn.example.nana.commons.exception.BusinessException
import cn.example.nana.models.graph.entity.ContentNode
import cn.example.nana.models.graph.entity.KeywordNode
import cn.example.nana.repo.neo4j.ContentRepository
import cn.example.nana.repo.neo4j.KeywordRepository
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper
import java.util.Map

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/10 02:14
 */
@Slf4j
@Service
class KnowledgeGraphCommand(
    private val keywordRepository: KeywordRepository,
    private val contentRepository: ContentRepository,
    private val openAiChatModel: OpenAiChatModel
){

    private val objectMapper = jacksonObjectMapper()

    @Transactional
    fun processSummary(summary: String) {

        val jsonString = ChatClient.create(openAiChatModel)
            .prompt(TextConstants.buildKnowledgeGraphPrompt(summary))
            .call()
            .content()?:throw BusinessException(Errors.CHAT_ERROR)

        log.info("LLM Response: $jsonString")

        try {
            val entries: List<InputJsonEntry> = objectMapper.readValue<List<InputJsonEntry>>(jsonString)
            log.info("Processing {} entries from JSON.", entries.size)

            // 用于在事务中缓存 KeywordNode，避免重复查询/创建
            val keywordNodeCache = mutableMapOf<String, KeywordNode>()
            val contentNodesToProcess = mutableListOf<Pair<InputJsonEntry, ContentNode>>()

            // --- 第一步: 创建或查找 Keyword 节点，并创建 Content 节点和 DESCRIBES 关系 ---
            entries.forEach { entry ->
                if (entry.keyWords.isBlank() || entry.content.isBlank()) {
                    log.warn("Skipping entry with blank keyWords or content: {}", entry)
                    return@forEach // continue in Kotlin's forEach
                }

                // 查找或创建源 KeywordNode
                val sourceKeywordNode = findOrCreateKeywordNode(entry.keyWords, keywordNodeCache)

                // 为每个条目创建一个新的 ContentNode
                val contentNode = ContentNode(text = entry.content)

                // 建立关系： Keyword --[DESCRIBES]-> Content
                sourceKeywordNode.describes.add(contentNode)
                // 注意：此时 contentNode 还没有 ID，ID 在保存时生成

                // 存储待处理的 ContentNode 及其来源信息，用于第二步建立 REFERENCES 关系
                contentNodesToProcess.add(entry to contentNode)

                // 确保 `to` 列表中的目标 KeywordNode 也被创建或查找（如果它们还不存在）
                entry.to.forEach { targetKeyword ->
                    if (targetKeyword.isNotBlank()) {
                        findOrCreateKeywordNode(targetKeyword, keywordNodeCache)
                    } else {
                        log.warn("Found blank target keyword in 'to' list for source keyword '{}'", entry.keyWords)
                    }
                }
            }

            // 保存所有 KeywordNode（会级联保存它们关联的新的 ContentNode 及 DESCRIBES 关系）
            // 这也会确保所有 Keyword 节点在 Neo4j 中存在
            val savedKeywordNodes = keywordRepository.saveAll(keywordNodeCache.values)
            log.info("Saved/Updated {} Keyword nodes.", savedKeywordNodes.size)

            // 更新缓存，确保包含已保存节点（特别是对于新创建的节点）
            savedKeywordNodes.forEach { keywordNodeCache[it.keyword] = it }


            // --- 第二步: 建立 Content --[REFERENCES]-> Keyword 关系 ---
            val finalContentNodesToSave = mutableListOf<ContentNode>()
            contentNodesToProcess.forEach { (entry, contentNode) ->
                // 查找对应的已保存的 ContentNode (或者直接使用内存中的 contentNode 对象，
                // 因为它已经被级联保存并可能拥有了ID)
                // 通常，内存中的对象在 saveAll 后状态已更新，可以直接使用 contentNode

                entry.to.forEach { targetKeyword ->
                    if (targetKeyword.isNotBlank()) {
                        val targetKeywordNode = keywordNodeCache[targetKeyword]
                        if (targetKeywordNode != null) {
                            // 建立关系: Content --[REFERENCES]-> Keyword
                            contentNode.references.add(targetKeywordNode)
                        } else {
                            // 这理论上不应该发生，因为第一步已经缓存了所有目标节点
                            log.error(
                                "Target KeywordNode '{}' not found in cache for content derived from '{}'. This indicates an issue.",
                                targetKeyword, entry.keyWords
                            )
                        }
                    }
                }
                finalContentNodesToSave.add(contentNode) // 添加到待保存列表
            }

            contentRepository.saveAll(finalContentNodesToSave)
            log.info("Updated {} Content nodes with REFERENCES relationships.", finalContentNodesToSave.size)

            log.info("Graph building process completed successfully.")

        } catch (e: Exception) {
            log.error("Error processing JSON and building graph: ${e.message}", e)
            // 由于 @Transactional，发生异常时会自动回滚
            throw e // 可以重新抛出，让调用者知道失败了
        }
    }

    private fun findOrCreateKeywordNode(keyword: String, cache: MutableMap<String, KeywordNode>): KeywordNode {
        return cache.computeIfAbsent(keyword) {
            keywordRepository.findById(keyword).orElseGet {
                log.debug("Creating new KeywordNode instance for: {}", keyword)
                KeywordNode(keyword = keyword) // 创建新实例，待后续 saveAll 保存
            }
        }
    }



}


data class InputJsonEntry(
    val keyWords: String,
    val content: String,
    val to: List<String> = emptyList() // 提供默认空列表
)
