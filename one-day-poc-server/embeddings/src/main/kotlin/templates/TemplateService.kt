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
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
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
    private const val EXCEPTION_COULD_NOT_STORE_TEMPLATE = "Failed to store template!"

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
                error(EXCEPTION_COULD_NOT_STORE_TEMPLATE)
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
     * @param fileURI file path for the template.
     * @param data The data to store (JSON-LD annotation of a template).
     */
    suspend fun storeTemplate(
        fileURI: String,
        data: String,
    ): StoreTemplateResponse {
        val templateId =
            TemplateStorageService.createTemplate(fileURI) ?: error(EXCEPTION_COULD_NOT_STORE_TEMPLATE)

        val remoteResponse = storeTemplateEmbedding(templateId, data)
        val success = remoteResponse.status == HttpStatusCode.OK
        return if (success) {
            Json.decodeFromString<StoreTemplateResponse>(remoteResponse.bodyAsText()).copy(id = templateId)
        } else {
            error(EXCEPTION_COULD_NOT_STORE_TEMPLATE)
        }
    }

    suspend fun storeTemplateEmbedding(
        name: String,
        data: String,
    ): HttpResponse {
        val payload = mapOf("name" to name, "text" to data)
        val response =
            httpClient
                .post(EmbeddingConstants.EMBED_AND_STORE_URL) {
                    setBody(Json.encodeToString(payload))
                }
        return if (response.status == HttpStatusCode.OK) {
            response
        } else {
            error(EXCEPTION_COULD_NOT_STORE_TEMPLATE)
        }
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
                error(EXCEPTION_COULD_NOT_STORE_TEMPLATE)
            }
        return searchResponse
    }
}
