package kcl.seg.rtt.prototype

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.serialization.json.Json
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.HttpTimeout

@Serializable
data class OllamaRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean
)

@Serializable
data class OllamaResponse(
    val response: String
)

@Serializable
data class FileContent(val content: String)

@Serializable
data class LlmResponse(
    val mainFile: String,
    val files: Map<String, FileContent>
)


class PrototypeService {
    private val client = HttpClient(CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }


        install(HttpTimeout) {
            requestTimeoutMillis = 600_000  // 10 minutes
            connectTimeoutMillis = 30_000   // 30 seconds
            socketTimeoutMillis = 600_000   // 10 minutes
        }
    }

    companion object {
        // Ollama defaults to this port
        private const val OLLAMA_PORT = 11434
    }

    private suspend fun isOllamaRunning(): Boolean {
        return try {
            val response = client.get("http://127.0.0.1:$OLLAMA_PORT")
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun generatePrototype(prompt: String): Result<LlmResponse> {
        if (!isOllamaRunning()) {
            return Result.failure(Exception("Ollama is not running. Run: 'ollama serve' in terminal to start it."))
        }

        return try {
            val llmResponse = callLLM(prompt)
            Result.success(llmResponse)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to call Ollama: ${e.message}"))
        }
    }

    private suspend fun callLLM(prompt: String): LlmResponse {
        println("Starting LLM call with prompt: $prompt")

        val ollamaRequest = OllamaRequest(
            model = "codellama:7b",
            prompt = """
            You are an AI that generates software prototypes formatted for WebContainers.
            Your output must be a JSON object containing:
            1. A 'mainFile' field specifying the main entry file (e.g., 'index.js').
            2. A 'files' object where each key is a filename and the value is an object containing a 'content' key with the file content.
            3. Include a 'package.json' file with required dependencies.
            4. Ensure all scripts use 'npm start' and static files are served correctly.
            
            Now, generate a project for the request: "$prompt"
        """.trimIndent(),
            stream = false
        )

        val ollamaApiUrl = "http://localhost:11434/api/generate"

        println("Sending request to Ollama...")

        val response: HttpResponse = client.post(ollamaApiUrl) {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(ollamaRequest)
        }

        // Parse JSON response from Ollama
        val responseText = response.bodyAsText()
        println("Ollama response: $responseText")

        val ollamaResponse = Json.decodeFromString<OllamaResponse>(responseText)

        return runCatching {
            Json.decodeFromString<LlmResponse>(ollamaResponse.response)
        }.getOrElse {
            throw IllegalArgumentException("Invalid JSON response from Ollama: ${it.message}")
        }
    }
}