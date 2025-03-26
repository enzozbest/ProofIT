package prompting.helpers.promptEngineering

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL
import java.util.Collections

/**
 * Utility singleton responsible for loading and providing access to the application's keywords list.
 *
 * This object loads keywords from a predefined JSON resource file using lazy initialization,
 * ensuring the file is read only once when first needed. The keywords are stored as an
 * immutable list to prevent modifications after loading.
 */
object KeywordLoader {
    private lateinit var keywordsInternal: List<String>

    /**
     * Flag to indicate if the keywords need to be reloaded.
     * This is used by the resetKeywords method to force reloading of the keywords.
     */
    private var needsReload = false

    /**
     * Resets the internal keywords list, forcing it to be reloaded on the next call to getKeywordsList.
     * This method is primarily used for testing purposes.
     */
    internal fun resetKeywords() {
        needsReload = true
    }

    /**
     * Provides access to the loaded keywords list.
     *
     * @return An immutable list of keywords that can be safely used throughout the application
     */
    fun getKeywordsList(): List<String> {
        if (!::keywordsInternal.isInitialized || needsReload) {
            val fileContent = getResource("/keywords.json")?.readText() ?: """{"keywords":[]}"""
            needsReload = false
            return Collections
                .unmodifiableList(Json.decodeFromString<KeywordList>(fileContent).keywords)
                .also { keywordsInternal = it }
        }
        return keywordsInternal
    }

    internal fun getResource(resource: String): URL? = this::class.java.getResource(resource)
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
