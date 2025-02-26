import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object EmbeddingService {
    internal var httpClient = HttpClient(CIO)

    init {
        initializeComponentLibrary()
    }

    private fun initializeComponentLibrary() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SeedingService.seedComponents()
                println("Component library successfully initialized")
            } catch (e: Exception) {
                println("ERROR: Failed to initialize component library: ${e.message}")
            }
        }
    }

    suspend fun embed(
        data: String,
        name: String,
    ): EmbeddingServiceResponse {
        val payload = mapOf("text" to data, "name" to name)
        val response =
            httpClient
                .post(EmbeddingConstants.EMBED_URL) {
                    setBody(Json.encodeToString(payload))
                }
        val responseText = response.bodyAsText()
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
        val response =
            httpClient
                .post(EmbeddingConstants.EMBED_AND_STORE_URL) {
                    setBody(Json.encodeToString(payload))
                }

        val responseText = response.bodyAsText()
        val storeResponse =
            runCatching { Json.decodeFromString<EmbeddingStoreResponse>(responseText) }.getOrElse {
                throw IllegalStateException("Failed to parse response!")
            }
        return storeResponse
    }

    suspend fun semanticSearch(embedding: List<Float>): SemanticSearchResponse {
        val payload = mapOf("embedding" to embedding)
        val response =
            httpClient
                .post(EmbeddingConstants.SEMANTIC_SEARCH_URL) {
                    setBody(Json.encodeToString(payload))
                }

        val responseText = response.bodyAsText()
        val searchResponse =
            runCatching { Json.decodeFromString<SemanticSearchResponse>(responseText) }.getOrElse {
                throw IllegalStateException("Failed to parse response!")
            }
        return searchResponse
    }
}
