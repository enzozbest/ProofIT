import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

object VectorDatabaseClient {
    private val client = HttpClient(CIO)

    /**
     * Store a new vector embedding in the vector store.
     */
    suspend fun storeEmbedding(
        name: String,
        vector: List<Float>,
    ): Boolean {
        val jsonData =
            Json.encodeToString(
                mapOf(
                    "name" to name,
                    "vector" to vector,
                ),
            )
        val response =
            client.post(jsonData) {
                url("http://localhost:7000/embeddings/new")
            }
        val json = Json.decodeFromString<JsonObject>(response.bodyAsText())
        return json["status"]?.jsonPrimitive?.content == "success"
    }

    /**
     * Query vector store for the closest matching vectors.
     */
    suspend fun queryEmbeddings(
        vector: List<Float>,
        topK: Int = 5,
    ): List<String> {
        val jsonData = Json.encodeToString(mapOf("vector" to vector, "topK" to topK))
        val response =
            client.post("http://localhost:7000/embeddings/") {
                setBody(jsonData)
            }
        val json = Json.decodeFromString<JsonObject>(response.bodyAsText())
        return json["matches"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
    }
}
