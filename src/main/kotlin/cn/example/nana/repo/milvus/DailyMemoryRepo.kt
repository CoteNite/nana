package cn.example.nana.repo.milvus

import cn.example.nana.commons.aop.Slf4j
import cn.example.nana.commons.aop.Slf4j.Companion.log
import cn.example.nana.models.milvus.MidTermMemoryIndex
import io.milvus.client.MilvusClient
import io.milvus.grpc.DataType
import io.milvus.param.dml.InsertParam
import org.springframework.stereotype.Repository


/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/11 14:54
 */
@Slf4j
@Repository
class DailyMemoryRepo(
    private val milvusClient: MilvusClient
){
    
    fun storeEmbeddingInMilvus(embedding: FloatArray, sessionId: String): String {
        val sessionIdFiled = InsertParam.Field.builder()
            .name(MidTermMemoryIndex.SESSION_ID)
            .values(listOf(sessionId))
            .build()

        val embeddingFields = InsertParam.Field.builder()
            .name(MidTermMemoryIndex.EMBEDDING)
            .values(listOf(embedding.toList()))
            .build()

        val insertParam = InsertParam.newBuilder()
            .withCollectionName(MidTermMemoryIndex.TABLE_NAME)
            .withFields(listOf(sessionIdFiled,embeddingFields))
            .build()

        try {
            milvusClient.insert(insertParam)
            log.info("成功插入 Embedding，Session ID: $sessionId")
            return embedding.first().toString()
        } catch (e: Exception) {
            log.error("插入 Embedding 失败: " + e.message)
        }
        return ""
    }


}