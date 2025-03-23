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
import prototype.helpers.OllamaRequest
import prototype.helpers.OllamaResponse
import prototype.helpers.OllamaService
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
            // Arrange
            val testPrompt = "test prompt"
            "Test error"
            val options = OllamaOptions()

            // Create a mock engine that returns an error
            val mockEngine =
                MockEngine { request ->
                    respondError(HttpStatusCode.InternalServerError)
                }
            val client = HttpClient(mockEngine)

            // Replace the OllamaService client with our mock
            OllamaService.client = client

            // Mock OllamaService to ensure isOllamaRunning returns true
            // This way, the error will come from the HTTP call, not from the isOllamaRunning check
            mockkObject(OllamaService)
            coEvery { OllamaService.isOllamaRunning() } returns true

            println("[DEBUG_LOG] Mock setup complete")

            val testImpl = PrototypeMain(testModel)

            // Act & Assert
            var caughtException: Exception? = null

            try {
                println("[DEBUG_LOG] About to call prompt()")
                val result = testImpl.prompt(testPrompt, options)
                println("[DEBUG_LOG] prompt() returned successfully with result: $result")
                // If we reach here, the test should fail
                fail("Expected IllegalStateException to be thrown")
            } catch (e: IllegalStateException) {
                // This is the expected exception
                println("[DEBUG_LOG] Caught expected IllegalStateException: ${e.message}")
                caughtException = e
            } catch (e: Exception) {
                // Unexpected exception
                println("[DEBUG_LOG] Caught unexpected exception: ${e::class.simpleName} - ${e.message}")
                fail("Expected IllegalStateException but got ${e::class.simpleName}: ${e.message}")
            }

            // Verify the exception
            assertNotNull(caughtException)
            assertEquals("Failed to receive response from the LLM", caughtException?.message)

            // Clean up
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
            // Arrange
            val testPrompt = "test prompt"
            val options = OllamaOptions()

            // Create a mock engine that returns a valid JSON response but with null fields
            val mockEngine = MockEngine { request ->
                respond(
                    content = """{"model":"llama2","created_at":"2024-01-01T00:00:00Z","response":null,"done":true,"done_reason":"stop"}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
            val client = HttpClient(mockEngine)

            // Replace the OllamaService client with our mock
            OllamaService.client = client

            // Mock OllamaService to ensure isOllamaRunning returns true
            mockkObject(OllamaService)
            coEvery { OllamaService.isOllamaRunning() } returns true

            val testImpl = PrototypeMain(testModel)

            // Act
            val result = testImpl.prompt(testPrompt, options)

            // Assert
            assertNull(result, "Expected null response")

            // Clean up
            unmockkObject(OllamaService)
        }
    }
}
