package prompting.helpers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import prototype.helpers.OllamaResponse
import prototype.helpers.OpenAIResponse
import kotlin.test.assertFailsWith

/**
 * Tests for the ResponseFormatter interface and its implementations.
 */
class ResponseFormatterTest {
    /**
     * Test that the ResponseFormatterFactory returns the correct formatter for a given route.
     */
    @Test
    fun `Test ResponseFormatterFactory returns correct values`() {
        val localFormatter = ResponseFormatterFactory.getFormatter("local")
        val openAIFormatter = ResponseFormatterFactory.getFormatter("openai")

        assert(localFormatter is OllamaResponseFormatter)
        assert(openAIFormatter is OpenAIResponseFormatter)

        val message =
            assertThrows<IllegalArgumentException> {
                ResponseFormatterFactory.getFormatter("invalid")
            }.message

        assertEquals("Invalid route invalid", message)
    }

    @Test
    fun `Test OllamaResponseFormatter with non-null response`() {
        val formatter = OllamaResponseFormatter()

        val response =
            OllamaResponse(
                model = "test-model",
                created_at = "2023-01-01T00:00:00Z",
                response = """{"key": "value"}""",
                done = true,
                done_reason = "stop",
            )

        val formattedResponse = formatter.format(response)
        assertEquals("""{"key": "value"}""", formattedResponse)
    }

    @Test
    fun `Test OllamaResponseFormatter with null response`() {
        val formatter = OllamaResponseFormatter()

        val response: OllamaResponse =
            OllamaResponse(
                model = "test-model",
                created_at = "2023-01-01T00:00:00Z",
                response = null,
                done = true,
                done_reason = "stop",
            )
        val message = assertFailsWith<RuntimeException> { formatter.format(response) }.message
        assertEquals("LLM response was null!", message)
    }

    @Test
    fun `Test OllamaResponseFormatter with invalid type`() {
        val formatter = OllamaResponseFormatter()
        val response =
            OpenAIResponse(
                model = "test-model",
                createdAt = 1672531200,
                response = """{"key": "value"}""",
                done = true,
                doneReason = "stop",
            )
        assertThrows<IllegalArgumentException> {
            formatter.format(response)
        }
    }

    @Test
    fun `Test OpenAIResponseFormatter with non-null response`() {
        val formatter = OpenAIResponseFormatter()

        val response =
            OpenAIResponse(
                model = "test-model",
                createdAt = 1672531200,
                response = """{"key": "value"}""",
                done = true,
                doneReason = "stop",
            )

        val formattedResponse = formatter.format(response)

        assertEquals("""{"key": "value"}""", formattedResponse)
    }

    @Test
    fun `Test OpenAIResponseFormatter with null response`() {
        val formatter = OpenAIResponseFormatter()

        val response =
            OpenAIResponse(
                model = "test-model",
                createdAt = 1672531200,
                response = null,
                done = true,
                doneReason = "stop",
            )

        val message = assertFailsWith<RuntimeException> { formatter.format(response) }.message
        assertEquals("LLM response was null!", message)
    }

    @Test
    fun `Test OpenAIResponseFormatter with invalid type`() {
        val formatter = OpenAIResponseFormatter()

        val response =
            OllamaResponse(
                model = "test-model",
                created_at = "2023-01-01T00:00:00Z",
                response = """{"key": "value"}""",
                done = true,
                done_reason = "stop",
            )

        assertThrows<IllegalArgumentException> {
            formatter.format(response)
        }
    }
}
