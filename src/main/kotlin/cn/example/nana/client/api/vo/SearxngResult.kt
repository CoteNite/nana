package cn.example.nana.client.api.vo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/9 12:24
 */
@JsonClass(generateAdapter = true)
data class SearxngResult(
    @Json(name = "url") val url: String,
    @Json(name = "title") val title: String,
    @Json(name = "content") val content: String? = ""
)

