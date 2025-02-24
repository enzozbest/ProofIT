import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

object EmbeddingConstants {
    const val EMBEDDING_SERVICE_URL = "http://localhost:7000/embeddings/"
    const val EMBED_URL = "$EMBEDDING_SERVICE_URL/embed"
    const val EMBED_AND_STORE_URL = "$EMBEDDING_SERVICE_URL/new"
}

@Serializable
data class EmbedResponse(
    val status: String,
    val embedding: String? = null,
)

object EmbeddingService {
    private val httpClient by lazy { HttpClient(CIO) }

    suspend fun <T> embed(
        data: T,
        name: String,
    ): List<Float> {
        val responseText =
            httpClient
                .post(EmbeddingConstants.EMBED_URL) {
                    setBody(Json.encodeToString(mapOf("text" to data.toString(), "name" to name)))
                }.bodyAsText()

        val embedResponse = runCatching { Json.decodeFromString<EmbedResponse>(responseText) }.getOrNull()
        return embedResponse?.let { response ->
            check(response.status == "success") { "Failed to embed provided data!" }
            response.embedding?.let { embedding ->
                embedding.split(",").map { it.toFloat() }
            } ?: emptyList()
        } ?: emptyList()
    }

    suspend fun <T> embedAndStore(
        name: String,
        data: T,
    ): Boolean {
        val responseText =
            httpClient
                .post(EmbeddingConstants.EMBED_AND_STORE_URL) {
                    setBody(Json.encodeToString(mapOf("name" to name, "text" to data.toString())))
                }.bodyAsText()
        val json = runCatching { Json.decodeFromString<JsonObject>(responseText) }.getOrNull()
        return json?.let { response -> response["status"]?.let { it.jsonPrimitive.content == "success" } } ?: false
    }

    suspend fun getEmbedding(name: String): List<Float> = emptyList()

    suspend fun semanticSearch(embedding: List<Float>): List<String> {
    }
}
