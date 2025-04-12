package cn.example.nana.tools


import cn.example.nana.client.WebSearchClient
import cn.example.nana.commons.aop.Slf4j
import cn.example.nana.commons.constants.TextConstants
import cn.example.nana.commons.enums.Errors
import cn.example.nana.commons.exception.BusinessException
import cn.example.nana.commons.utils.PictureUtils
import cn.example.nana.component.RagIngestion
import cn.example.nana.query.MilvusQuery
import cn.example.nana.query.Neo4jQuery
import cn.example.nana.service.RagService
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.prompt.SystemPromptTemplate
import org.springframework.ai.model.Media
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.util.MimeTypeUtils
import java.time.LocalDateTime
import java.util.concurrent.Executors


/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/8 00:46
 */
@Slf4j
@Service
class CommonTools(
    private val ollamaChatModel: OllamaChatModel,
    private val webSearchClient: WebSearchClient,
    private val ragIngestion: RagIngestion,
    private val openAiChatModel: OpenAiChatModel,
    private val ragService: RagService,
    private val milvusQuery: MilvusQuery,
    private val neo4jQuery: Neo4jQuery
){

    companion object{
        private val EXECUTOR = Executors.newFixedThreadPool(32)
    }

    @Tool(description = """
        当用户提到时间相关的词汇(早上，中午，下午，晚上),
        或者和时间获取当前的时间(晚安，早安)等,
        使用该方法获取时间
    """)
    fun getCurrentDateTime(): String {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString()
    }

    @Tool(description = "解析url形式的图片")
    fun parseImage(@ToolParam(description = "输入图片的url") imageUrl: String): String {
        val localPicturePath = PictureUtils.saveImageCache(imageUrl)

        try {
            val content = ChatClient.create(ollamaChatModel)
                .prompt()
                .user {
                    it.text(TextConstants.IMAGE_TO_TEXT_PROMPT)
                        .media(Media(MimeTypeUtils.IMAGE_PNG, ClassPathResource(localPicturePath)))
                }
                .call()
                .content() ?: throw BusinessException(Errors.CHAT_ERROR)
            return content
        }finally {
            ClassPathResource(localPicturePath).file.delete()
        }

    }

    @Tool(description = "无法从已有数据中获得有效且准确度高的信息，尝试从网络获取信息")
    fun getInformationOnWeb(@ToolParam(description = "要检索的问题或是信息") information: String):String{
        val results = webSearchClient.searchSync(information, 30)
        val uriList = results.map { it.url }
        val documents = ragIngestion.ingestUrlData(uriList)

        val allContent = documents.joinToString("\n\n---\n\n") { doc ->
            """
            URL: ${doc?.metadata?.get("url")}
            Title: ${doc?.metadata?.get("title")}
            Content: ${doc?.text}
            """.trimIndent()
        }

        val ragMessage = SystemPromptTemplate(TextConstants.buildRagContextPromptForSingleSummary()).createMessage(mapOf("documents" to allContent))
        val summary = ChatClient.create(openAiChatModel)
            .prompt(Prompt(ragMessage))
            .user("请总结以下多个网页的内容，重点回答用户的问题：${information}\n\n${allContent}") // 可以根据你的需求调整 Prompt
            .call()
            .content() ?: ""

        EXECUTOR.submit{
            ragService.storeWebSearchResultInMilvus(summary)
        }

        return summary
    }

    @Tool(description = """
        该工具有较高的使用优先级，通常作为知识库或是过去知识记忆使用，且平常对话可能也会经常使用。
        当用户问题涉及到特定领域知识、历史对话信息或需要精确答案时，应优先使用此工具。
        通过本地知识库查询相关信息，获取权威、准确的答案。查询应包含用户问题的关键概念和要点。
    """)
    fun searchWithKnowledgeGraph(@ToolParam(description = "用户的完整问题或包含关键概念的查询语句") query: String): String {

        val milvusResults = milvusQuery.search(query, "web_search_results", topK = 30)

        if (milvusResults.isEmpty()) {
            return "没有找到相关信息。"
        }

        val enrichedResults = mutableListOf<String>()

        // 从用户查询中提取关键词（简单的按空格分割）
        val keywordsFromQuery = query.split("\\s+").toList()

        for (result in milvusResults) {
            val summary = result.summary // 从 Milvus 结果中获取 summary

            // 2. 基于 summary 在 Neo4j 知识图谱中查找相关联的内容
            val relatedContent = neo4jQuery.findRelatedContentBySummaryUsingKeywords(summary, keywords = keywordsFromQuery, depth = 5)

            val contextBuilder = StringBuilder()
            contextBuilder.append("找到以下相关信息：\n")
            contextBuilder.append("原始摘要：${summary}\n")

            if (relatedContent.isNotEmpty()) {
                contextBuilder.append("知识图谱关联内容：\n")
                relatedContent.forEach { contentNode ->
                    contextBuilder.append("- ${contentNode.contentNode.text}\n")

                    contentNode.matchedKeywords.forEach { keyword ->
                        contextBuilder.append("  - 关键词：${keyword}\n")
                    }
                }
            } else {
                contextBuilder.append("知识图谱中没有找到与该摘要直接关联的内容。\n")
            }

            enrichedResults.add(contextBuilder.toString())
        }

        // 3. 将增强后的信息拼接成一个字符串返回
        return enrichedResults.joinToString("\n\n")
    }






}
