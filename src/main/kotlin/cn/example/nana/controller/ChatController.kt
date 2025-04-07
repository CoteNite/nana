package cn.example.nana.controller

import cn.example.nana.commons.response.Response
import cn.example.nana.query.ChatQuery
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/7 23:03
 */
@RestController
@RequestMapping("chat")
class ChatController(
    private val chatQuery: ChatQuery
){

    @GetMapping("/generate")
    fun generate(@RequestParam message:String, @RequestParam sessionId:String, @RequestParam(required = false) imageUrl: String?):Response{
        val content = chatQuery.generate(sessionId,message,imageUrl)
        return Response.success(content)
    }


}