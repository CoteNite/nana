package cn.example.nana.tools


import cn.example.nana.commons.aop.Slf4j
import cn.example.nana.commons.aop.Slf4j.Companion.log
import cn.example.nana.commons.constants.TextConstants
import cn.example.nana.commons.enums.Errors
import cn.example.nana.commons.exception.BusinessException
import cn.example.nana.commons.utils.PictureUtils
import cn.hutool.core.util.IdUtil
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.model.Media
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.util.MimeTypeUtils
import java.time.LocalDateTime


/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/8 00:46
 */
@Slf4j
@Service
class CommonTools(
    private val chatModel: OllamaChatModel
){

    @Tool(description = "获取当前的时间")
    fun getCurrentDateTime(): String {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString()
    }


    @Tool(description = "记录当前时间到记忆中，以便后续使用")
    fun getNowTime(): String {
        return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString()
    }


    @Tool(description = "解析url形式的图片")
    fun parseImage(@ToolParam(description = "输入图片的url") imageUrl: String): String {
        val localPicturePath = PictureUtils.saveImageCache(imageUrl)

        try {
            val content = ChatClient.create(chatModel)
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





}