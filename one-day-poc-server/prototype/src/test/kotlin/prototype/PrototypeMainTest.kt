package prototype

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import prototype.helpers.OllamaRequest
import prototype.helpers.OllamaResponse
import prototype.helpers.OllamaService
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import io.mockk.coEvery
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.serialization.json.Json


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
            val expectedResponse = OllamaResponse(
                model = testModel,
                created_at = "2024-01-01T00:00:00Z",
                response = "test response",
                done = true,
                done_reason = "stop"
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

            mockkObject(OllamaService)
            coEvery { OllamaService.generateResponse(OllamaRequest(testPrompt, testModel, false)) } returns Result.success(expectedResponse)

            val testImpl = PrototypeMain(testModel)
            val result = testImpl.prompt(testPrompt)

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
            val errorMessage = "Test error"

            // Mock OllamaService to return a failure result
            mockkObject(OllamaService)
            coEvery { OllamaService.generateResponse(OllamaRequest(testPrompt, testModel, false)) } returns Result.failure(RuntimeException(errorMessage))

            val testImpl = PrototypeMain(testModel)

            // Act & Assert
            val exception = assertThrows<IllegalStateException> {
                runBlocking {
                    testImpl.prompt(testPrompt)
                }
            }

            assertEquals("Failed to receive response from the LLM", exception.message)
        }
    }


}
