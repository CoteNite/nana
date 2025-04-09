package cn.example.nana.tools


import cn.example.nana.client.WebSearchClient
import cn.example.nana.commons.aop.Slf4j
import cn.example.nana.commons.constants.TextConstants
import cn.example.nana.commons.enums.Errors
import cn.example.nana.commons.exception.BusinessException
import cn.example.nana.commons.utils.PictureUtils
import cn.example.nana.component.RagIngestion
import cn.example.nana.repo.MilvusRepository
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
    private val ragService: RagService
){


    companion object{
        private val EXECUTOR = Executors.newFixedThreadPool(32)
    }

    @Tool(description = "获取当前的时间")
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




}
