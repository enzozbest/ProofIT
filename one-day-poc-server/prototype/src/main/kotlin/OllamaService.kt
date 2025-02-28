package kcl.seg.rtt.prototype

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Request structure for Ollama API calls
 */
@Serializable
data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean,
)

/**
 * Response structure from Ollama API
 */
@Serializable
data class OllamaResponse(
    val model: String,
    val createdAt: String,
    val response: String,
    val done: Boolean,
    val doneReason: String,
)

/**
 * Service for interacting with a local Ollama instance
 */
object OllamaService {
    private val jsonParser = Json { ignoreUnknownKeys = true }
    private const val OLLAMA_PORT = 11434
    private const val OLLAMA_HOST = "127.0.0.1"

    val client =
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 600_000 // 10 minutes
                connectTimeoutMillis = 30_000 // 30 seconds
                socketTimeoutMillis = 600_000 // 10 minutes
            }
        }

    /**
     * Checks if local Ollama instance is accessible
     */
    suspend fun isOllamaRunning(): Boolean =
        try {
            val response = client.get("http://$OLLAMA_HOST:$OLLAMA_PORT/")
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }

    /**
     * Generates an LLM response using Ollama
     *
     * @param prompt Input prompt for the model
     * @return Result containing [LlmResponse] or failure with error message
     * @throws Exception if Ollama is not running or returns invalid response
     */
    suspend fun generateResponse(request: OllamaRequest): Result<LlmResponse> {
        if (!isOllamaRunning()) {
            return Result.failure(Exception("Ollama is not running. Run: 'ollama serve' in terminal to start it."))
        }

        return try {
            val response = callOllama(request.prompt, request.model)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to call Ollama: ${e.message}"))
        }
    }

    /**
     * Makes API call to Ollama and parses the response
     *
     * @param prompt Input prompt for the model
     * @return Parsed LLM response
     * @throws Exception on network errors or invalid JSON responses
     */
    private suspend fun callOllama(
        prompt: String,
        model: String,
    ): LlmResponse {
        val ollamaRequest =
            OllamaRequest(
                model = model,
                prompt = prompt,
                stream = false,
            )

        val ollamaApiUrl = "http://$OLLAMA_HOST:$OLLAMA_PORT/api/generate"

        val response: HttpResponse =
            runCatching {
                client.post(ollamaApiUrl) {
                    contentType(ContentType.Application.Json)
                    setBody(ollamaRequest)
                }
            }.getOrElse {
                println("Failed to call Ollama: ${it.message}")
                throw it
            }

        val responseText = response.bodyAsText()
        val ollamaResponse = jsonParser.decodeFromString<OllamaResponse>(responseText)

        // Only for debugging
        val jsonPrinter = Json { prettyPrint = true }
        println("Formatted JSON Response:\n" + jsonPrinter.encodeToString(ollamaResponse))

        return runCatching {
            Json.decodeFromString<LlmResponse>(ollamaResponse.response)
        }.getOrElse {
            throw IllegalArgumentException("Invalid JSON response from Ollama: ${it.message}")
        }
    }
}
