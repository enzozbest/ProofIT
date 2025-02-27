import kotlinx.serialization.Serializable

object EmbeddingConstants {
    private const val EMBEDDING_SERVICE_URL = "http://localhost:7000/"
    const val EMBED_URL = "$EMBEDDING_SERVICE_URL/embed"
    const val EMBED_AND_STORE_URL = "$EMBEDDING_SERVICE_URL/new"
    const val SEMANTIC_SEARCH_URL = "$EMBEDDING_SERVICE_URL/search"
}

@Serializable
data class TemplateEmbedResponse(
    val status: String,
    val embedding: String? = null,
)

@Serializable
data class StoreTemplateResponse(
    val status: String,
    val message: String? = null,
)

@Serializable
data class TemplateSearchResponse(
    val status: String,
    val matches: List<String> = emptyList(),
)

@Serializable
data class SearchData(
    val embedding: List<Float>,
    val query: String,
)
