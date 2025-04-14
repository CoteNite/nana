package cn.example.nana.controller

import cn.example.nana.client.WeatherClient
import cn.example.nana.commons.constants.TextConstants
import cn.example.nana.models.graph.entity.KeywordNode
import cn.example.nana.service.TweetService
import cn.example.nana.task.MemoryReinforcementTask
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.data.neo4j.core.Neo4jTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/9 01:50
 */
@RestController
@RequestMapping("test")
class TestController(
    private val weatherClient: WeatherClient,
    private val tweetService: TweetService,
    private val openAiChatModel: OpenAiChatModel
){

    @PostMapping("test")
    fun test() {

        val tweet = tweetService.generateTweet()

        println(tweet)

        println(
            ChatClient.create(openAiChatModel)
                .prompt(TextConstants.generateImageGenerationPrompt(tweet))
                .call().content()
        )

    }


}