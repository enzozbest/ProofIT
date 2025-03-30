package prototype

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import prototype.helpers.OllamaOptions
import prototype.helpers.OllamaResponse
import prototype.services.OllamaRequest
import prototype.services.OllamaService
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for the PrototypeMain class.
 *
 * Since we can't easily mock the OllamaService object (which is a singleton),
 * we use a test implementation of PrototypeMain that doesn't depend on OllamaService.
 */
class PrototypeMainTest {
    private val testModel = "llama2"

    /**
     * Test that the prompt method returns the response when the LLM call is successful.
     */
    @Test
    fun `prompt returns response when LLM call is successful`() {
        runBlocking {
            val testPrompt = "test prompt"
            val expectedResponse =
                OllamaResponse(
                    model = testModel,
                    created_at = "2024-01-01T00:00:00Z",
                    response = "test response",
                    done = true,
                    done_reason = "stop",
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

            mockkObject(OllamaService)
            coEvery {
                OllamaService.generateResponse(
                    OllamaRequest(
                        testPrompt,
                        testModel,
                        false,
                    ),
                )
            } returns Result.success(expectedResponse)

            val testImpl = PrototypeMain(testModel)
            val result = testImpl.prompt(testPrompt, OllamaOptions())

            assertNotNull(result)
            assertEquals(expectedResponse, result)

            unmockkObject(OllamaService)
        }
    }

    /**
     * Test that the prompt method throws an exception when the LLM call fails.
     */
    @Test
    fun `prompt throws exception when LLM call fails`() {
        runBlocking {
            val testPrompt = "test prompt"
            "Test error"
            val options = OllamaOptions()

            val mockEngine =
                MockEngine { request ->
                    respondError(HttpStatusCode.InternalServerError)
                }
            val client = HttpClient(mockEngine)

            OllamaService.client = client

            mockkObject(OllamaService)
            coEvery { OllamaService.isOllamaRunning() } returns true

            println("[DEBUG_LOG] Mock setup complete")

            val testImpl = PrototypeMain(testModel)

            var caughtException: Exception? = null

            try {
                println("[DEBUG_LOG] About to call prompt()")
                val result = testImpl.prompt(testPrompt, options)
                println("[DEBUG_LOG] prompt() returned successfully with result: $result")
                fail("Expected IllegalStateException to be thrown")
            } catch (e: IllegalStateException) {
                println("[DEBUG_LOG] Caught expected IllegalStateException: ${e.message}")
                caughtException = e
            } catch (e: Exception) {
                println("[DEBUG_LOG] Caught unexpected exception: ${e::class.simpleName} - ${e.message}")
                fail("Expected IllegalStateException but got ${e::class.simpleName}: ${e.message}")
            }

            assertNotNull(caughtException)
            assertEquals("Failed to receive response from the LLM", caughtException?.message)

            unmockkObject(OllamaService)
        }
    }

    /**
     * Test that the prompt method handles null response correctly.
     * This test is needed to achieve 100% branch coverage for the getOrNull() call.
     */
    @Test
    fun `prompt handles null response correctly`() {
        runBlocking {
            val testPrompt = "test prompt"
            val options = OllamaOptions()

            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = """{"model":"llama2","created_at":"2024-01-01T00:00:00Z","response":null,"done":true,"done_reason":"stop"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine)

            OllamaService.client = client

            mockkObject(OllamaService)
            coEvery { OllamaService.isOllamaRunning() } returns true

            val testImpl = PrototypeMain(testModel)

            val result = testImpl.prompt(testPrompt, options)

            assertNull(result, "Expected null response")

            unmockkObject(OllamaService)
        }
    }

    /**
     * Test that covers the branch where OllamaService.generateResponse has an exception.
     * This test is needed to achieve 100% branch coverage for line 29.
     */
    @Test
    fun `prompt handles exception during generateResponse`() {
        runBlocking {
            val testPrompt = "test prompt"
            val options = OllamaOptions()
            val testException = RuntimeException("Test exception")

            mockkObject(OllamaService)
            coEvery {
                OllamaService.generateResponse(any())
            } throws testException

            val testImpl = PrototypeMain(testModel)

            try {
                testImpl.prompt(testPrompt, options)
                fail("Expected exception to be thrown")
            } catch (e: RuntimeException) {
                assertEquals("Test exception", e.message)
            } finally {
                unmockkObject(OllamaService)
            }
        }
    }

    /**
     * Test that covers the branch where OllamaService.generateResponse throws a specific exception.
     * This test is needed to achieve 100% branch coverage for line 29.
     */
    @Test
    fun `prompt handles specific exception during generateResponse`() {
        runBlocking {
            val testPrompt = "test prompt"
            val options = OllamaOptions()

            val mockEngine =
                MockEngine { request ->
                    throw java.net.ConnectException("Connection refused")
                }
            val client = HttpClient(mockEngine)

            OllamaService.client = client

            mockkObject(OllamaService)
            coEvery { OllamaService.isOllamaRunning() } returns true

            val testImpl = PrototypeMain(testModel)

            try {
                testImpl.prompt(testPrompt, options)
                fail("Expected IllegalStateException to be thrown")
            } catch (e: IllegalStateException) {
                assertEquals("Failed to receive response from the LLM", e.message)
            } finally {
                unmockkObject(OllamaService)
            }
        }
    }

    /**
     * Test that covers the branch where response is not null and response.response is not null.
     * This test is needed to achieve 100% branch coverage for line 35.
     */
    @Test
    fun `prompt handles non-null response with non-null response field`() {
        runBlocking {
            val testPrompt = "test prompt"
            val options = OllamaOptions()
            val expectedResponse = "This is a test response"

            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = """{"model":"llama2","created_at":"2024-01-01T00:00:00Z","response":"$expectedResponse","done":true,"done_reason":"stop"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine)

            OllamaService.client = client

            mockkObject(OllamaService)
            coEvery { OllamaService.isOllamaRunning() } returns true

            val testImpl = PrototypeMain(testModel)

            val result = testImpl.prompt(testPrompt, options)

            assertNotNull(result, "Expected non-null result")
            assertEquals(expectedResponse, result.response, "Expected response to match")

            unmockkObject(OllamaService)
        }
    }

    /**
     * Test that attempts to cover all branches in a single test.
     * This is a comprehensive test that tries to hit all code paths.
     */
    @Test
    fun `prompt comprehensive test for all branches`() {
        runBlocking {
            val testPrompt = "test prompt"
            val options = OllamaOptions()
            val expectedResponse = "This is a test response"

            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = """{"model":"llama2","created_at":"2024-01-01T00:00:00Z","response":"$expectedResponse","done":true,"done_reason":"stop"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine)

            OllamaService.client = client

            mockkObject(OllamaService)

            coEvery { OllamaService.isOllamaRunning() } returns true

            val testImpl = PrototypeMain(testModel)

            val result = testImpl.prompt(testPrompt, options)

            assertNotNull(result, "Expected non-null result")
            assertEquals(expectedResponse, result?.response, "Expected response to match")

            coEvery { OllamaService.isOllamaRunning() } returns false

            try {
                testImpl.prompt(testPrompt, options)
                fail("Expected IllegalStateException to be thrown")
            } catch (e: IllegalStateException) {
                assertEquals("Failed to receive response from the LLM", e.message)
            }

            unmockkObject(OllamaService)
        }
    }

    /**
     * Test that covers the branch where llmResponse.getOrNull() returns null.
     * This test is needed to achieve 100% branch coverage for line 33.
     */
    @Test
    fun `prompt handles getOrNull returning null`() {
        runBlocking {
            val testPrompt = "test prompt"
            val options = OllamaOptions()

            val mockEngine =
                MockEngine { request ->
                    respondError(HttpStatusCode.InternalServerError)
                }
            val client = HttpClient(mockEngine)

            OllamaService.client = client

            mockkObject(OllamaService)
            coEvery { OllamaService.isOllamaRunning() } returns true

            val testImpl = PrototypeMain(testModel)

            try {
                testImpl.prompt(testPrompt, options)
                fail("Expected IllegalStateException to be thrown")
            } catch (e: IllegalStateException) {
                assertEquals("Failed to receive response from the LLM", e.message)
            } finally {
                unmockkObject(OllamaService)
            }
        }
    }

    /**
     * Test that covers the branch where response is not null but response.response is null.
     * This test is needed to achieve 100% branch coverage for line 35.
     */
    @Test
    fun `prompt handles non-null response with null response field`() {
        runBlocking {
            val testPrompt = "test prompt"
            val options = OllamaOptions()

            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = """{"model":"llama2","created_at":"2024-01-01T00:00:00Z","response":null,"done":true,"done_reason":"stop"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = HttpClient(mockEngine)

            OllamaService.client = client

            mockkObject(OllamaService)
            coEvery { OllamaService.isOllamaRunning() } returns true

            val testImpl = PrototypeMain(testModel)

            val result = testImpl.prompt(testPrompt, options)

            assertNull(result, "Expected null result when response.response is null")

            unmockkObject(OllamaService)
        }
    }
}
