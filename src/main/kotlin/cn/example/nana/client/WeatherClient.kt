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
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

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
    private var sk: String,
    @Value("\${weather.sub}")
    private var sub: String,
    @Value("\${weather.kid}")
    private var kid: String,
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
            val call = weatherApi.getCurrentWeather(location, lang, unit, "Bearer ${this.createJwtKey()}")
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


    private fun createJwtKey():String{
        var privateKeyString = this.sk
        privateKeyString =
            privateKeyString.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "")
                .trim { it <= ' ' }
        val privateKeyBytes = Base64.getDecoder().decode(privateKeyString)
        val keySpec = PKCS8EncodedKeySpec(privateKeyBytes)
        val keyFactory = KeyFactory.getInstance("EdDSA")
        val privateKey = keyFactory.generatePrivate(keySpec)


        val headerJson = "{\"alg\": \"EdDSA\", \"kid\": \"$kid\"}"


        val iat = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond() - 30
        val exp = iat + 900
        val payloadJson = "{\"sub\": \"$sub\", \"iat\": $iat, \"exp\": $exp}"

        val headerEncoded = Base64.getUrlEncoder().encodeToString(headerJson.toByteArray(StandardCharsets.UTF_8))
        val payloadEncoded = Base64.getUrlEncoder().encodeToString(payloadJson.toByteArray(StandardCharsets.UTF_8))
        val data = "$headerEncoded.$payloadEncoded"


        val signer = Signature.getInstance("EdDSA")
        signer.initSign(privateKey)
        signer.update(data.toByteArray(StandardCharsets.UTF_8))
        val signature = signer.sign()

        val signatureString = Base64.getUrlEncoder().encodeToString(signature)

        val jwt = "$data.$signatureString"

        return jwt
    }
}