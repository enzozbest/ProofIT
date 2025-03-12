package prompting.helpers.promptEngineering

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Collections

object KeywordLoader {
    private val keywords: List<String> by lazy {
        val fileContent = this::class.java.getResource("/keywords.json")?.readText() ?: "[]"
        Collections.unmodifiableList(Json.decodeFromString<KeywordList>(fileContent).keywords)
    }

    fun getKeywordsList(): List<String> = keywords
}

@Serializable
data class KeywordList(
    @SerialName("keywords")
    private val keywordsIn: List<String>,
) {
    val keywords: List<String> get() = Collections.unmodifiableList(keywordsIn)

    override fun toString(): String = "KeywordList(keywords=$keywordsIn)"
}
