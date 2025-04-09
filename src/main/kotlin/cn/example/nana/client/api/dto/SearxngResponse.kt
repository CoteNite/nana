package cn.example.nana.client.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/9 03:56
 */
@JsonClass(generateAdapter = true)
data class SearxngResponse(
     @Json(name = "results") val results: List<SearchResult> = emptyList()
)

data class SearchResult(
     val url: String,
     val title: String,
     val content: String
)

