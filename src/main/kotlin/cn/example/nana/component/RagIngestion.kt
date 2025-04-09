package cn.example.nana.component

import cn.example.nana.commons.utils.CrawlUtil
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Component

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/9 13:33
 */
@Component
class RagIngestion(
    private val embeddingModel: EmbeddingModel,
    private val vectorStore: VectorStore
) {

    fun ingestUrlData(urlList: List<String>):List<Document?>{
        val crawlUtil = CrawlUtil(urlList)

        val pageDataList = crawlUtil.startCrawl()

        val documents = crawlUtil.convertToDocuments(pageDataList)

        return documents.map {
            val embed = embeddingModel.embed(it)
            it.metadata["embedding"]=embed
            it.text?.let { it1 -> Document(it1, it.metadata) }
        }

    }
}