package cn.example.nana.query

import cn.example.nana.commons.constants.TextConstants
import cn.example.nana.commons.enums.Errors
import cn.example.nana.commons.exception.BusinessException
import cn.example.nana.tools.CommonTools
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.cassandra.CassandraChatMemory
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.stereotype.Service

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/9 02:20
 */

interface ChatQuery {
//    fun chat(sessionId: String,message: String,prompt: String):String
}

@Service
class ChatQueryImpl(
    private val chatModel: OpenAiChatModel,
    private val chatMemory: CassandraChatMemory,
    private val commonTools: CommonTools
): ChatQuery {
//
//
//
//    override fun chatWithPrompt(sessionId: String,message: String,prompt:String):String{
//        val advisor = MessageChatMemoryAdvisor(chatMemory, sessionId, 10000)
//        val content = ChatClient.create(chatModel)
//            .prompt(prompt)
//            .tools(commonTools)
//            .advisors(advisor)
//            .user(message)
//            .call()
//            .content()?:throw BusinessException(Errors.CHAT_ERROR)
//        return content
//    }


}

