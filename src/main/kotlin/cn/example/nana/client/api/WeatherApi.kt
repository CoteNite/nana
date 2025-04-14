package cn.example.nana.client.api

import cn.example.nana.client.api.dto.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/14 16:06
 */
interface WeatherApi {

    @GET("/v7/weather/now")
    fun getCurrentWeather(
        @Query("location") location: String,
        @Query("lang") lang: String = "zh",
        @Query("unit") unit: String = "m",
        @Header("Authorization") authorization: String
    ): Call<WeatherResponse>

}