package cn.example.nana.client.api.dto

import com.squareup.moshi.Json


/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/14 16:11
 */
data class WeatherResponse(
    @Json(name = "code") val code: String,
    @Json(name = "updateTime") val updateTime: String,
    @Json(name = "fxLink") val fxLink: String,
    @Json(name = "now") val now: Now,
    @Json(name = "refer") val refer: Refer
)

data class Now(
    @Json(name = "obsTime") val obsTime: String,
    @Json(name = "temp") val temp: String,
    @Json(name = "feelsLike") val feelsLike: String,
    @Json(name = "icon") val icon: String,
    @Json(name = "text") val text: String,
    @Json(name = "wind360") val wind360: String,
    @Json(name = "windDir") val windDir: String,
    @Json(name = "windScale") val windScale: String,
    @Json(name = "windSpeed") val windSpeed: String,
    @Json(name = "humidity") val humidity: String,
    @Json(name = "precip") val precip: String,
    @Json(name = "pressure") val pressure: String,
    @Json(name = "vis") val vis: String,
    @Json(name = "cloud") val cloud: String?,
    @Json(name = "dew") val dew: String?
)

data class Refer(
    @Json(name = "sources") val sources: List<String>?,
    @Json(name = "license") val license: List<String>?
)
