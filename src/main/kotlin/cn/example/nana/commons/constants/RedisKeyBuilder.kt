package cn.example.nana.commons.constants

/**
 * @Author  RichardYoung
 * @Description
 * @Date  2025/4/2 21:45
 */
object RedisKeyBuilder {

    private const val PREFIX="nana:"

    fun buildKeyWordsKey():String{
        return "${PREFIX}keywords"
    }

    fun buildSessionKeywordsKey(sessionId: String): String {
        return "${PREFIX}keywords:$sessionId"
    }

    fun buildWebSearchKeywordsKey(): String {
        return "${PREFIX}web_search_keywords"
    }

}