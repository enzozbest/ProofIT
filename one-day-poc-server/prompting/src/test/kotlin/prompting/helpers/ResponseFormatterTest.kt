package prompting.helpers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import prototype.helpers.OllamaResponse
import prototype.helpers.OpenAIResponse

/**
 * Tests for the ResponseFormatter interface and its implementations.
 */
class ResponseFormatterTest {
    /**
     * Test that the ResponseFormatterFactory returns the correct formatter for a given route.
     */
    @Test
    fun testResponseFormatterFactory() {
        val localFormatter = ResponseFormatterFactory.getFormatter("local")
        val openAIFormatter = ResponseFormatterFactory.getFormatter("openai")
        
        assert(localFormatter is OllamaResponseFormatter)
        assert(openAIFormatter is OpenAIResponseFormatter)
        
        assertThrows<IllegalArgumentException> {
            ResponseFormatterFactory.getFormatter("invalid")
        }
    }
    
    /**
     * Test that the OllamaResponseFormatter formats a response correctly.
     */
    @Test
    fun testOllamaResponseFormatter() {
        val formatter = OllamaResponseFormatter()
        
        val response = OllamaResponse(
            model = "test-model",
            created_at = "2023-01-01T00:00:00Z",
            response = """{"key": "value"}""",
            done = true,
            done_reason = "stop"
        )
        
        val formattedResponse = formatter.format(response)
        
        // The formatter should return the response as-is
        assertEquals("""{"key": "value"}""", formattedResponse)
    }
    
    /**
     * Test that the OllamaResponseFormatter throws an exception for invalid response types.
     */
    @Test
    fun testOllamaResponseFormatterWithInvalidType() {
        val formatter = OllamaResponseFormatter()
        
        val response = OpenAIResponse(
            model = "test-model",
            created_at = 1672531200,
            response = """{"key": "value"}""",
            done = true,
            done_reason = "stop"
        )
        
        assertThrows<IllegalArgumentException> {
            formatter.format(response)
        }
    }
    
    /**
     * Test that the OpenAIResponseFormatter formats a response correctly.
     */
    @Test
    fun testOpenAIResponseFormatter() {
        val formatter = OpenAIResponseFormatter()
        
        val response = OpenAIResponse(
            model = "test-model",
            created_at = 1672531200,
            response = """{"key": "value"}""",
            done = true,
            done_reason = "stop"
        )
        
        val formattedResponse = formatter.format(response)
        
        // The formatter should return the response as-is
        assertEquals("""{"key": "value"}""", formattedResponse)
    }
    
    /**
     * Test that the OpenAIResponseFormatter throws an exception for invalid response types.
     */
    @Test
    fun testOpenAIResponseFormatterWithInvalidType() {
        val formatter = OpenAIResponseFormatter()
        
        val response = OllamaResponse(
            model = "test-model",
            created_at = "2023-01-01T00:00:00Z",
            response = """{"key": "value"}""",
            done = true,
            done_reason = "stop"
        )
        
        assertThrows<IllegalArgumentException> {
            formatter.format(response)
        }
    }
}