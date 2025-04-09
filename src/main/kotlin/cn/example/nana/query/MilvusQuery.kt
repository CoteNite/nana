package cn.example.nana.query

import cn.example.nana.commons.aop.Slf4j
import cn.example.nana.commons.aop.Slf4j.Companion.log
import io.milvus.client.MilvusClient
import io.milvus.exception.MilvusException
import io.milvus.grpc.SearchResults
import io.milvus.param.R
import io.milvus.param.dml.SearchParam
import io.milvus.response.SearchResultsWrapper
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/10 04:23
 */
interface MilvusQuery {
    fun search(query: String, collectionName: String, topK: Int = 3,vectorDimension:Int=768): List<MilvusSearchResult>
}

@Slf4j
@Service
class MilvusQueryImpl(
    private val milvusClient: MilvusClient,
    private val embeddingModel: EmbeddingModel
): MilvusQuery {


    private val primaryKeyFieldName = "id"
    private val outputSummaryFieldName = "summary"


    override fun search(query: String, collectionName: String, topK: Int,vectorDimension:Int): List<MilvusSearchResult> {
        try {
            val queryVector = embeddingModel.embed(query)

            val outputFields = listOf(primaryKeyFieldName, outputSummaryFieldName)

            val searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                .withVectors(listOf(queryVector)) // Milvus 通常期望 List<List<Float>>，即使只有一个向量
                .withVectorFieldName("content_vector") // 你需要指定向量字段的名称，这里假设是 "vector"
                .withOutFields(outputFields)
                .withTopK(topK)
                .build()

            val searchResultR = milvusClient.search(searchParam)

            // 检查 Milvus 操作状态
            if (searchResultR.status != R.Status.Success.code) {
                log.error("Milvus search failed: ${searchResultR.message}")
                return emptyList() // 或者抛出异常
            }


            val results = mutableListOf<MilvusSearchResult>()
            val wrapper = SearchResultsWrapper(searchResultR.data.results)

            if (wrapper.getRowRecords(0).isNotEmpty()) {
                val rowRecords = wrapper.getRowRecords(0)
                for (rowRecord in rowRecords) {
                    // 通过字段名获取值，注意类型转换
                    val id = rowRecord.get(primaryKeyFieldName)?.toString() ?: "N/A" // 提供默认值或进行更严格的检查
                    val summary = rowRecord.get(outputSummaryFieldName)?.toString() ?: "N/A"

                    results.add(MilvusSearchResult(id, summary))
                }
            }

            return results

        } catch (e: MilvusException) {
            log.error("MilvusException during search: ${e.message}")
            return emptyList()
        } catch (e: Exception) {
            log.error("Exception during search: ${e.message}")
            return emptyList() // 或者重新抛出
        }
    }

}

data class MilvusSearchResult(val id: String, val summary: String)

