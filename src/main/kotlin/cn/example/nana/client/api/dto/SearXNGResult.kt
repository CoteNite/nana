package cn.example.nana.client.api.dto

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/9 03:57
 */
data class SearXNGResult(
    private val title: String? = null ,
    private val content: String? = null ,
    private val url: String? = null ,
    private val engine: String? = null ,
    private val parsedUrl: List<String>? = null ,
    private val template: String? = null ,
    private val engines: List<String>? = null ,
    private val positions: List<Int>? = null ,
    private val score: Double = 0.0 ,
    private val category: String? = null

)

