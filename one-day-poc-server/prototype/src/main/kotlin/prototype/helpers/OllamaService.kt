package prototype.helpers

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import utils.environment.EnvironmentLoader

/**
 * Request structure for Ollama API calls
 *
 */
@Serializable
data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean,
    val options: OllamaOptions = OllamaOptions(),
)

/**
 * Response structure from Ollama API
 */
@Serializable
data class OllamaResponse(
    val model: String,
    val created_at: String,
    val response: String,
    val done: Boolean,
    val done_reason: String,
)

@Serializable
data class OllamaOptions(
    val temperature: Double? = null,
    val top_k: Int? = null,
    val top_p: Double? = null,
    val num_predict: Int? = null,
    val stop: List<String>? = null,
)

/**
 * Service for interacting with an Ollama instance
 */
object OllamaService {
    private val jsonParser = Json { ignoreUnknownKeys = true }
    private const val OLLAMA_PORT = 11434
    private val OLLAMA_HOST = EnvironmentLoader.get("OLLAMA_HOST").also { println(it) }
    private const val REQUEST_TIMEOUT_MILLIS = 600_000_000L
    private const val CONNECT_TIMEOUT_MILLIS = 30_000_000L
    private const val SOCKET_TIMEOUT_MILLIS = 600_000_000L

    var client =
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
            }
        }

    /**
     * Check if local Ollama instance is accessible.
     * @return true if Ollama is running, false otherwise.
     */
    suspend fun isOllamaRunning(): Boolean =
        runCatching {
            val response = client.get("http://$OLLAMA_HOST:$OLLAMA_PORT/")
            response.status == HttpStatusCode.OK
        }.getOrElse { false }

    /**
     * Sends a prompt to an LLM via Ollama and returns the generated response.
     *
     * @param request Input for the model formatted as an instance of [OllamaRequest].
     * @return Result object containing an instance of [OllamaResponse], or a failure with error message.
     */
    suspend fun generateResponse(request: OllamaRequest): Result<OllamaResponse> {
        if (!isOllamaRunning()) {
            return Result.failure(Exception("Ollama is not running. Run: 'ollama serve' in terminal to start it."))
        }

        return runCatching {
            val response = callOllama(request)
            Result.success(response)
        }.getOrElse {
            Result.failure(Exception("Failed to call Ollama: ${it.message}"))
        }
    }

    /**
     * Makes a call to Ollama and parses the response.
     *
     * @param request An instance of [OllamaRequest] containing the prompt and model for evaluation.
     * @return An instance of [OllamaResponse] containing the LLM's response.
     * @throws Exception on network errors or invalid JSON responses
     */
    private suspend fun callOllama(request: OllamaRequest): OllamaResponse {
        val ollamaApiUrl = "http://$OLLAMA_HOST:$OLLAMA_PORT/api/generate"
        val response: HttpResponse =
            runCatching {
                client.post(ollamaApiUrl) {
                    header(HttpHeaders.ContentType, "application/json")
                    setBody(jsonParser.encodeToString<OllamaRequest>(request))
                }
            }.getOrElse {
                println("Failed to call Ollama: ${it.message}")
                throw it
            }
        val responseText = response.bodyAsText()
        val ollamaResponse = runCatching { jsonParser.decodeFromString<OllamaResponse>(responseText) }.getOrNull()

        // Only for debugging
        val jsonPrinter = Json { prettyPrint = true }
        println("Formatted JSON Response:\n" + jsonPrinter.encodeToString(ollamaResponse))
        return ollamaResponse ?: throw SerializationException("Failed to parse Ollama response")
    }
}
