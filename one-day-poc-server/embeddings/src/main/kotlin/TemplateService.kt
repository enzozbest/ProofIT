import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
        val storeResponse =
            runCatching { Json.decodeFromString<StoreTemplateResponse>(responseText) }.getOrElse {
                throw IllegalStateException("Failed to parse response!")
            }

        val templateResult = TemplateStorageService.createTemplate(fileURI)

        println("templateResult: $templateResult")

        if (templateResult.isSuccess) {
            val template = templateResult.getOrNull()
            if (template != null) {
                return storeResponse.copy(id = template.id)
            }
        }

        val exception = templateResult.exceptionOrNull()
            ?: Exception("Unknown error during template creation")
        throw IllegalStateException("Failed to store template in database: ${exception.message}", exception)
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
