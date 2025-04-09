package cn.example.nana.client

import cn.example.nana.client.api.SearXNGApi
import cn.example.nana.client.api.dto.SearchResult
import cn.example.nana.client.api.dto.SearxngResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.CompletableFuture

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/9 12:13
 */
@Component
class WebSearchClient(
    @Value("\${searxng.base-url}")
    private var baseUrl: String
) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val searxngApi = retrofit.create(SearXNGApi::class.java)

    companion object {
        // 最大尝试分页数 - 防止无限循环
        private const val MAX_PAGES = 10
        // 每页获取的结果数量通常有默认值，但这里我们用它来评估何时停止尝试
        private const val EXPECTED_RESULTS_PER_PAGE = 10 // 您可能需要根据Searxng API的实际行为调整这个值
    }


    /**
     * 执行搜索并返回结果列表
     *
     * @param query 查询内容
     * @param count 需要的结果数量
     * @return 搜索结果列表
     */
    fun searchSync(query: String, count: Int): List<SearchResult> {
        if (query.isBlank()) {
            throw IllegalArgumentException("查询内容不能为空")
        }

        if (count <= 0) {
            throw IllegalArgumentException("查询数量必须大于0")
        }

        val results = mutableListOf<SearchResult>()
        var currentPage = 1
        var fetchedCount = 0

        while (fetchedCount < count && currentPage <= MAX_PAGES) {
            try {
                val call = searxngApi.search(query, pageNumber = currentPage)
                val response = call.execute()

                if (!response.isSuccessful) {
                    throw IOException("API请求失败 (页码: $currentPage): ${response.code()} ${response.message()}")
                }

                val searxngResponse = response.body() ?: throw IOException("响应体为空 (页码: $currentPage)")

                val currentResults = searxngResponse.results.map { result ->
                    SearchResult(
                        url = result.url,
                        title = result.title,
                        content = result.content ?: ""
                    )
                }

                results.addAll(currentResults)
                fetchedCount = results.size
                currentPage++

                // 如果当前页返回的结果数量少于预期，可能已经到达最后一页
                if (currentResults.size < EXPECTED_RESULTS_PER_PAGE) {
                    break
                }

            } catch (e: Exception) {
                throw IOException("搜索失败 (页码: $currentPage): ${e.message}", e)
            }
        }

        return results.take(count) // 最终确保返回的结果数量不超过count
    }

    suspend fun searchAsync(query: String, count: Int) : List<SearchResult>{
        if (query.isBlank()) {
            throw IllegalArgumentException("查询内容不能为空")
        }

        if (count <= 0) {
            throw IllegalArgumentException("查询数量必须大于0")
        }

        val allResults = mutableListOf<SearchResult>()
        val numPagesToFetch = (count + EXPECTED_RESULTS_PER_PAGE - 1) / EXPECTED_RESULTS_PER_PAGE
        val actualNumPages = minOf(numPagesToFetch, MAX_PAGES)
        val deferreds = mutableListOf<Deferred<List<SearchResult>>>()

        coroutineScope {
            for (page in 1..actualNumPages) {
                val deferred = async {
                    try {
                        val response = searxngApi.search(query, pageNumber = page).execute()
                        if (response.isSuccessful) {
                            val searxngResponse = response.body()
                            searxngResponse?.results?.map { result ->
                                SearchResult(
                                    url = result.url,
                                    title = result.title,
                                    content = result.content ?: ""
                                )
                            } ?: emptyList()
                        } else {
                            throw IOException("API请求失败 (页码: $page): ${response.code()} ${response.message()}")
                        }
                    } catch (e: Exception) {
                        println("搜索失败 (页码: $page): ${e.message}") // 打印错误信息，可以选择更合适的错误处理方式
                        emptyList() // 返回空列表，不中断整个爬取过程
                    }
                }
                deferreds.add(deferred)
            }

            deferreds.awaitAll().forEach { results ->
                allResults.addAll(results)
            }
        }

        return allResults.take(count)
    }

}