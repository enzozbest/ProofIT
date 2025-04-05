package prototype.services

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import prototype.helpers.LLMOptions
import prototype.helpers.LLMResponse
import prototype.helpers.OllamaOptions
import prototype.helpers.OllamaResponse
import utils.environment.EnvironmentLoader

/**
 * This class encapsulates all parameters needed to make a request to the Ollama API.
 *
 * @property model The identifier of the language model to use (e.g., "codellama:13b")
 * @property prompt The text prompt to send to the language model
 * @property stream Whether to stream the response (false for complete response at once)
 * @property options Additional parameters to control model behavior such as temperature and sampling
 */
@Serializable
data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean,
    val options: OllamaOptions = OllamaOptions(),
)

/**
 * Wrapper class for OllamaResponse that includes extracted templates.
 * This approach keeps the original OllamaResponse class unchanged for backward compatibility.
 *
 * @property response The original OllamaResponse from Ollama
 * @property extractedTemplates List of code templates extracted from the LLM response
 */
@Serializable
data class EnhancedResponse(
    val response: OllamaResponse?,
    val extractedTemplates: List<String> = emptyList(),
)

/**
 * Service for interacting with an Ollama instance
 */
object OllamaService : LLMService {
    private val jsonParser = Json { ignoreUnknownKeys = true }
    private const val OLLAMA_PORT = 11434
    private val OLLAMA_HOST = EnvironmentLoader.get("OLLAMA_HOST")
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
     * Sends a prompt to the language model and returns the generated response.
     *
     * @param prompt The text prompt to send to the language model
     * @param model The identifier of the language model to use
     * @param options Options for controlling the behavior of the language model
     * @return The response from the language model, or null if the request failed
     */
    override suspend fun generateResponse(
        prompt: String,
        model: String,
        options: LLMOptions
    ): Result<LLMResponse?> {
        if (options !is OllamaOptions) {
            return Result.failure(Exception("Invalid options type for OllamaService"))
        }

        val request = OllamaRequest(model = model, prompt = prompt, stream = false, options = options)
        return generateOllamaResponse(request)
    }

    /**
     * Sends a prompt to an LLM via Ollama and returns the generated response.
     *
     * @param request Input for the model formatted as an instance of [OllamaRequest].
     * @return Result object containing an instance of [OllamaResponse], or a failure with error message.
     */
    suspend fun generateOllamaResponse(request: OllamaRequest): Result<OllamaResponse?> {
        if (!isOllamaRunning()) {
            return Result.failure(Exception("Ollama is not running. Run: 'ollama serve' in terminal to start it."))
        }

        return try {
            Result.success(callOllama(request))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to call Ollama: ${e.message}"))
        }
    }

    /**
     * Makes a call to Ollama and parses the response.
     *
     * @param request An instance of [OllamaRequest] containing the prompt and model for evaluation.
     * @return An instance of [OllamaResponse] containing the LLM's response, or null if parsing fails.
     * @throws Exception on network errors
     */
    private suspend fun callOllama(request: OllamaRequest): OllamaResponse? {
        val ollamaApiUrl = "http://$OLLAMA_HOST:$OLLAMA_PORT/api/generate"
        val response: HttpResponse =
            client.post(ollamaApiUrl) {
                header(HttpHeaders.ContentType, "application/json")
                setBody(jsonParser.encodeToString<OllamaRequest>(request))
            }

        if (response.status != HttpStatusCode.OK) {
            throw Exception("HTTP error: ${response.status}")
        }
        val responseText = response.bodyAsText()
        val ollamaResponse =
            runCatching {
                jsonParser.decodeFromString<OllamaResponse>(responseText)
            }.getOrElse {
                throw Exception("Failed to parse Ollama response")
            }

        return ollamaResponse
    }
}
