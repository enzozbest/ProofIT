import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tables.templates.Template

object TemplateService {
    internal var httpClient = HttpClient(CIO)

    /**
     * Embeds the given data and returns the embedding.
     *
     * @param data The data to embed.
     * @param name The identifier for the data.
     * @return The embedding of the data as a [TemplateEmbedResponse].
     */
    suspend fun embed(
        data: String,
        name: String,
    ): TemplateEmbedResponse {
        val payload = mapOf("text" to data, "name" to name)
        val response =
            httpClient
                .post(EmbeddingConstants.EMBED_URL) {
                    setBody(Json.encodeToString(payload))
                }
        val responseText = response.bodyAsText()
        val embedResponse =
            runCatching { Json.decodeFromString<TemplateEmbedResponse>(responseText) }.getOrElse {
                throw IllegalStateException("Failed to parse response!")
            }
        return embedResponse
    }

    /**
     * Stores the given template.
     * The template will be stored for keyword search, and embedded and stored for semantic search.
     * @param name The identifier of the template.
     * @param fileURI file path for the template.
     * @param data The data to store (JSON-LD annotation of a template).
     */
    suspend fun storeTemplate(
        name: String,
        fileURI: String,
        data: String,
    ): StoreTemplateResponse {
        val payload = mapOf("name" to name, "text" to data)
        val response =
            httpClient
                .post(EmbeddingConstants.EMBED_AND_STORE_URL) {
                    setBody(Json.encodeToString(payload))
                }

        val responseText = response.bodyAsText()

        val storeResponse = try {
            Json.decodeFromString<StoreTemplateResponse>(responseText)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to parse response!", e)
        }

        val templateId = storeAndGetId(fileURI)

        return storeResponse.copy(id = templateId)
    }

    internal suspend fun storeAndGetId(fileURI: String): String? {
        val templateResult = TemplateStorageService.createTemplate(fileURI)

        if (templateResult.isFailure) {
            val exception = templateResult.exceptionOrNull()
                ?: Exception("Unknown error during template creation")
            throw IllegalStateException("Failed to store template in database: ${exception.message}", exception)
        }

        return templateResult.getOrNull()?.id
    }

    suspend fun search(
        embedding: List<Float>,
        query: String,
    ): TemplateSearchResponse {
        val payload = SearchData(embedding, query)
        val response =
            httpClient
                .post(EmbeddingConstants.SEMANTIC_SEARCH_URL) {
                    setBody(Json.encodeToString(payload))
                }

        val responseText = response.bodyAsText()
        val searchResponse =
            runCatching { Json.decodeFromString<TemplateSearchResponse>(responseText) }.getOrElse {
                throw IllegalStateException("Failed to parse response!")
            }
        return searchResponse
    }
}
