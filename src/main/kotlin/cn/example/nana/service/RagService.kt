package cn.example.nana.service

import cn.example.nana.command.KnowledgeGraphCommand
import cn.example.nana.repo.milvus.StoreWebSearchRepo
import org.springframework.stereotype.Service

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/10 03:42
 */
interface RagService {
    fun storeWebSearchResultInMilvus(summary: String)
}


@Service
class RagServiceImpl(
    private val storeWebSearchRepo: StoreWebSearchRepo,
    private val knowledgeGraphCommand: KnowledgeGraphCommand
): RagService{

    override fun storeWebSearchResultInMilvus(summary: String) {
        knowledgeGraphCommand.processWebSearchSummary(summary)
        storeWebSearchRepo.storeWebSearchResultInMilvus(summary)
    }

}