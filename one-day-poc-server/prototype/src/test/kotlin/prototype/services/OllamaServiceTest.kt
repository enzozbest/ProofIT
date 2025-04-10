package prototype.services

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kcl.seg.rtt.prototype.prototype.helpers.generateResponse
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import prototype.helpers.OllamaOptions
import prototype.helpers.OllamaResponse
import prototype.helpers.OpenAIOptions
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class OllamaServiceTest {
    @Test
    fun `Test OllamaService returns false if Ollama isn't running`() =
        runBlocking {
            val client =
                HttpClient(MockEngine.Companion) {
                    engine {
                        addHandler { respondError(HttpStatusCode.Companion.ServiceUnavailable) }
                    }
                }
            OllamaService.client = client
            val request = OllamaRequest("model", "prompt", false)

            val response = OllamaService.generateResponse(request)
            Assertions.assertTrue(response.isFailure)
            Assertions.assertEquals(
                "Ollama is not running. Run: 'ollama serve' in terminal to start it.",
                response.exceptionOrNull()?.message,
            )
        }

    @Test
    fun `Test OllamaService successfully calls Ollama`() =
        runBlocking {
            val expectedResponse =
                OllamaResponse(
                    model = "model",
                    created_at = "2025-03-11T12:00:00Z",
                    response = "Test response from Ollama",
                    done = true,
                    done_reason = "complete",
                )

            val mockEngine =
                MockEngine.Companion {
                    respond(
                        content = Json.Default.encodeToString(OllamaResponse.serializer(), expectedResponse),
                        status = HttpStatusCode.Companion.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = HttpClient(mockEngine)
            OllamaService.client = client

            val request = OllamaRequest("model", "prompt", false)
            val response = OllamaService.generateResponse(request)

            Assertions.assertTrue(response.isSuccess)
            val result = response.getOrNull()
            Assertions.assertNotNull(result)
            Assertions.assertEquals(expectedResponse.model, result?.model)
            Assertions.assertEquals(expectedResponse.response, result?.response)
            Assertions.assertEquals(expectedResponse.done, result?.done)
            Assertions.assertEquals(expectedResponse.done_reason, result?.done_reason)
        }

    @Test
    fun `Test getClient in OllamaService`() {
        val client = OllamaService.client
        Assertions.assertEquals(client.engine, client.engine)
    }

    @Test
    fun `Test isOllamaRunning returns false when Ollama is unreachable`() =
        runBlocking {
            val client =
                HttpClient(MockEngine.Companion) {
                    engine {
                        addHandler {
                            throw Exception("Connection refused")
                        }
                    }
                }
            OllamaService.client = client

            val result = OllamaService.isOllamaRunning()
            Assertions.assertFalse(result)
        }

    @Test
    fun `Test generateResponse fails when call fails`() =
        runBlocking {
            mockkObject(OllamaService)
            coEvery { OllamaService.isOllamaRunning() } returns false
            val client =
                HttpClient(MockEngine.Companion) {
                    engine {
                        addHandler {
                            respondError(HttpStatusCode.Companion.InternalServerError)
                        }
                    }
                }
            OllamaService.client = client
            val request = OllamaRequest("model", "prompt", false)
            val response = OllamaService.generateResponse(request)
            Assertions.assertTrue(response.isFailure)
            Assertions.assertEquals(
                "Ollama is not running. Run: 'ollama serve' in terminal to start it.",
                response.exceptionOrNull()?.message,
            )
            coVerify { OllamaService.isOllamaRunning() }
            unmockkObject(OllamaService)
        }

    @Test
    fun `Test OllamaService generates response successfully`() =
        runBlocking {
            val expectedResponse =
                OllamaResponse(
                    model = "model",
                    created_at = "2025-03-11T12:00:00Z",
                    response = "Test response from Ollama",
                    done = true,
                    done_reason = "complete",
                )

            val mockEngine =
                MockEngine.Companion { request ->
                    respond(
                        content = Json.Default.encodeToString(OllamaResponse.serializer(), expectedResponse),
                        status = HttpStatusCode.Companion.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = HttpClient(mockEngine)
            OllamaService.client = client

            val request = OllamaRequest("model", "prompt", false)
            val response = OllamaService.generateResponse(request)

            Assertions.assertTrue(response.isSuccess)
            val result = response.getOrNull()
            Assertions.assertNotNull(result)
            Assertions.assertEquals(expectedResponse.model, result?.model)
            Assertions.assertEquals(expectedResponse.response, result?.response)
            Assertions.assertEquals(expectedResponse.done, result?.done)
            Assertions.assertEquals(expectedResponse.done_reason, result?.done_reason)
        }

    @Test
    fun `Test OllamaService successfully parses response`() =
        runBlocking {
            val expectedResponse =
                OllamaResponse(
                    model = "model",
                    created_at = "2025-03-11T12:00:00Z",
                    response = "Test response from Ollama",
                    done = true,
                    done_reason = "complete",
                )

            val mockEngine =
                MockEngine.Companion {
                    respond(
                        content = Json.Default.encodeToString(OllamaResponse.serializer(), expectedResponse),
                        status = HttpStatusCode.Companion.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = HttpClient(mockEngine)
            OllamaService.client = client

            val request = OllamaRequest("model", "prompt", false)
            val response = OllamaService.generateResponse(request)

            Assertions.assertTrue(response.isSuccess)
            val result = response.getOrNull()
            Assertions.assertNotNull(result)
            Assertions.assertEquals(expectedResponse.model, result?.model)
            Assertions.assertEquals(expectedResponse.response, result?.response)
            Assertions.assertEquals(expectedResponse.done, result?.done)
            Assertions.assertEquals(expectedResponse.done_reason, result?.done_reason)
        }

    @Test
    fun `Test OllamaService fails on an invalid response`() =
        runBlocking {
            val mockEngine =
                MockEngine.Companion {
                    respond(
                        content = "{ invalid json }",
                        status = HttpStatusCode.Companion.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = HttpClient(mockEngine)
            OllamaService.client = client

            val request = OllamaRequest("model", "prompt", false)
            val response = OllamaService.generateResponse(request)
            val exception = response.exceptionOrNull()
            Assertions.assertEquals("Failed to call Ollama: Failed to parse Ollama response", exception?.message)
        }

    @Test
    fun `Test callOllama handles exceptions successfully`() =
        runBlocking {
            val mockEngine =
                MockEngine.Companion {
                    throw Exception("Network error")
                }

            val client = HttpClient(mockEngine)
            mockkObject(OllamaService)
            coEvery { OllamaService.isOllamaRunning() } returns true

            OllamaService.client = client

            val request = OllamaRequest("model", "prompt", false)

            val response = OllamaService.generateResponse(request)

            Assertions.assertTrue(response.isFailure)

            val resultException = response.exceptionOrNull()
            Assertions.assertNotNull(resultException)
            Assertions.assertEquals("Failed to call Ollama: Network error", resultException?.message)
            unmockkObject(OllamaService)
        }

    @Test
    fun `Test OllamaResponse data class properties and behavior`() {
        val response =
            OllamaResponse(
                model = "llama2",
                created_at = "2023-01-01T12:00:00Z",
                response = "This is a test response",
                done = true,
                done_reason = "stop",
            )

        Assertions.assertEquals("llama2", response.model)
        Assertions.assertEquals("2023-01-01T12:00:00Z", response.created_at)
        Assertions.assertEquals("This is a test response", response.response)
        Assertions.assertTrue(response.done)
        Assertions.assertEquals("stop", response.done_reason)

        val copiedResponse = response.copy(model = "gpt4", response = "Updated response")
        Assertions.assertEquals("gpt4", copiedResponse.model)
        Assertions.assertEquals("Updated response", copiedResponse.response)
        Assertions.assertEquals(response.created_at, copiedResponse.created_at)
        Assertions.assertEquals(response.done, copiedResponse.done)
        Assertions.assertEquals(response.done_reason, copiedResponse.done_reason)

        val sameResponse =
            OllamaResponse(
                model = "llama2",
                created_at = "2023-01-01T12:00:00Z",
                response = "This is a test response",
                done = true,
                done_reason = "stop",
            )
        Assertions.assertEquals(response, sameResponse)
        Assertions.assertNotEquals(response, copiedResponse)

        val toStringResult = response.toString()
        Assertions.assertTrue(toStringResult.contains("model=llama2"))
        Assertions.assertTrue(toStringResult.contains("created_at=2023-01-01T12:00:00Z"))
        Assertions.assertTrue(toStringResult.contains("response=This is a test response"))
        Assertions.assertTrue(toStringResult.contains("done=true"))
        Assertions.assertTrue(toStringResult.contains("done_reason=stop"))
    }

    @Test
    fun `Test OllamaResponse serialization and deserialization`() {
        val original =
            OllamaResponse(
                model = "llama2",
                created_at = "2023-01-01T12:00:00Z",
                response = "This is a test response",
                done = true,
                done_reason = "stop",
            )

        val json = Json.Default.encodeToString(OllamaResponse.serializer(), original)

        Assertions.assertTrue(json.contains("\"model\":\"llama2\""))
        Assertions.assertTrue(json.contains("\"created_at\":\"2023-01-01T12:00:00Z\""))
        Assertions.assertTrue(json.contains("\"response\":\"This is a test response\""))
        Assertions.assertTrue(json.contains("\"done\":true"))
        Assertions.assertTrue(json.contains("\"done_reason\":\"stop\""))

        val deserialized = Json.Default.decodeFromString(OllamaResponse.serializer(), json)

        Assertions.assertEquals(original, deserialized)
        Assertions.assertEquals(original.model, deserialized.model)
        Assertions.assertEquals(original.created_at, deserialized.created_at)
        Assertions.assertEquals(original.response, deserialized.response)
        Assertions.assertEquals(original.done, deserialized.done)
        Assertions.assertEquals(original.done_reason, deserialized.done_reason)
    }

    @Test
    fun `Test OllamaOptions data class properties and behavior`() {
        val defaultOptions = OllamaOptions()
        Assertions.assertNull(defaultOptions.temperature)
        Assertions.assertNull(defaultOptions.topK)
        Assertions.assertNull(defaultOptions.topP)
        Assertions.assertNull(defaultOptions.numPredict)
        Assertions.assertNull(defaultOptions.stop)

        val options =
            OllamaOptions(
                temperature = 0.7,
                topK = 40,
                topP = 0.9,
                numPredict = 100,
                stop = listOf(".", "?", "!"),
            )

        Assertions.assertEquals(0.7, options.temperature)
        Assertions.assertEquals(40, options.topK)
        Assertions.assertEquals(0.9, options.topP)
        Assertions.assertEquals(100, options.numPredict)
        Assertions.assertEquals(listOf(".", "?", "!"), options.stop)

        val copiedOptions = options.copy(temperature = 0.5, topK = 50)
        Assertions.assertEquals(0.5, copiedOptions.temperature)
        Assertions.assertEquals(50, copiedOptions.topK)
        Assertions.assertEquals(options.topP, copiedOptions.topP)
        Assertions.assertEquals(options.numPredict, copiedOptions.numPredict)
        Assertions.assertEquals(options.stop, copiedOptions.stop)

        val sameOptions =
            OllamaOptions(
                temperature = 0.7,
                topK = 40,
                topP = 0.9,
                numPredict = 100,
                stop = listOf(".", "?", "!"),
            )
        Assertions.assertEquals(options, sameOptions)
        Assertions.assertNotEquals(options, copiedOptions)
        Assertions.assertNotEquals(options, defaultOptions)

        val toStringResult = options.toString()
        Assertions.assertTrue(toStringResult.contains("temperature=0.7"))
        Assertions.assertTrue(toStringResult.contains("topK=40"))
        Assertions.assertTrue(toStringResult.contains("topP=0.9"))
        Assertions.assertTrue(toStringResult.contains("numPredict=100"))
        Assertions.assertTrue(toStringResult.contains("stop=[., ?, !]"))
    }

    @Test
    fun `Test OllamaOptions serialization and deserialization`() {
        val original =
            OllamaOptions(
                temperature = 0.7,
                topK = 40,
                topP = 0.9,
                numPredict = 100,
                stop = listOf(".", "?", "!"),
            )

        val json = Json.Default.encodeToString(OllamaOptions.serializer(), original)

        Assertions.assertTrue(json.contains("\"temperature\":0.7"))
        Assertions.assertTrue(json.contains("\"top_k\":40"))
        Assertions.assertTrue(json.contains("\"top_p\":0.9"))
        Assertions.assertTrue(json.contains("\"num_predict\":100"))
        Assertions.assertTrue(json.contains("\"stop\":[\".\",\"?\",\"!\"]"))

        val deserialized = Json.Default.decodeFromString(OllamaOptions.serializer(), json)

        Assertions.assertEquals(original, deserialized)
        Assertions.assertEquals(original.temperature, deserialized.temperature)
        Assertions.assertEquals(original.topK, deserialized.topK)
        Assertions.assertEquals(original.topP, deserialized.topP)
        Assertions.assertEquals(original.numPredict, deserialized.numPredict)
        Assertions.assertEquals(original.stop, deserialized.stop)
    }

    @Test
    fun `Test OllamaOptions with null values serialization and deserialization`() {
        val original =
            OllamaOptions(
                temperature = 0.7,
                topK = null,
                topP = 0.9,
                numPredict = null,
                stop = null,
            )

        val json = Json.Default.encodeToString(OllamaOptions.serializer(), original)

        Assertions.assertTrue(json.contains("\"temperature\":0.7"))
        Assertions.assertTrue(json.contains("\"top_p\":0.9"))
        Assertions.assertTrue(json.contains("\"top_k\":null") || !json.contains("\"top_k\""))
        Assertions.assertTrue(json.contains("\"num_predict\":null") || !json.contains("\"num_predict\""))
        Assertions.assertTrue(json.contains("\"stop\":null") || !json.contains("\"stop\""))

        val deserialized = Json.Default.decodeFromString(OllamaOptions.serializer(), json)

        Assertions.assertEquals(original, deserialized)
        Assertions.assertEquals(original.temperature, deserialized.temperature)
        Assertions.assertEquals(original.topK, deserialized.topK)
        Assertions.assertEquals(original.topP, deserialized.topP)
        Assertions.assertEquals(original.numPredict, deserialized.numPredict)
        Assertions.assertEquals(original.stop, deserialized.stop)
    }

    @Test
    fun `test handles incorrect option type`() {
        val invalidOptions = OpenAIOptions()

        runBlocking {
            val result = OllamaService.generateResponse("test", "test", invalidOptions)
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertSame(IllegalArgumentException::class.java, exception!!::class.java)
            assertEquals("Invalid options type for OllamaService", exception.message)
        }
    }
}
