package kcl.seg.rtt.chat.utils

import kotlinx.serialization.json.Json

object KeywordLoader {
    private val keywords: List<String> by lazy {
        val fileContent = this::class.java.getResource("/keywords.json")?.readText() ?: "[]"
        Json.decodeFromString<KeywordList>(fileContent).keywords
    }

    fun getKeywords(): List<String> = keywords
}

@kotlinx.serialization.Serializable
data class KeywordList(val keywords: List<String>)
