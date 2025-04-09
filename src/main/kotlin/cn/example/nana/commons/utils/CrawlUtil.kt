package cn.example.nana.commons.utils

import cn.example.nana.commons.aop.Slf4j
import cn.example.nana.commons.aop.Slf4j.Companion.log
import cn.example.nana.commons.enums.Errors
import cn.example.nana.commons.exception.BusinessException
import com.alibaba.fastjson2.JSON
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.reader.tika.TikaDocumentReader
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.VectorStore
import us.codecraft.webmagic.Page
import us.codecraft.webmagic.Site
import us.codecraft.webmagic.Spider
import us.codecraft.webmagic.processor.PageProcessor
import us.codecraft.webmagic.scheduler.BloomFilterDuplicateRemover
import us.codecraft.webmagic.scheduler.QueueScheduler
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/9 00:57
 */
@Slf4j
class CrawlUtil(
    private val targetUrls: List<String>,
    private val threadCount: Int = 32,
) : PageProcessor{

    companion object{

        private val TOKEN_TEXT_SPLITTER = TokenTextSplitter()

        private val CRAWL_DATA = ConcurrentHashMap<String, WebPageData>()
        private val processedUrlCount = AtomicInteger(0)

        // 用于清除之前爬取的数据
        private fun clearCrawlData() {
            CRAWL_DATA.clear()
            processedUrlCount.set(0)
        }

        // 获取爬取的数据
        private fun getCrawlData(): List<WebPageData> {
            return CRAWL_DATA.values.toList()
        }

    }

    override fun process(page: Page) {
        try {
            extractPageInformation(page)

            // 计数器增加
            processedUrlCount.incrementAndGet()
        } catch (e: Exception) {
            log.error("处理页面时出错: ${page.url}, 错误: ${e.message}", e)
        }
    }

    /**
     * 开始爬取数据
     */
    fun startCrawl(): List<WebPageData> {
        clearCrawlData()
        log.info("开始网页数据抓取，目标URL数量: ${targetUrls.size}，使用线程数: $threadCount...")

        // 使用布隆过滤器去重
        val scheduler = QueueScheduler().setDuplicateRemover(
            BloomFilterDuplicateRemover(10000000)
        )

        Spider.create(this)
            .addUrl(*targetUrls.toTypedArray())
            .setScheduler(scheduler)
            .thread(threadCount)
            .run()

        log.info("网页数据抓取完成，共抓取到 ${CRAWL_DATA.size} 个网页的数据。")

        return getCrawlData()
    }

    /**
     * 异步爬取数据
     */
    suspend fun startCrawlAsync(): List<WebPageData> = withContext(Dispatchers.IO) {
        clearCrawlData()
        log.info("开始异步网页数据抓取，目标URL数量: ${targetUrls.size}，使用线程数: $threadCount...")

        val scheduler = QueueScheduler().setDuplicateRemover(
            BloomFilterDuplicateRemover(10000000)
        )

        val spider = Spider.create(this@CrawlUtil)
            .addUrl(*targetUrls.toTypedArray())
            .setScheduler(scheduler)
            .thread(threadCount)

        // 异步启动爬虫
        spider.runAsync()

        // 等待爬虫完成
        while (spider.status!=Spider.Status.Stopped) {
            delay(1000)
            log.debug("已处理 ${processedUrlCount.get()} 个页面，队列中剩余: ${spider.scheduler}")
        }

        log.info("网页数据抓取完成，共抓取到 ${CRAWL_DATA.size} 个网页的数据。")

        return@withContext getCrawlData()
    }

    private fun extractPageInformation(page: Page) {
        // 提取页面标题
        val title = page.html.xpath("//title/text()").get() ?: "无标题"

        // 提取页面内容
        val contentList = mutableListOf<String>()

        // 尝试提取结构化内容
        val paragraphs = page.html.xpath("//body//p/text()").all()
        if (paragraphs.isNotEmpty()) {
            contentList.addAll(paragraphs)
        }

        // 提取标题内容
        val headings = page.html.xpath("//h1/text() | //h2/text() | //h3/text()").all()
        if (headings.isNotEmpty()) {
            contentList.addAll(headings)
        }

        // 如果上述方法未获取到内容，尝试获取所有文本
        if (contentList.isEmpty()) {
            val allText = page.html.xpath("//body//text()").all()
                .filter { it.trim().isNotEmpty() }
            contentList.addAll(allText)
        }

        // 清洗内容
        val cleanedContent = contentList
            .map { it.trim() }
            .filter { it.length > 5 } // 过滤过短内容

        // 将抓取的数据添加到集合
        val url = page.url.toString()
        CRAWL_DATA[url] = WebPageData(url, title, cleanedContent)

        log.debug("已抓取页面: $title")
    }


    fun save2VectorStore(pageDataList: List<WebPageData>, vectorStore: VectorStore,searchWords:String){
        pageDataList.forEach{ pageData ->
            val url = pageData.url
            val title = pageData.title ?: "无标题"
            pageData.content?.forEach { content ->
                // 创建包含元数据的 Document
                val document = Document(content, mapOf("url" to url, "title" to title, "searchWords" to searchWords))
                // 使用分词器分割文本
                val documents = TOKEN_TEXT_SPLITTER.apply(listOf(document))
                // 将分割后的文档添加到向量数据库
                vectorStore.accept(documents)
            }
        }
        log.info("成功将 ${pageDataList.size} 个网页的数据保存到向量数据库。")
    }

    private fun buildFileList(reader:TikaDocumentReader): MutableList<Document>? {
        val documents = reader.get()
        val documentsSplitterList = TOKEN_TEXT_SPLITTER.apply(documents)
        return documentsSplitterList
    }

    fun convertToDocuments(pageDataList: List<WebPageData>): List<Document> {
        return pageDataList.map { pageData ->
            val content = pageData.content?.joinToString("\n") ?: "" // 将内容列表合并为单个字符串
            val metadata = mapOf("url" to pageData.url, "title" to (pageData.title ?: ""))
            Document(content, metadata)
        }
    }

}

data class WebPageData(val url: String, val title: String?, val content: List<String>?)