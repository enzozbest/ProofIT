import kotlinx.serialization.Serializable

object EmbeddingConstants {
    private const val EMBEDDING_SERVICE_URL = "http://localhost:7000/embeddings/"
    const val EMBED_URL = "$EMBEDDING_SERVICE_URL/embed"
    const val EMBED_AND_STORE_URL = "$EMBEDDING_SERVICE_URL/new"
    const val SEMANTIC_SEARCH_URL = "$EMBEDDING_SERVICE_URL/semantic-search"
}

@Serializable
data class EmbeddingServiceResponse(
    val status: String,
    val embedding: String? = null,
)

@Serializable
data class EmbeddingStoreResponse(
    val status: String,
    val message: String? = null,
)

@Serializable
data class SemanticSearchResponse(
    val status: String,
    val matches: List<String> = emptyList(),
)
