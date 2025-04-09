package cn.example.nana.repo

import io.milvus.client.MilvusClient
import io.milvus.param.dml.InsertParam
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.stereotype.Repository

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/10 01:28
 */
@Repository
class MilvusRepository(
    private val embeddingModel: EmbeddingModel,
    private val milvusClient: MilvusClient
){

    fun storeWebSearchResultInMilvus(summary: String) {
        // 1. 向量化总结内容
        val embeddings = embeddingModel.embed(listOf(summary))
        if (embeddings.isNotEmpty()) {
            val embedding = embeddings.first()

            val summaryList = ArrayList<String>()
            val vectorList = ArrayList<List<Float>>()
            val createdAtList = ArrayList<Long>()

            summaryList.add(summary)
            vectorList.add(embedding.toList())
            createdAtList.add(System.currentTimeMillis() / 1000)

            val summaryField = InsertParam.Field.builder()
                .name("summary")
                .values(summaryList)
                .build()

            val vectorField = InsertParam.Field.builder()
                .name("content_vector")
                .values(vectorList)
                .build()

            val createdAtField = InsertParam.Field.builder()
                .name("created_at")
                .values(createdAtList)
                .build()

            val insertParam = InsertParam.newBuilder()
                .withCollectionName("web_search_results")
                .withFields(listOf( summaryField, vectorField, createdAtField))
                .build()

            try {
                milvusClient.insert(insertParam)
                println("成功存储 1 条知识记录到 Milvus")
            } catch (e: Exception) {
                println("存储到 Milvus 失败：${e.message}")
                e.printStackTrace()
            }
        } else {
            println("无法生成总结内容的向量。")
        }
    }
}