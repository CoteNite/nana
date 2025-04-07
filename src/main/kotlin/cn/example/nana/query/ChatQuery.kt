package cn.example.nana.query

import cn.example.nana.commons.constants.TextConstants
import cn.example.nana.commons.enums.Errors
import cn.example.nana.commons.exception.BusinessException
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.cassandra.CassandraChatMemory
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.model.Media
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.stereotype.Service
import org.springframework.util.MimeTypeUtils
import java.net.URL

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/3 01:37
 */
interface ChatQuery{

    fun generate(sessionId:String,message:String,imageUrl: String?): String

}

@Service
class ChatQueryImpl(
    private val chatModel: OpenAiChatModel,
    private val chatMemory: CassandraChatMemory
): ChatQuery {

    override fun generate(sessionId:String,message:String,imageUrl: String?):String{
        val content:String =
            if (imageUrl!=null)
                this.chat(sessionId, message,imageUrl)
            else
                this.chat(sessionId, message)

        return content
    }


    private fun chat(sessionId: String,message: String):String{
        val advisor = MessageChatMemoryAdvisor(chatMemory, sessionId, 1000)
        val content = ChatClient.create(chatModel)
            .prompt(TextConstants.NANA_INFORMATION)
            .advisors(advisor)
            .user(message)
            .call()
            .content()?:throw BusinessException(Errors.CHAT_ERROR)
        return content
    }

    private fun chat(sessionId: String,message: String,imageUrl:String?):String{
        val advisor = MessageChatMemoryAdvisor(chatMemory, sessionId, 1000)
        val userMessage = UserMessage(message, Media(MimeTypeUtils.IMAGE_PNG, URL(imageUrl)))
        val content = ChatClient.create(chatModel)
            .prompt(TextConstants.NANA_INFORMATION)
            .advisors(advisor)
            .messages(userMessage)
            .user(message)
            .call()
            .content()?:throw BusinessException(Errors.CHAT_ERROR)
        return content
    }

}