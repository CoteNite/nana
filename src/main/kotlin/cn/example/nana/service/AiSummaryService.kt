package cn.example.nana.service

import cn.example.nana.commons.constants.TextConstants
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.stereotype.Service

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/11 13:13
 */
interface AiSummaryService {
    fun summarizeConversation(conversation: String): String
}

@Service
class AiSummaryServiceImpl(
    private val openAiChatModel: OpenAiChatModel,
):AiSummaryService{
    override fun summarizeConversation(conversation: String): String {
        return ChatClient.create(openAiChatModel)
            .prompt(Prompt(TextConstants.buildSummaryPrompt(conversation)))
            .call()
            .content()?:""
    }

}