package prototype.helpers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotEquals

class EnhancedResponseTest {
    @Test
    fun `Test EnhancedResponse Class`() {
        val ollamaResponse = OllamaResponse("model", "created_at", "response content", false, "stop")
        val templates = listOf("<Button>Click me</Button>", "<Input type=\"text\" />")
        val enhancedResponse = EnhancedResponse(ollamaResponse, templates)

        assertEquals(ollamaResponse, enhancedResponse.response)
        assertEquals(templates, enhancedResponse.extractedTemplates)
    }

    @Test
    fun `Test EnhancedResponse with null OllamaResponse`() {
        val templates = listOf("<Button>Click me</Button>")
        val enhancedResponse = EnhancedResponse(null, templates)

        assertNull(enhancedResponse.response)
        assertEquals(templates, enhancedResponse.extractedTemplates)
    }

    @Test
    fun `Test EnhancedResponse with empty templates list`() {
        val ollamaResponse = OllamaResponse("model", "created_at", "response", true, "stop")
        val enhancedResponse = EnhancedResponse(ollamaResponse)

        assertEquals(ollamaResponse, enhancedResponse.response)
        assertTrue(enhancedResponse.extractedTemplates.isEmpty())
    }

    @Test
    fun `Test EnhancedResponse serialises to JSON`() {
        val ollamaResponse = OllamaResponse("mistral", "2023-01-01", "test response", true, "stop")
        val templates = listOf("<Card><h1>Title</h1></Card>", "<Footer>Copyright 2023</Footer>")
        val enhancedResponse = EnhancedResponse(ollamaResponse, templates)

        val json = Json.encodeToString(enhancedResponse)

        assertTrue(json.contains("\"response\":{"))
        assertTrue(json.contains("\"model\":\"mistral\""))
        assertTrue(json.contains("\"extractedTemplates\":["))
        assertTrue(json.contains("<Card><h1>Title</h1></Card>"))
        assertTrue(json.contains("<Footer>Copyright 2023</Footer>"))
    }

    @Test
    fun `Test EnhancedResponse de-serialises from JSON`() {
        val expectedOllamaResponse = OllamaResponse("llama2", "2023-03-15T12:00:00Z", "generated code", true, "complete")
        val expectedTemplates = listOf("<Component1 />", "<Component2 prop=\"value\" />")
        val expected = EnhancedResponse(expectedOllamaResponse, expectedTemplates)

        val jsonString = """
            {
                "response":{
                    "model":"llama2",
                    "created_at":"2023-03-15T12:00:00Z",
                    "response":"generated code",
                    "done":true,
                    "done_reason":"complete"
                },
                "extractedTemplates":["<Component1 />", "<Component2 prop=\"value\" />"]
            }
        """.trimIndent()

        val actual = Json.decodeFromString<EnhancedResponse>(jsonString)

        assertEquals(expected, actual)
        assertEquals(expectedOllamaResponse, actual.response)
        assertEquals(expectedTemplates, actual.extractedTemplates)
    }

    @Test
    fun `Test EnhancedResponse with null response de-serialises from JSON`() {
        val expectedTemplates = listOf("<Component1 />", "<Component2 />")
        val expected = EnhancedResponse(null, expectedTemplates)

        val jsonString = """
            {
                "response":null,
                "extractedTemplates":["<Component1 />", "<Component2 />"]
            }
        """.trimIndent()

        val actual = Json.decodeFromString<EnhancedResponse>(jsonString)

        assertEquals(expected, actual)
        assertNull(actual.response)
        assertEquals(expectedTemplates, actual.extractedTemplates)
    }

    @Test
    fun `Test EnhancedResponse with empty templates de-serialises from JSON`() {
        val expectedOllamaResponse = OllamaResponse("gpt4", "2023-03-15", "response content", true, "complete")
        val expected = EnhancedResponse(expectedOllamaResponse, emptyList())

        val jsonString = """
            {
                "response":{
                    "model":"gpt4",
                    "created_at":"2023-03-15",
                    "response":"response content",
                    "done":true,
                    "done_reason":"complete"
                },
                "extractedTemplates":[]
            }
        """.trimIndent()

        val actual = Json.decodeFromString<EnhancedResponse>(jsonString)

        assertEquals(expected, actual)
        assertEquals(expectedOllamaResponse, actual.response)
        assertTrue(actual.extractedTemplates.isEmpty())
    }

    @Test
    fun `Test equality and hashCode for EnhancedResponse`() {
        val ollamaResponse1 = OllamaResponse("model1", "created_at", "response", false, "stop")
        val ollamaResponse2 = OllamaResponse("model2", "created_at", "response", false, "stop")

        val templates1 = listOf("<Template1 />", "<Template2 />")
        val templates2 = listOf("<Template3 />", "<Template4 />")

        val response1 = EnhancedResponse(ollamaResponse1, templates1)
        val response2 = EnhancedResponse(ollamaResponse1, templates1) // Same as response1
        val response3 = EnhancedResponse(ollamaResponse2, templates1) // Different OllamaResponse
        val response4 = EnhancedResponse(ollamaResponse1, templates2) // Different templates

        // Test equality
        assertEquals(response1, response2)
        assertNotEquals(response1, response3)
        assertNotEquals(response1, response4)

        // Test hashCode
        assertEquals(response1.hashCode(), response2.hashCode())
    }

    @Test
    fun `Test EnhancedResponse copy function`() {
        val ollamaResponse = OllamaResponse("model", "created_at", "response", true, "stop")
        val templates = listOf("<Template1 />", "<Template2 />")
        val original = EnhancedResponse(ollamaResponse, templates)

        val newOllamaResponse = OllamaResponse("new-model", "created_at", "response", true, "stop")
        val newTemplates = listOf("<NewTemplate />")

        val copiedWithNewResponse = original.copy(response = newOllamaResponse)
        val copiedWithNewTemplates = original.copy(extractedTemplates = newTemplates)
        val copiedWithBothNew = original.copy(response = newOllamaResponse, extractedTemplates = newTemplates)

        assertEquals(newOllamaResponse, copiedWithNewResponse.response)
        assertEquals(templates, copiedWithNewResponse.extractedTemplates)

        assertEquals(ollamaResponse, copiedWithNewTemplates.response)
        assertEquals(newTemplates, copiedWithNewTemplates.extractedTemplates)

        assertEquals(newOllamaResponse, copiedWithBothNew.response)
        assertEquals(newTemplates, copiedWithBothNew.extractedTemplates)
    }

    @Test
    fun `Test toString contains all properties`() {
        val ollamaResponse = OllamaResponse("model", "created_at", "response", true, "stop")
        val templates = listOf("<Template1 />", "<Template2 />")
        val enhancedResponse = EnhancedResponse(ollamaResponse, templates)

        val toStringResult = enhancedResponse.toString()

        assertTrue(toStringResult.contains("response=OllamaResponse"))
        assertTrue(toStringResult.contains("model=model"))
        assertTrue(toStringResult.contains("extractedTemplates="))
        assertTrue(toStringResult.contains("<Template1 />"))
        assertTrue(toStringResult.contains("<Template2 />"))
    }
}