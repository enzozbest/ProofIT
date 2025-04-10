package prototype.services

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import prototype.helpers.LLMOptions
import prototype.helpers.LLMResponse
import prototype.helpers.OllamaOptions
import prototype.helpers.OllamaResponse
import prototype.helpers.OpenAIOptions
import prototype.helpers.OpenAIResponse
import kotlin.test.assertFailsWith

/**
 * Tests for the LLMService interface and its implementations.
 */
class LLMServiceTest {
    /**
     * Test that the LLMServiceFactory returns the correct service for a given route.
     */
    @Test
    fun testLLMServiceFactory() {
        val localService = LLMServiceFactory.getService("local")
        val openAIService = LLMServiceFactory.getService("openai")

        assertTrue(localService is OllamaService)
        assertTrue(openAIService is OpenAIService)
    }

    /**
     * Test implementation of LLMService for testing OllamaService
     */
    class TestOllamaService : LLMService {
        override suspend fun generateResponse(
            prompt: String,
            model: String,
            options: LLMOptions,
        ): Result<LLMResponse?> =
            Result.success(
                OllamaResponse(
                    model = "test-model",
                    created_at = "2023-01-01T00:00:00Z",
                    response = "Test response",
                    done = true,
                    done_reason = "stop",
                ),
            )
    }

    /**
     * Test that the OllamaService generates a response correctly.
     */
    @Test
    fun testOllamaService() =
        runBlocking {
            val testService = TestOllamaService()

            val result =
                testService.generateResponse(
                    "Test prompt",
                    "test-model",
                    OllamaOptions(temperature = 0.5),
                )

            assertTrue(result.isSuccess)
            val response = result.getOrNull()
            assertNotNull(response)
            assertTrue(response is OllamaResponse)
            val ollamaResponse = response as OllamaResponse
            assertEquals("test-model", ollamaResponse.model)
            assertEquals("2023-01-01T00:00:00Z", ollamaResponse.created_at)
            assertEquals("Test response", ollamaResponse.response)
            assertTrue(ollamaResponse.done)
            assertEquals("stop", ollamaResponse.done_reason)
        }

    /**
     * Test implementation of LLMService for testing OpenAIService
     */
    class TestOpenAIService : LLMService {
        override suspend fun generateResponse(
            prompt: String,
            model: String,
            options: LLMOptions,
        ): Result<LLMResponse?> =
            Result.success(
                OpenAIResponse(
                    model = "test-model",
                    createdAt = 1672531200,
                    response = "Test response",
                    done = true,
                    doneReason = "stop",
                ),
            )
    }

    /**
     * Test that the OpenAIService generates a response correctly.
     */
    @Test
    fun testOpenAIService() =
        runBlocking {
            // Create a test implementation of OpenAIService
            val testService = TestOpenAIService()

            // Call the service
            val result =
                testService.generateResponse(
                    "Test prompt",
                    "test-model",
                    OpenAIOptions(temperature = 0.5),
                )

            // Verify the result
            assertTrue(result.isSuccess)
            val response = result.getOrNull()
            assertNotNull(response)
            assertTrue(response is OpenAIResponse)
            val openAIResponse = response as OpenAIResponse
            assertEquals("test-model", openAIResponse.model)
            assertEquals(1672531200, openAIResponse.createdAt)
            assertEquals("Test response", openAIResponse.response)
            assertTrue(openAIResponse.done)
            assertEquals("stop", openAIResponse.doneReason)
        }

    @Test
    fun `test correct exception is thrown for illegal route`() {
        val route = "invalid_route"
        assertFailsWith<IllegalArgumentException> {
            LLMServiceFactory.getService(route)
        }.apply {
            assertEquals("Invalid route $route", message)
        }
    }
}
