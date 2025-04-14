package cn.example.nana.service

import cn.example.nana.client.WeatherClient
import cn.example.nana.commons.constants.TextConstants
import cn.example.nana.commons.enums.Errors
import cn.example.nana.commons.exception.BusinessException
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.stereotype.Service

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/14 20:25
 */
interface TweetService {
    fun generateTweet(): String
}

@Service
class TweetServiceImpl(
    private val weatherClient: WeatherClient,
    private val openAiChatModel: OpenAiChatModel
): TweetService{




    override fun generateTweet():String{

        val response = weatherClient.getCurrentWeather()?:throw BusinessException(Errors.API_ERROR)

        val tweet = ChatClient.create(openAiChatModel)
            .prompt(TextConstants.generateWeatherTweetPrompt(response.now))
            .call()
            .content()?:throw BusinessException(Errors.CHAT_ERROR)

        return tweet
    }






}
