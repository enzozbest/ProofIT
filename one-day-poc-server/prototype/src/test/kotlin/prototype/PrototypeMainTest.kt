package prototype

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import prototype.helpers.OllamaRequest
import prototype.helpers.OllamaResponse
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
            // Arrange
            val testPrompt = "test prompt"
            val expectedResponse = OllamaResponse(
                model = testModel,
                created_at = "2024-01-01T00:00:00Z",
                response = "test response",
                done = true,
                done_reason = "stop"
            )

            // Create a test implementation that returns a successful Result
            val testImpl = PrototypeMainTestImpl(testModel) { request ->
                // Verify the request parameters
                assertEquals(testPrompt, request.prompt)
                assertEquals(testModel, request.model)
                assertEquals(false, request.stream)

                // Return a successful Result
                Result.success(expectedResponse)
            }

            // Act
            val result = testImpl.prompt(testPrompt)

            // Assert
            assertNotNull(result)
            assertEquals(expectedResponse, result)
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

            // Create a test implementation that returns a failure Result
            val testImpl = PrototypeMainTestImpl(testModel) { request ->
                // Verify the request parameters
                assertEquals(testPrompt, request.prompt)
                assertEquals(testModel, request.model)
                assertEquals(false, request.stream)

                // Return a failure Result
                Result.failure(RuntimeException(errorMessage))
            }

            // Act & Assert
            val exception = assertThrows<IllegalStateException> {
                testImpl.prompt(testPrompt)
            }

            assertEquals("Failed to receive response from the LLM", exception.message)
        }
    }
}
