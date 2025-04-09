package cn.example.nana.commons.utils

import cn.example.nana.commons.aop.Slf4j.Companion.log
import cn.hutool.core.util.IdUtil
import org.springframework.util.ResourceUtils
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/8 16:46
 */
object PictureUtils {

    fun saveImageCache(imageUrl: String):String{
        val base64 = this.transPictureURL2Base64(imageUrl)
        return transBase642LocalPicture(base64,IdUtil.simpleUUID())
    }


    private fun transPictureURL2Base64(imageUrl: String): String {
        val url = URL(imageUrl)
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
        connection.setRequestMethod("GET")

        val outputStream = ByteArrayOutputStream()
        connection.inputStream.use { inputStream ->
            val buffer = ByteArray(4096)
            var bytesRead: Int
            while ((inputStream.read(buffer).also { bytesRead = it }) != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
        }
        val imageBytes: ByteArray = outputStream.toByteArray()

        val base64Image: String = Base64.getEncoder().encodeToString(imageBytes)

        val formattedBase64 = "data:image/jpeg;base64,$base64Image"

        return formattedBase64
    }

    private fun transBase642LocalPicture(base64: String, fileName: String):String{

        var base64Data = base64
        if (base64.contains(",")) {
            base64Data = base64.substring(base64.indexOf(",") + 1)
        }

        val imageBytes = Base64.getDecoder().decode(base64Data)


        var staticPath: String
        try {
            val path = File(ResourceUtils.getURL("classpath:static").path)
            staticPath = path.absolutePath
        } catch (e: Exception) {
            val path = File("src/main/resources/static")
            staticPath = path.absolutePath
        }

        val directory = File(staticPath)
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val imageFile = File(directory, "${fileName}.png")
        FileOutputStream(imageFile).use { fos ->
            fos.write(imageBytes)
        }

        log.info("图片已经保存到 ${imageFile.absolutePath}")

        return "/static/${fileName}.png"

    }

}

