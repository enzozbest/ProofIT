package helpers

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object KeywordLoader {
    private val keywords: List<String> by lazy {
        val fileContent = this::class.java.getResource("/keywords.json")?.readText() ?: "[]"
        Json.decodeFromString<KeywordList>(fileContent).keywords
    }

    fun getKeywordsList(): List<String> = keywords
}

@Serializable
data class KeywordList(
    val keywords: List<String>,
)
