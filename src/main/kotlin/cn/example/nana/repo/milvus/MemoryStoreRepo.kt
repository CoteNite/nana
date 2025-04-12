package cn.example.nana.repo.milvus

import cn.example.nana.commons.aop.Slf4j
import cn.example.nana.commons.aop.Slf4j.Companion.log
import cn.example.nana.models.milvus.MemoryStoreIndex
import io.milvus.client.MilvusClient
import io.milvus.param.R
import io.milvus.param.dml.InsertParam
import io.milvus.param.dml.SearchParam
import io.milvus.response.SearchResultsWrapper
import org.springframework.stereotype.Repository


/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/11 14:54
 */
@Slf4j
@Repository
class MemoryStoreRepo(
    private val milvusClient: MilvusClient
){

    fun storeEmbeddingInMilvus(embedding: FloatArray, sessionId: String, memoryAge: String, timestamp: Long): String {
        val sessionIdFiled = InsertParam.Field.builder()
            .name(MemoryStoreIndex.SESSION_ID)
            .values(listOf(sessionId))
            .build()

        val memoryAgeFiled = InsertParam.Field.builder()
            .name(MemoryStoreIndex.MEMORY_AGE)
            .values(listOf(memoryAge))
            .build()

        val embeddingFields = InsertParam.Field.builder()
            .name(MemoryStoreIndex.EMBEDDING)
            .values(listOf(embedding.toList()))
            .build()

        val timestampFiled = InsertParam.Field.builder()
            .name(MemoryStoreIndex.TIMESTAMP)
            .values(listOf(timestamp))
            .build()

        val insertParam = InsertParam.newBuilder()
            .withCollectionName(MemoryStoreIndex.TABLE_NAME)
            .withFields(listOf(sessionIdFiled, memoryAgeFiled, embeddingFields, timestampFiled))
            .build()

        try {
            milvusClient.insert(insertParam)
            log.info("成功插入 Embedding，Session ID: $sessionId, Memory Age: $memoryAge, Timestamp: $timestamp")
            return embedding.first().toString() // 这里可以根据你的实际需求返回更有意义的 ID，例如 Milvus 的向量 ID
        } catch (e: Exception) {
            log.error("插入 Embedding 失败: " + e.message)
        }
        return ""
    }

    fun searchEmbeddings(embedding:FloatArray):  List<Map<String, Any>> {
        try {
            val outputFields = listOf(
                MemoryStoreIndex.SESSION_ID,
                MemoryStoreIndex.MEMORY_AGE,
                MemoryStoreIndex.TIMESTAMP
            )

            val searchParam = SearchParam.newBuilder()
                .withCollectionName(MemoryStoreIndex.TABLE_NAME)
                .withVectors(listOf(embedding.toList()))
                .withVectorFieldName(MemoryStoreIndex.EMBEDDING)
                .withOutFields(outputFields)
                .withTopK(30)
                .build()

            val searchResultR = milvusClient.search(searchParam)

            // 检查 Milvus 操作状态
            if (searchResultR.status != R.Status.Success.code) {
                log.error("Milvus search failed: ${searchResultR.message}")
                return emptyList() // 或者抛出异常
            }

            val results = mutableListOf<Map<String, Any>>()
            val wrapper = SearchResultsWrapper(searchResultR.data.results)

            if (wrapper.getRowRecords(0).isNotEmpty()) {
                val rowRecords = wrapper.getRowRecords(0)
                for (rowRecord in rowRecords) {
                    val result = mutableMapOf<String, Any>()
                    result[MemoryStoreIndex.SESSION_ID] = rowRecord.get(MemoryStoreIndex.SESSION_ID)?.toString() ?: "N/A"
                    result[MemoryStoreIndex.MEMORY_AGE] = rowRecord.get(MemoryStoreIndex.MEMORY_AGE)?.toString() ?: "N/A"
                    result[MemoryStoreIndex.TIMESTAMP] = rowRecord.get(MemoryStoreIndex.TIMESTAMP)?.toString()?.toLongOrNull() ?: -1L
                    results.add(result)
                }
            }

            return results.distinct()
        } catch (e: Exception) {
            println("Error searching embeddings in Milvus: ${e.message}")
            return emptyList()
        }
    }


}