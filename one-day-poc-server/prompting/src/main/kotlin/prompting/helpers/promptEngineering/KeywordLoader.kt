package prompting.helpers.promptEngineering

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Collections

/**
 * Utility singleton responsible for loading and providing access to the application's keywords list.
 *
 * This object loads keywords from a predefined JSON resource file using lazy initialization,
 * ensuring the file is read only once when first needed. The keywords are stored as an
 * immutable list to prevent modifications after loading.
 */
object KeywordLoader {
    private val keywords: List<String> by lazy {
        val fileContent = this::class.java.getResource("/keywords.json")?.readText() ?: "[]"
        Collections.unmodifiableList(Json.decodeFromString<KeywordList>(fileContent).keywords)
    }

    /**
     * Provides access to the loaded keywords list.
     *
     * @return An immutable list of keywords that can be safely used throughout the application
     */
    fun getKeywordsList(): List<String> = keywords
}

/**
 * A container class for deserializing the keywords JSON file.
 *
 * This serializable data class handles the JSON structure of the keywords resource file
 * and provides additional protection by ensuring all access to the keywords list
 * returns immutable collections.
 *
 * @property keywordsIn The internal storage for the deserialized keywords list
 */
@Serializable
data class KeywordList(
    @SerialName("keywords")
    private val keywordsIn: List<String>,
) {
    val keywords: List<String> get() = Collections.unmodifiableList(keywordsIn)

    override fun toString(): String = "KeywordList(keywords=$keywordsIn)"
}
