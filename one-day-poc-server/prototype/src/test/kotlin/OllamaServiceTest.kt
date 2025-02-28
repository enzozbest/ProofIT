import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kcl.seg.rtt.prototype.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.*

class OllamaServiceTest {
    @Test
    fun `test generateResponse handles invalid JSON`() =
        testApplication {
            val invalidJsonEngine =
                MockEngine {
                    respond(
                        content = """{
                    "model": "codellama:7b",
                    "created_at": "2023-01-01T00:00:00Z",
                    "response": "INVALID_JSON",
                    "done": true,
                    "done_reason": "completed"
                }""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }

            val invalidService =
                OllamaService().apply {
                    client =
                        HttpClient(invalidJsonEngine) {
                            install(ContentNegotiation) {
                                json(Json { ignoreUnknownKeys = true })
                            }
                        }
                }

            val result = invalidService.generateResponse("test")
            assertTrue(result.isFailure)
            assertContains(
                result.exceptionOrNull()?.message ?: "",
                "Invalid JSON response from Ollama",
            )
        }

    @Test
    fun `test generateResponse when Ollama is not running`() =
        testApplication {
            val unhealthyEngine =
                MockEngine {
                    respond("Not Found", HttpStatusCode.NotFound)
                }

            val unhealthyService =
                OllamaService().apply {
                    client = HttpClient(unhealthyEngine)
                }

            val result = unhealthyService.generateResponse("test")
            assertTrue(result.isFailure)
            assertEquals(
                "Ollama is not running. Run: 'ollama serve' in terminal to start it.",
                result.exceptionOrNull()?.message,
            )
        }

    @Test
    fun `test generateResponse handles HTTP errors`() =
        testApplication {
            val errorEngine =
                MockEngine { request ->
                    when (request.url.encodedPath) {
                        "/" -> respond("Not Found", HttpStatusCode.NotFound) // Changed this line
                        "/api/generate" -> respond("Internal Error", HttpStatusCode.InternalServerError)
                        else -> respond("Not Found", HttpStatusCode.NotFound)
                    }
                }

            val errorService =
                OllamaService().apply {
                    client =
                        HttpClient(errorEngine) {
                            install(ContentNegotiation) {
                                json(Json { ignoreUnknownKeys = true })
                            }
                        }
                }

            val result = errorService.generateResponse("test")
            assertTrue(result.isFailure)
            assertContains(
                result.exceptionOrNull()?.message ?: "",
                "Ollama is not running",
            )
        }

    @Test
    fun `test successful response parsing`() =
        testApplication {
            val successEngine =
                MockEngine { request ->
                    when (request.url.encodedPath) {
                        "/" ->
                            respond(
                                content = "",
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                            )

                        "/api/generate" -> {
                            // This is the inner LlmResponse that would be in the 'response' field
                            val llmResponse =
                                LlmResponse(
                                    mainFile = "index.html",
                                    files =
                                        mapOf(
                                            "index.html" to FileContent("<html>Test</html>"),
                                        ),
                                )

                            // Convert LlmResponse to JSON string
                            val llmResponseJson = Json.encodeToString(llmResponse)

                            // Create the outer OllamaResponse
                            val ollamaResponse =
                                OllamaResponse(
                                    model = "codellama:7b",
                                    createdAt = "2023-01-01T00:00:00Z",
                                    response = llmResponseJson,
                                    done = true,
                                    doneReason = "completed",
                                )

                            // Convert the entire response to JSON
                            val responseJson = Json.encodeToString(ollamaResponse)

                            respond(
                                content = responseJson,
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                            )
                        }

                        else ->
                            respond(
                                content = "Not Found",
                                status = HttpStatusCode.NotFound,
                            )
                    }
                }

            val successService =
                OllamaService().apply {
                    client =
                        HttpClient(successEngine) {
                            install(ContentNegotiation) {
                                json(
                                    Json {
                                        prettyPrint = true
                                        isLenient = true
                                        ignoreUnknownKeys = true
                                    },
                                )
                            }
                        }
                }

            val result = successService.generateResponse("valid prompt")

            // Debug output
            println("Result: $result")
            if (result.isFailure) {
                println("Error: ${result.exceptionOrNull()?.message}")
                result.exceptionOrNull()?.printStackTrace()
            }

            assertTrue(result.isSuccess)
            assertEquals("index.html", result.getOrNull()?.mainFile)
            assertEquals(
                "<html>Test</html>",
                result
                    .getOrNull()
                    ?.files
                    ?.get("index.html")
                    ?.content,
            )
        }

    @Test
    fun `test generateResponse handles network exception during API call`() =
        testApplication {
            val throwingEngine =
                MockEngine { request ->
                    when (request.url.encodedPath) {
                        // Health check should succeed so that generateResponse proceeds
                        "/" ->
                            respond(
                                content = "",
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                            )
                        // When calling the generation endpoint, throw an exception to simulate a network error
                        "/api/generate" -> throw Exception("Simulated network error")
                        else -> respond("Not Found", HttpStatusCode.NotFound)
                    }
                }

            val throwingService =
                OllamaService().apply {
                    client =
                        HttpClient(throwingEngine) {
                            install(ContentNegotiation) {
                                json(Json { ignoreUnknownKeys = true })
                            }
                        }
                }

            val result = throwingService.generateResponse("test prompt")

            assertTrue(result.isFailure)
            assertContains(result.exceptionOrNull()?.message ?: "", "Simulated network error")
        }
}
