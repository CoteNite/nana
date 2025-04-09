package cn.example.nana.controller

import cn.example.nana.command.KnowledgeGraphCommand
import cn.example.nana.commons.utils.CrawlUtil
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore
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
    private val knowledgeGraphCommand: KnowledgeGraphCommand
){

    @PostMapping("test")
    fun test(@RequestParam summary:String) {
        knowledgeGraphCommand.processSummary(summary)
    }

}