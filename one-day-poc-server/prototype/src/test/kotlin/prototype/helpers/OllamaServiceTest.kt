package kcl.seg.rtt.prototype.prototype.helpers

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import io.mockk.*
import prototype.helpers.OllamaRequest
import prototype.helpers.OllamaResponse
import prototype.helpers.OllamaService


class OllamaServiceTest {
    @Test
    fun `Test OllamaService returns false if Ollama isn't running`() = runBlocking {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { respondError(HttpStatusCode.ServiceUnavailable) }
            }
        }
        OllamaService.client = client
        val request = OllamaRequest("model", "prompt", false)

        val response = OllamaService.generateResponse(request)
        assertTrue(response.isFailure)
        assertEquals("Ollama is not running. Run: 'ollama serve' in terminal to start it.", response.exceptionOrNull()?.message)
    }

    @Test
    fun `Test OllamaService successfully calls Ollama`() = runBlocking {
        val expectedResponse = OllamaResponse(
            model = "model",
            created_at = "2025-03-11T12:00:00Z",
            response = "Test response from Ollama",
            done = true,
            done_reason = "complete"
        )

        val mockEngine = MockEngine {
            respond(
                content = Json.encodeToString(OllamaResponse.serializer(),expectedResponse),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
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
    fun `Test isOllamaRunning returns false when Ollama is unreachable`() = runBlocking {
        val client = HttpClient(MockEngine) {
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
    fun `Test generateResponse fails when call fails`() = runBlocking {
        mockkObject(OllamaService)
        coEvery { OllamaService.isOllamaRunning() } returns false
        val client = HttpClient(MockEngine) {
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
        assertEquals("Ollama is not running. Run: 'ollama serve' in terminal to start it.", response.exceptionOrNull()?.message)
        coVerify { OllamaService.isOllamaRunning() }
        unmockkObject(OllamaService)
    }

    @Test
    fun `Test OllamaService generates response successfully`() = runBlocking {
        val expectedResponse = OllamaResponse(
            model = "model",
            created_at = "2025-03-11T12:00:00Z",
            response = "Test response from Ollama",
            done = true,
            done_reason = "complete"
        )

        val mockEngine = MockEngine { request ->
            respond(
                content = Json.encodeToString(OllamaResponse.serializer(), expectedResponse),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
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
    fun `Test OllamaService successfully parses response`() = runBlocking {
        val expectedResponse = OllamaResponse(
            model = "model",
            created_at = "2025-03-11T12:00:00Z",
            response = "Test response from Ollama",
            done = true,
            done_reason = "complete"
        )

        val mockEngine = MockEngine {
            respond(
                content = Json.encodeToString(OllamaResponse.serializer(),expectedResponse),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
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
    fun `Test OllamaService fails on an invalid response`() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = "{ invalid json }",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
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
    fun `Test callOllama handles exceptions successfully`() = runBlocking {
        val mockEngine = MockEngine {
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

}
