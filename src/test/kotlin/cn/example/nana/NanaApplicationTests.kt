package cn.example.nana

import cn.example.nana.client.WebSearchClient
import cn.example.nana.commons.utils.CrawlUtil
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import us.codecraft.webmagic.Page
import us.codecraft.webmagic.Site
import us.codecraft.webmagic.Spider
import us.codecraft.webmagic.processor.PageProcessor


@SpringBootTest
class NanaApplicationTests{

    private val webSearchClient = WebSearchClient("http://localhost:8041/")

    @Test
    fun SearchTest() {

        // 执行同步搜索
        val results = webSearchClient.searchSync("5080相对4080的提升", 60)


        // 打印结果，便于调试
        println("同步搜索找到 ${results.size} 条结果:")
        results.forEachIndexed { index, result ->
            println("结果 ${index + 1}:")
            println("标题: ${result.title}")
            println("URL: ${result.url}")
            println("内容摘要: ${result.content.take(100)}...")
            println("-------------------")
        }


        println(results)

    }



}




