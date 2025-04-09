package cn.example.nana.controller

import cn.example.nana.commons.response.Response
import cn.example.nana.service.NanaService
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.web.bind.annotation.PostMapping
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
class NanaController(
    private val nanaService: NanaService
){

    //根据设定，nana应该只有一个SessionId去进行操作
    @PostMapping("/generate")
    fun generate(@RequestParam message:String):Response{
        val content = nanaService.generate(null,message)
        return Response.success(content)
    }

    @PostMapping("/testGenerate")
    fun testGenerate(@RequestParam message:String,sessionId:String?):Response{
        val content = nanaService.generate(sessionId,message)
        return Response.success(content)
    }


}