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

class PrototypeService {
    private val client = HttpClient(CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }


    companion object {
        // Ollama defaults to this port
        private const val OLLAMA_PORT = 11434
    }

    @Serializable
    data class LlmRequest(
        val model: String = "codellama:7b",
        val prompt: String
    )

    @Serializable
    data class LlmResponse(
        val response: String
    )

    private suspend fun isOllamaRunning(): Boolean {
        return try {
            val response = client.get("http://127.0.0.1:$OLLAMA_PORT")
            response.status == HttpStatusCode.OK
        } catch (e: Exception) {
            false
        }
    }

    suspend fun generatePrototype(prompt: String): String {
        if (isOllamaRunning()) {
             val llmResponse = callLLM(prompt)
             return llmResponse
        } else {
            return "Ollama is not running. Run: 'ollama serve' in terminal to start ollama locally."
        }

    }

    private suspend fun callLLM(prompt: String): String {
        val response = client.post("http://localhost:11434/api/generate") {
            contentType(ContentType.Application.Json)
            setBody(LlmRequest(prompt = prompt))
        }
        return response.body<LlmResponse>().response
    }
}