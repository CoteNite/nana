package cn.example.nana.service

import cn.example.nana.commons.constants.TextConstants
import cn.example.nana.commons.enums.Errors
import cn.example.nana.commons.exception.BusinessException
import cn.example.nana.tools.CommonTools
import cn.example.nana.tools.MemoryTools
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.cassandra.CassandraChatMemory
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.stereotype.Service

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/3 01:37
 */
interface NanaService{
    fun generate(sessionId:String?,message:String): String
}

@Service
class NanaServiceImpl(
    private val chatModel: OpenAiChatModel,
    private val chatMemory: CassandraChatMemory,
    private val commonTools: CommonTools,
    private val memoryTools: MemoryTools
): NanaService {

    override fun generate(sessionId:String?,message:String):String{
        val content:String =this.chat4Nana(sessionId?:"45", message)
        return content
    }


    private fun chat4Nana(sessionId: String, message: String):String{
        val advisor = MessageChatMemoryAdvisor(chatMemory, sessionId, 10000)
        val content = ChatClient.create(chatModel)
            .prompt(TextConstants.NANA_INFORMATION)
            .tools(commonTools,memoryTools)
            .advisors(advisor)
            .user(message)
            .call()
            .content()?:throw BusinessException(Errors.CHAT_ERROR)
        return content
    }


}