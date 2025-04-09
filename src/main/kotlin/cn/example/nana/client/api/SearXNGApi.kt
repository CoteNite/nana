package cn.example.nana.client.api

import cn.example.nana.client.api.dto.SearxngResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import retrofit2.http.QueryMap

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/9 03:55
 */
interface SearXNGApi{

    @GET("search")
    fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("pageno") pageNumber: Int = 1,
        @Query("categories") categories: String = "general"
    ): Call<SearxngResponse>

}