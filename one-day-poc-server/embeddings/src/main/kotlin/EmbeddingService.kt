import io.ktor.client.*
import io.ktor.client.engine.cio.*

object EmbeddingConstants {
    const val EMBEDDING_SERVICE_URL = "http://localhost:7000/embeddings/embed"
}

object EmbeddingService {
    private val httpClient by lazy { HttpClient(CIO) }

    suspend fun <T> embed(): List<Float> = emptyList()

    suspend fun embedAndStore(name: String, code: String): Boolean = false

    suspend fun queryEmbeddings(): List<String> = emptyList()
}
