package cn.example.nana.query

import cn.example.nana.commons.constants.TextConstants
import cn.example.nana.commons.enums.Errors
import cn.example.nana.commons.exception.BusinessException
import cn.example.nana.tools.CommonTools
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.cassandra.CassandraChatMemory
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.model.Media
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

    fun generate(sessionId:String?,message:String,imageUrl: String?): String

}

@Service
class ChatQueryImpl(
    private val chatModel: OpenAiChatModel,
    private val chatMemory: CassandraChatMemory,
    private val commonTools: CommonTools
): ChatQuery {

    override fun generate(sessionId:String?,message:String,imageUrl: String?):String{
        val content:String =this.chat(sessionId?:"45", message)
        return content
    }


    private fun chat(sessionId: String,message: String):String{
        val advisor = MessageChatMemoryAdvisor(chatMemory, sessionId, 10000)
        val content = ChatClient.create(chatModel)
            .prompt(TextConstants.NANA_INFORMATION)
            .tools(commonTools)
            .advisors(advisor)
            .user(message)
            .call()
            .content()?:throw BusinessException(Errors.CHAT_ERROR)
        return content
    }


}