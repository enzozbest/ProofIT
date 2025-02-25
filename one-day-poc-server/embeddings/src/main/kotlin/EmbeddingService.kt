import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object EmbeddingService {
    private val httpClient by lazy { HttpClient(CIO) }

    suspend fun embed(
        data: String,
        name: String,
    ): EmbeddingServiceResponse {
        val payload = mapOf("text" to data, "name" to name)
        val responseText =
            httpClient
                .post(EmbeddingConstants.EMBED_URL) {
                    setBody(Json.encodeToString(payload))
                }.bodyAsText()
        val embedResponse =
            runCatching { Json.decodeFromString<EmbeddingServiceResponse>(responseText) }.getOrElse {
                throw IllegalStateException("Failed to parse response!")
            }
        return embedResponse
    }

    suspend fun embedAndStore(
        name: String,
        data: String,
    ): EmbeddingStoreResponse {
        val payload = mapOf("name" to name, "text" to data)
        val responseText =
            httpClient
                .post(EmbeddingConstants.EMBED_AND_STORE_URL) {
                    setBody(Json.encodeToString(payload))
                }.bodyAsText()

        val storeResponse =
            runCatching { Json.decodeFromString<EmbeddingStoreResponse>(responseText) }.getOrElse {
                throw IllegalStateException("Failed to parse response!")
            }
        return storeResponse
    }

    suspend fun semanticSearch(embedding: List<Float>): SemanticSearchResponse {
        val payload = mapOf("embedding" to embedding)
        val responseText =
            httpClient
                .post(EmbeddingConstants.SEMANTIC_SEARCH_URL) {
                    setBody(Json.encodeToString(payload))
                }.bodyAsText()
        val searchResponse =
            runCatching { Json.decodeFromString<SemanticSearchResponse>(responseText) }.getOrElse {
                throw IllegalStateException("Failed to parse response!")
            }
        return searchResponse
    }
}
