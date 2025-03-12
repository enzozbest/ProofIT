package templates

import embeddings.EmbeddingConstants
import embeddings.SearchData
import embeddings.StoreTemplateResponse
import embeddings.TemplateEmbedResponse
import embeddings.TemplateSearchResponse
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Service responsible for interacting with the embedding service API.
 *
 * This singleton object provides methods for embedding template data,
 * storing templates, and performing semantic searches using embedded
 * vector representations.
 */
object TemplateService {
    internal var httpClient = HttpClient(CIO)

    /**
     * Embeds the given data and returns the embedding.
     *
     * This method sends a request to the embedding service to generate a
     * vector representation of the provided text data.
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
     * Stores the given template in both the embedding service and local storage.
     *
     * This method performs two operations:
     * 1. Sends the template to the embedding service to be embedded and stored for semantic search
     * 2. Creates a local record of the template for file path reference
     *
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
            try {
                Json.decodeFromString<StoreTemplateResponse>(responseText)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to parse response!", e)
            }

        val templateId = TemplateStorageService.createTemplate(fileURI)

        return storeResponse.copy(id = templateId)
    }

    /**
     * Performs a semantic search against stored templates using an embedding vector.
     *
     * This method sends a search request to the embedding service to find templates
     * that are semantically similar to the provided embedding vector.
     *
     * @param embedding The vector representation to search with
     * @param query The original text query corresponding to the embedding
     * @return Search response containing matching template identifiers
     * @throws IllegalStateException If the response from the embedding service cannot be parsed
     */
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
