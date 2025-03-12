package embeddings

import kotlinx.serialization.Serializable

/**
 * Contains constant URL values for accessing the Embedding Service.
 *
 * This object centralizes the API endpoints used for communication with the
 * template embedding service, ensuring consistency throughout the application.
 */
internal object EmbeddingConstants {
    private const val EMBEDDING_SERVICE_URL = "http://localhost:7000/"
    const val EMBED_URL = "$EMBEDDING_SERVICE_URL/embed"
    const val EMBED_AND_STORE_URL = "$EMBEDDING_SERVICE_URL/new"
    const val SEMANTIC_SEARCH_URL = "$EMBEDDING_SERVICE_URL/search"
}

/**
 * Represents a response from the embedding generation endpoint.
 *
 * This class encapsulates the result of generating an embedding vector
 * from template text.
 *
 * @property status The status of the embedding request ("success" or error code)
 * @property embedding The generated embedding as a serialized string, or null if failed
 */
@Serializable
data class TemplateEmbedResponse(
    val status: String,
    val embedding: List<Float> = emptyList(),
)

/**
 * Represents a response from storing a template with its embedding.
 *
 * This class encapsulates the result of adding a new template to the
 * embedding database.
 *
 * @property status The status of the storage request ("success" or error code)
 * @property id The unique identifier assigned to the stored template, or null if failed
 * @property message Optional message providing additional information about the result
 */
@Serializable
data class StoreTemplateResponse(
    val status: String,
    val id: String? = null,
    val message: String? = null,
)

/**
 * Represents a response from a semantic search operation.
 *
 * This class encapsulates the results of searching for templates
 * based on semantic similarity.
 *
 * @property status The status of the search request ("success" or error code)
 * @property matches A list of template identifiers matching the search criteria
 */
@Serializable
data class TemplateSearchResponse(
    val status: String,
    val matches: List<String> = emptyList(),
)

/**
 * Represents a request for semantic search.
 *
 * This class encapsulates the necessary data for performing a semantic
 * search against stored templates.
 *
 * @property embedding The numerical vector representation used for similarity comparison
 * @property query The original text query that was converted to the embedding
 */
@Serializable
internal data class SearchData(
    val embedding: List<Float>,
    val query: String,
)
