package cn.example.nana.client

import cn.example.nana.client.api.WeatherApi
import cn.example.nana.client.api.dto.WeatherResponse
import cn.example.nana.commons.aop.Slf4j
import cn.example.nana.commons.aop.Slf4j.Companion.log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/14 16:33
 */
@Slf4j
@Component
class WeatherClient(
    @Value("\${weather.base-url}")
    private var baseUrl: String,
    @Value("\${weather.sk}")
    private var apiToken: String,
    @Value("\${weather.location}")
    private var location: String
) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val weatherApi = retrofit.create(WeatherApi::class.java)

    /**
     * 获取当前天气信息
     *
     * @param location 需要查询地区的 LocationID 或经纬度坐标
     * @param lang     多语言设置 (默认为 "zh")
     * @param unit     数据单位设置 (默认为 "m")
     * @return 天气信息，如果获取失败则返回 null
     */
    fun getCurrentWeather(
        location: String = this.location,
        lang: String = "zh",
        unit: String = "m"
    ): WeatherResponse? {
        if (location.isBlank()) {
            log.warn("警告：查询地区不能为空")
            return null
        }

        return try {
            val call = weatherApi.getCurrentWeather(location, lang, unit, "Bearer $apiToken")
            val response = call.execute()

            if (!response.isSuccessful) {
                throw IOException("API 请求失败: ${response.code()} ${response.message()}")
            }

            response.body()
        } catch (e: IOException) {
            log.error("获取天气信息失败: ${e.message}")
            null
        } catch (e: Exception) {
            log.error("获取天气信息时发生未知错误: ${e.message}")
            null
        }
    }
}