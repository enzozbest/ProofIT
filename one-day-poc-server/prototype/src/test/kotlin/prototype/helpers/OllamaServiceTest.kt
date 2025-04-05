package kcl.seg.rtt.prototype.prototype.helpers

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
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import prototype.helpers.OllamaOptions
import prototype.helpers.OllamaResponse
import prototype.services.OllamaRequest
import prototype.services.OllamaService

class OllamaServiceTest {
    @Test
    fun `Test OllamaService returns false if Ollama isn't running`() =
        runBlocking {
            val client =
                HttpClient(MockEngine) {
                    engine {
                        addHandler { respondError(HttpStatusCode.ServiceUnavailable) }
                    }
                }
            OllamaService.client = client
            val request = OllamaRequest("model", "prompt", false)

            val response = OllamaService.generateResponse(request)
            assertTrue(response.isFailure)
            assertEquals(
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
                MockEngine {
                    respond(
                        content = Json.encodeToString(OllamaResponse.serializer(), expectedResponse),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = HttpClient(mockEngine)
            OllamaService.client = client

            val request = OllamaRequest("model", "prompt", false)
            val response = OllamaService.generateResponse(request)

            assertTrue(response.isSuccess)
            val result = response.getOrNull()
            assertNotNull(result)
            assertEquals(expectedResponse.model, result?.model)
            assertEquals(expectedResponse.response, result?.response)
            assertEquals(expectedResponse.done, result?.done)
            assertEquals(expectedResponse.done_reason, result?.done_reason)
        }

    @Test
    fun `Test getClient in OllamaService`() {
        val client = OllamaService.client
        assertEquals(client.engine, client.engine)
    }

    @Test
    fun `Test isOllamaRunning returns false when Ollama is unreachable`() =
        runBlocking {
            val client =
                HttpClient(MockEngine) {
                    engine {
                        addHandler {
                            throw Exception("Connection refused")
                        }
                    }
                }
            OllamaService.client = client

            val result = OllamaService.isOllamaRunning()
            assertFalse(result)
        }

    @Test
    fun `Test generateResponse fails when call fails`() =
        runBlocking {
            mockkObject(OllamaService)
            coEvery { OllamaService.isOllamaRunning() } returns false
            val client =
                HttpClient(MockEngine) {
                    engine {
                        addHandler {
                            respondError(HttpStatusCode.InternalServerError)
                        }
                    }
                }
            OllamaService.client = client
            val request = OllamaRequest("model", "prompt", false)
            val response = OllamaService.generateResponse(request)
            assertTrue(response.isFailure)
            assertEquals(
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
                MockEngine { request ->
                    respond(
                        content = Json.encodeToString(OllamaResponse.serializer(), expectedResponse),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = HttpClient(mockEngine)
            OllamaService.client = client

            val request = OllamaRequest("model", "prompt", false)
            val response = OllamaService.generateResponse(request)

            assertTrue(response.isSuccess)
            val result = response.getOrNull()
            assertNotNull(result)
            assertEquals(expectedResponse.model, result?.model)
            assertEquals(expectedResponse.response, result?.response)
            assertEquals(expectedResponse.done, result?.done)
            assertEquals(expectedResponse.done_reason, result?.done_reason)
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
                MockEngine {
                    respond(
                        content = Json.encodeToString(OllamaResponse.serializer(), expectedResponse),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = HttpClient(mockEngine)
            OllamaService.client = client

            val request = OllamaRequest("model", "prompt", false)
            val response = OllamaService.generateResponse(request)

            assertTrue(response.isSuccess)
            val result = response.getOrNull()
            assertNotNull(result)
            assertEquals(expectedResponse.model, result?.model)
            assertEquals(expectedResponse.response, result?.response)
            assertEquals(expectedResponse.done, result?.done)
            assertEquals(expectedResponse.done_reason, result?.done_reason)
        }

    @Test
    fun `Test OllamaService fails on an invalid response`() =
        runBlocking {
            val mockEngine =
                MockEngine {
                    respond(
                        content = "{ invalid json }",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }

            val client = HttpClient(mockEngine)
            OllamaService.client = client

            val request = OllamaRequest("model", "prompt", false)
            val response = OllamaService.generateResponse(request)
            val exception = response.exceptionOrNull()
            assertEquals("Failed to call Ollama: Failed to parse Ollama response", exception?.message)
        }

    @Test
    fun `Test callOllama handles exceptions successfully`() =
        runBlocking {
            val mockEngine =
                MockEngine {
                    throw Exception("Network error")
                }

            val client = HttpClient(mockEngine)
            mockkObject(OllamaService)
            coEvery { OllamaService.isOllamaRunning() } returns true

            OllamaService.client = client

            val request = OllamaRequest("model", "prompt", false)

            val response = OllamaService.generateResponse(request)

            assertTrue(response.isFailure)

            val resultException = response.exceptionOrNull()
            assertNotNull(resultException)
            assertEquals("Failed to call Ollama: Network error", resultException?.message)
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

        assertEquals("llama2", response.model)
        assertEquals("2023-01-01T12:00:00Z", response.created_at)
        assertEquals("This is a test response", response.response)
        assertTrue(response.done)
        assertEquals("stop", response.done_reason)

        val copiedResponse = response.copy(model = "gpt4", response = "Updated response")
        assertEquals("gpt4", copiedResponse.model)
        assertEquals("Updated response", copiedResponse.response)
        assertEquals(response.created_at, copiedResponse.created_at)
        assertEquals(response.done, copiedResponse.done)
        assertEquals(response.done_reason, copiedResponse.done_reason)

        val sameResponse =
            OllamaResponse(
                model = "llama2",
                created_at = "2023-01-01T12:00:00Z",
                response = "This is a test response",
                done = true,
                done_reason = "stop",
            )
        assertEquals(response, sameResponse)
        assertNotEquals(response, copiedResponse)

        val toStringResult = response.toString()
        assertTrue(toStringResult.contains("model=llama2"))
        assertTrue(toStringResult.contains("created_at=2023-01-01T12:00:00Z"))
        assertTrue(toStringResult.contains("response=This is a test response"))
        assertTrue(toStringResult.contains("done=true"))
        assertTrue(toStringResult.contains("done_reason=stop"))
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

        val json = Json.encodeToString(OllamaResponse.serializer(), original)

        assertTrue(json.contains("\"model\":\"llama2\""))
        assertTrue(json.contains("\"created_at\":\"2023-01-01T12:00:00Z\""))
        assertTrue(json.contains("\"response\":\"This is a test response\""))
        assertTrue(json.contains("\"done\":true"))
        assertTrue(json.contains("\"done_reason\":\"stop\""))

        val deserialized = Json.decodeFromString(OllamaResponse.serializer(), json)

        assertEquals(original, deserialized)
        assertEquals(original.model, deserialized.model)
        assertEquals(original.created_at, deserialized.created_at)
        assertEquals(original.response, deserialized.response)
        assertEquals(original.done, deserialized.done)
        assertEquals(original.done_reason, deserialized.done_reason)
    }

    @Test
    fun `Test OllamaOptions data class properties and behavior`() {
        val defaultOptions = OllamaOptions()
        assertNull(defaultOptions.temperature)
        assertNull(defaultOptions.top_k)
        assertNull(defaultOptions.top_p)
        assertNull(defaultOptions.num_predict)
        assertNull(defaultOptions.stop)

        val options =
            OllamaOptions(
                temperature = 0.7,
                top_k = 40,
                top_p = 0.9,
                num_predict = 100,
                stop = listOf(".", "?", "!"),
            )

        assertEquals(0.7, options.temperature)
        assertEquals(40, options.top_k)
        assertEquals(0.9, options.top_p)
        assertEquals(100, options.num_predict)
        assertEquals(listOf(".", "?", "!"), options.stop)

        val copiedOptions = options.copy(temperature = 0.5, top_k = 50)
        assertEquals(0.5, copiedOptions.temperature)
        assertEquals(50, copiedOptions.top_k)
        assertEquals(options.top_p, copiedOptions.top_p)
        assertEquals(options.num_predict, copiedOptions.num_predict)
        assertEquals(options.stop, copiedOptions.stop)

        val sameOptions =
            OllamaOptions(
                temperature = 0.7,
                top_k = 40,
                top_p = 0.9,
                num_predict = 100,
                stop = listOf(".", "?", "!"),
            )
        assertEquals(options, sameOptions)
        assertNotEquals(options, copiedOptions)
        assertNotEquals(options, defaultOptions)

        val toStringResult = options.toString()
        assertTrue(toStringResult.contains("temperature=0.7"))
        assertTrue(toStringResult.contains("top_k=40"))
        assertTrue(toStringResult.contains("top_p=0.9"))
        assertTrue(toStringResult.contains("num_predict=100"))
        assertTrue(toStringResult.contains("stop=[., ?, !]"))
    }

    @Test
    fun `Test OllamaOptions serialization and deserialization`() {
        val original =
            OllamaOptions(
                temperature = 0.7,
                top_k = 40,
                top_p = 0.9,
                num_predict = 100,
                stop = listOf(".", "?", "!"),
            )

        val json = Json.encodeToString(OllamaOptions.serializer(), original)

        assertTrue(json.contains("\"temperature\":0.7"))
        assertTrue(json.contains("\"top_k\":40"))
        assertTrue(json.contains("\"top_p\":0.9"))
        assertTrue(json.contains("\"num_predict\":100"))
        assertTrue(json.contains("\"stop\":[\".\",\"?\",\"!\"]"))

        val deserialized = Json.decodeFromString(OllamaOptions.serializer(), json)

        assertEquals(original, deserialized)
        assertEquals(original.temperature, deserialized.temperature)
        assertEquals(original.top_k, deserialized.top_k)
        assertEquals(original.top_p, deserialized.top_p)
        assertEquals(original.num_predict, deserialized.num_predict)
        assertEquals(original.stop, deserialized.stop)
    }

    @Test
    fun `Test OllamaOptions with null values serialization and deserialization`() {
        val original =
            OllamaOptions(
                temperature = 0.7,
                top_k = null,
                top_p = 0.9,
                num_predict = null,
                stop = null,
            )

        val json = Json.encodeToString(OllamaOptions.serializer(), original)

        assertTrue(json.contains("\"temperature\":0.7"))
        assertTrue(json.contains("\"top_p\":0.9"))
        assertTrue(json.contains("\"top_k\":null") || !json.contains("\"top_k\""))
        assertTrue(json.contains("\"num_predict\":null") || !json.contains("\"num_predict\""))
        assertTrue(json.contains("\"stop\":null") || !json.contains("\"stop\""))

        val deserialized = Json.decodeFromString(OllamaOptions.serializer(), json)

        assertEquals(original, deserialized)
        assertEquals(original.temperature, deserialized.temperature)
        assertEquals(original.top_k, deserialized.top_k)
        assertEquals(original.top_p, deserialized.top_p)
        assertEquals(original.num_predict, deserialized.num_predict)
        assertEquals(original.stop, deserialized.stop)
    }
}
