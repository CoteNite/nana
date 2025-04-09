package cn.example.nana.controller

import cn.example.nana.commons.utils.CrawlUtil
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/9 01:50
 */
@RestController
@RequestMapping("test")
class TestController(
    private val vectorStore: MilvusVectorStore
){

    @RequestMapping("test")
    fun test() {
        val urls = listOf("https://cotenite.github.io/blog/story/%E5%B0%8F%E8%AF%B4/%E9%92%9F%E6%97%B6%E6%82%9F.html")

        val crawlUtil = CrawlUtil(urls)

        val pageDataList = crawlUtil.startCrawl()

    }

}