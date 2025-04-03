package prototype.helpers

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LLMResponsesTest {
    @BeforeAll
    fun setup() {
        // This ensures the class is loaded and initialized
        println("[DEBUG_LOG] Setting up LLMResponsesTest")
    }

    // Test for OllamaResponse data class
    @Test
    fun `Test OllamaResponse Class`() {
        val response = OllamaResponse("model", "created_at", "response", false, "done_reason")
        assertEquals("model", response.model)
        assertEquals("created_at", response.created_at)
        assertEquals("response", response.response)
        assertFalse(response.done)
        assertEquals("done_reason", response.done_reason)
    }

    // Test for OpenAIResponse data class
    @Test
    fun `Test OpenAIResponse Class`() {
        val response = OpenAIResponse("model", 1672531200, "response", true, "stop")
        assertEquals("model", response.model)
        assertEquals(1672531200, response.created_at)
        assertEquals("response", response.response)
        assertTrue(response.done)
        assertEquals("stop", response.done_reason)
    }

    // Test for OpenAIResponse with default values
    @Test
    fun `Test OpenAIResponse with default values`() {
        val response = OpenAIResponse("model", 1672531200, "response")
        assertEquals("model", response.model)
        assertEquals(1672531200, response.created_at)
        assertEquals("response", response.response)
        assertTrue(response.done) // Default value is true
        assertEquals("stop", response.done_reason) // Default value is "stop"
    }

    // Test for parseOpenAIResponse function with complete response
    @Test
    fun `Test parseOpenAIResponse with complete response`() {
        val jsonString =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": [
                    {
                        "type": "text",
                        "id": "output-id",
                        "status": "completed",
                        "role": "assistant",
                        "content": [
                            {
                                "type": "text",
                                "text": "This is a test response"
                            }
                        ]
                    }
                ]
            }
            """.trimIndent()

        val response = parseOpenAIResponse(jsonString)
        assertEquals("test-model", response.model)
        assertEquals(1672531200, response.created_at)
        assertEquals("This is a test response", response.response)
        assertTrue(response.done)
        assertEquals("stop", response.done_reason)
    }

    // Test for parseOpenAIResponse function with empty output
    @Test
    fun `Test parseOpenAIResponse with empty output`() {
        val jsonString =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": []
            }
            """.trimIndent()

        val response = parseOpenAIResponse(jsonString)
        assertEquals("test-model", response.model)
        assertEquals(1672531200, response.created_at)
        assertEquals("", response.response)
        assertTrue(response.done)
        assertEquals("stop", response.done_reason)
    }

    // Test for parseOpenAIResponse function with null response
    @Test
    fun `Test parseOpenAIResponse with null content`() {
        val jsonString =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": [
                    {
                        "type": "text",
                        "id": "output-id",
                        "status": "completed",
                        "role": "assistant",
                        "content": []
                    }
                ]
            }
            """.trimIndent()

        val response = parseOpenAIResponse(jsonString)
        assertEquals("test-model", response.model)
        assertEquals(1672531200, response.created_at)
        assertEquals("", response.response)
        assertTrue(response.done)
        assertEquals("stop", response.done_reason)
    }

    // Test for parseOpenAIResponse function with complex response including all fields
    @Test
    fun `Test parseOpenAIResponse with complex response`() {
        val jsonString =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "error": null,
                "incomplete_details": null,
                "instructions": "test instructions",
                "max_output_tokens": 100,
                "model": "test-model",
                "output": [
                    {
                        "type": "text",
                        "id": "output-id",
                        "status": "completed",
                        "role": "assistant",
                        "content": [
                            {
                                "type": "text",
                                "text": "This is a test response",
                                "annotations": []
                            }
                        ]
                    }
                ],
                "parallel_tool_calls": false,
                "previous_response_id": null,
                "reasoning": {
                    "effort": "high",
                    "generate_summary": "yes"
                },
                "store": true,
                "temperature": 0.7,
                "text": {
                    "format": {
                        "type": "markdown"
                    }
                },
                "tool_choice": null,
                "tools": [],
                "top_p": 0.9,
                "truncation": null,
                "usage": {
                    "input_tokens": 10,
                    "input_tokens_details": {
                        "cached_tokens": 5
                    },
                    "output_tokens": 20,
                    "output_tokens_details": {
                        "reasoning_tokens": 15
                    },
                    "total_tokens": 30
                },
                "user": "test-user",
                "metadata": {
                    "test": "value"
                }
            }
            """.trimIndent()

        val response = parseOpenAIResponse(jsonString)
        assertEquals("test-model", response.model)
        assertEquals(1672531200, response.created_at)
        assertEquals("This is a test response", response.response)
        assertTrue(response.done)
        assertEquals("stop", response.done_reason)
    }

    // Test for parseOpenAIResponse with minimal valid response
    @Test
    fun `Test parseOpenAIResponse with minimal valid response`() {
        val jsonString =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model"
            }
            """.trimIndent()

        val response = parseOpenAIResponse(jsonString)
        assertEquals("test-model", response.model)
        assertEquals(1672531200, response.created_at)
        assertEquals("", response.response)
        assertTrue(response.done)
        assertEquals("stop", response.done_reason)
    }

    // Test for parseOpenAIResponse with output but no content
    @Test
    fun `Test parseOpenAIResponse with output but no content`() {
        val jsonString =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": [
                    {
                        "type": "text",
                        "id": "output-id",
                        "status": "completed",
                        "role": "assistant"
                    }
                ]
            }
            """.trimIndent()

        val response = parseOpenAIResponse(jsonString)
        assertEquals("test-model", response.model)
        assertEquals(1672531200, response.created_at)
        assertEquals("", response.response)
        assertTrue(response.done)
        assertEquals("stop", response.done_reason)
    }

    // Test for parseOpenAIResponse with output containing content with no items
    @Test
    fun `Test parseOpenAIResponse with output containing content with no items`() {
        val jsonString =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": [
                    {
                        "type": "text",
                        "id": "output-id",
                        "status": "completed",
                        "role": "assistant",
                        "content": []
                    }
                ]
            }
            """.trimIndent()

        val response = parseOpenAIResponse(jsonString)
        assertEquals("test-model", response.model)
        assertEquals(1672531200, response.created_at)
        assertEquals("", response.response)
        assertTrue(response.done)
        assertEquals("stop", response.done_reason)
    }

    // Test for parseOpenAIResponse with output and content but no text
    @Test
    fun `Test parseOpenAIResponse with output and content but no text`() {
        val jsonString =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": [
                    {
                        "type": "text",
                        "id": "output-id",
                        "status": "completed",
                        "role": "assistant",
                        "content": [
                            {
                                "type": "text",
                                "text": ""
                            }
                        ]
                    }
                ]
            }
            """.trimIndent()

        val response = parseOpenAIResponse(jsonString)
        assertEquals("test-model", response.model)
        assertEquals(1672531200, response.created_at)
        assertEquals("", response.response)
        assertTrue(response.done)
        assertEquals("stop", response.done_reason)
    }

    // Test for parseOpenAIResponse with null output field
    @Test
    fun `Test parseOpenAIResponse with null output field`() {
        val jsonString =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": null
            }
            """.trimIndent()

        val response = parseOpenAIResponse(jsonString)
        assertEquals("test-model", response.model)
        assertEquals(1672531200, response.created_at)
        assertEquals("", response.response)
        assertTrue(response.done)
        assertEquals("stop", response.done_reason)
    }

    // Test for parseOpenAIResponse with output item that has null content
    @Test
    fun `Test parseOpenAIResponse with output item that has null content`() {
        val jsonString =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": [
                    {
                        "type": "text",
                        "id": "output-id",
                        "status": "completed",
                        "role": "assistant",
                        "content": null
                    }
                ]
            }
            """.trimIndent()

        val response = parseOpenAIResponse(jsonString)
        assertEquals("test-model", response.model)
        assertEquals(1672531200, response.created_at)
        assertEquals("", response.response)
        assertTrue(response.done)
        assertEquals("stop", response.done_reason)
    }

    // Test for parseOpenAIResponse with a complex structure that exercises all branches
    @Test
    fun `Test parseOpenAIResponse with complex structure that exercises all branches`() {
        // First test with output as an empty list
        val jsonString1 =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": []
            }
            """.trimIndent()

        val response1 = parseOpenAIResponse(jsonString1)
        assertEquals("test-model", response1.model)
        assertEquals(1672531200, response1.created_at)
        assertEquals("", response1.response)

        // Then test with output containing an item with empty content
        val jsonString2 =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": [
                    {
                        "type": "text",
                        "id": "output-id",
                        "status": "completed",
                        "role": "assistant",
                        "content": []
                    }
                ]
            }
            """.trimIndent()

        val response2 = parseOpenAIResponse(jsonString2)
        assertEquals("test-model", response2.model)
        assertEquals(1672531200, response2.created_at)
        assertEquals("", response2.response)

        // Finally test with null output
        val jsonString3 =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": null
            }
            """.trimIndent()

        val response3 = parseOpenAIResponse(jsonString3)
        assertEquals("test-model", response3.model)
        assertEquals(1672531200, response3.created_at)
        assertEquals("", response3.response)
    }

    // Test specifically targeting the branches we're missing
    @Test
    fun `Test parseOpenAIResponse targeting specific branches`() {
        // Test for line 136 branch - this.output is null
        println("[DEBUG_LOG] Testing with null output")
        val jsonString1 =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model"
            }
            """.trimIndent()

        val response1 = parseOpenAIResponse(jsonString1)
        assertEquals("test-model", response1.model)
        assertEquals("", response1.response)

        // Test for line 139 branch - content is null or empty
        println("[DEBUG_LOG] Testing with null content")
        val jsonString2 =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": [
                    {
                        "type": "text",
                        "id": "output-id",
                        "status": "completed",
                        "role": "assistant"
                    }
                ]
            }
            """.trimIndent()

        val response2 = parseOpenAIResponse(jsonString2)
        assertEquals("test-model", response2.model)
        assertEquals("", response2.response)

        // Test with a completely different structure
        println("[DEBUG_LOG] Testing with completely different structure")
        val jsonString3 =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": [
                    {
                        "type": "text",
                        "id": "output-id",
                        "status": "completed",
                        "role": "assistant",
                        "content": [
                            {
                                "type": "text",
                                "text": "This is a test response"
                            }
                        ]
                    }
                ]
            }
            """.trimIndent()

        val response3 = parseOpenAIResponse(jsonString3)
        assertEquals("test-model", response3.model)
        assertEquals("This is a test response", response3.response)
    }

    // Test for OllamaResponse serialization
    @Test
    fun `Test OllamaResponse serialization`() {
        val response = OllamaResponse("model", "created_at", "response", false, "done_reason")
        val json = Json.encodeToString(response)

        // Verify the JSON contains all the expected fields with correct values
        assertTrue(json.contains("\"model\":\"model\""))
        assertTrue(json.contains("\"created_at\":\"created_at\""))
        assertTrue(json.contains("\"response\":\"response\""))
        assertTrue(json.contains("\"done\":false"))
        assertTrue(json.contains("\"done_reason\":\"done_reason\""))

        // Deserialize back to object and verify equality
        val deserializedResponse = Json.decodeFromString<OllamaResponse>(json)
        assertEquals(response, deserializedResponse)
    }

    // Test for OllamaResponse deserialization
    @Test
    fun `Test OllamaResponse deserialization`() {
        val json =
            """
            {
                "model": "test-model",
                "created_at": "2023-01-01T12:00:00Z",
                "response": "This is a test response",
                "done": true,
                "done_reason": "stop"
            }
            """.trimIndent()

        val response = Json.decodeFromString<OllamaResponse>(json)
        assertEquals("test-model", response.model)
        assertEquals("2023-01-01T12:00:00Z", response.created_at)
        assertEquals("This is a test response", response.response)
        assertTrue(response.done)
        assertEquals("stop", response.done_reason)
    }

    // Test for OpenAIResponse serialization
    @Test
    fun `Test OpenAIResponse serialization`() {
        val response = OpenAIResponse("model", 1672531200, "response", true, "stop")
        val json = Json.encodeToString(response)
        println("[DEBUG_LOG] OpenAIResponse JSON: $json")

        // Verify the JSON contains all the expected fields with correct values
        assertTrue(json.contains("\"model\":\"model\""))
        assertTrue(json.contains("\"created_at\":1672531200"))
        assertTrue(json.contains("\"response\":\"response\""))

        // Default values might be omitted in the serialized JSON
        // So we don't check for them directly

        // Deserialize back to object and verify equality
        val deserializedResponse = Json.decodeFromString<OpenAIResponse>(json)
        assertEquals(response, deserializedResponse)
        assertEquals("model", deserializedResponse.model)
        assertEquals(1672531200, deserializedResponse.created_at)
        assertEquals("response", deserializedResponse.response)
        assertTrue(deserializedResponse.done)
        assertEquals("stop", deserializedResponse.done_reason)
    }

    // Test for OpenAIResponse deserialization
    @Test
    fun `Test OpenAIResponse deserialization`() {
        val json =
            """
            {
                "model": "test-model",
                "created_at": 1672531200,
                "response": "This is a test response",
                "done": true,
                "done_reason": "stop"
            }
            """.trimIndent()

        val response = Json.decodeFromString<OpenAIResponse>(json)
        assertEquals("test-model", response.model)
        assertEquals(1672531200, response.created_at)
        assertEquals("This is a test response", response.response)
        assertTrue(response.done)
        assertEquals("stop", response.done_reason)
    }

    // Test for OpenAIResponse deserialization with default values
    @Test
    fun `Test OpenAIResponse deserialization with default values`() {
        val json =
            """
            {
                "model": "test-model",
                "created_at": 1672531200,
                "response": "This is a test response"
            }
            """.trimIndent()

        val response = Json.decodeFromString<OpenAIResponse>(json)
        assertEquals("test-model", response.model)
        assertEquals(1672531200, response.created_at)
        assertEquals("This is a test response", response.response)
        assertTrue(response.done) // Default value is true
        assertEquals("stop", response.done_reason) // Default value is "stop"
    }

    // Test for OutputItem serialization and deserialization
    @Test
    fun `Test OutputItem serialization and deserialization`() {
        val contentItems =
            listOf(
                ContentItem("text", "This is a test content", emptyList()),
            )
        val outputItem = OutputItem("text", "output-id", "completed", "assistant", contentItems)

        // Test serialization
        val json = Json.encodeToString(outputItem)
        assertTrue(json.contains("\"type\":\"text\""))
        assertTrue(json.contains("\"id\":\"output-id\""))
        assertTrue(json.contains("\"status\":\"completed\""))
        assertTrue(json.contains("\"role\":\"assistant\""))
        assertTrue(json.contains("\"content\":["))

        // Test deserialization
        val deserializedItem = Json.decodeFromString<OutputItem>(json)
        assertEquals(outputItem, deserializedItem)
        assertEquals("text", deserializedItem.type)
        assertEquals("output-id", deserializedItem.id)
        assertEquals("completed", deserializedItem.status)
        assertEquals("assistant", deserializedItem.role)
        assertEquals(1, deserializedItem.content.size)
        assertEquals("text", deserializedItem.content[0].type)
        assertEquals("This is a test content", deserializedItem.content[0].text)
    }

    // Test for ContentItem serialization and deserialization
    @Test
    fun `Test ContentItem serialization and deserialization`() {
        val contentItem = ContentItem("text", "This is a test content", emptyList())

        // Test serialization
        val json = Json.encodeToString(contentItem)
        println("[DEBUG_LOG] ContentItem JSON: $json")
        assertTrue(json.contains("\"type\":\"text\""))
        assertTrue(json.contains("\"text\":\"This is a test content\""))
        // The annotations field might be omitted if it's empty

        // Test deserialization
        val deserializedItem = Json.decodeFromString<ContentItem>(json)
        assertEquals(contentItem, deserializedItem)
        assertEquals("text", deserializedItem.type)
        assertEquals("This is a test content", deserializedItem.text)
        assertTrue(deserializedItem.annotations.isEmpty())
    }

    // Test for Reasoning serialization and deserialization
    @Test
    fun `Test Reasoning serialization and deserialization`() {
        val reasoning = Reasoning("high", "yes")

        // Test serialization
        val json = Json.encodeToString(reasoning)
        assertTrue(json.contains("\"effort\":\"high\""))
        assertTrue(json.contains("\"generate_summary\":\"yes\""))

        // Test deserialization
        val deserializedReasoning = Json.decodeFromString<Reasoning>(json)
        assertEquals(reasoning, deserializedReasoning)
        assertEquals("high", deserializedReasoning.effort)
        assertEquals("yes", deserializedReasoning.generateSummary)
    }

    // Test for Reasoning with null values
    @Test
    fun `Test Reasoning with null values`() {
        val reasoning = Reasoning(null, null)

        // Test serialization
        val json = Json.encodeToString(reasoning)
        assertTrue(json.contains("{}") || json.contains("\"effort\":null") || json.contains("\"generate_summary\":null"))

        // Test deserialization
        val deserializedReasoning = Json.decodeFromString<Reasoning>(json)
        assertEquals(reasoning, deserializedReasoning)
        assertNull(deserializedReasoning.effort)
        assertNull(deserializedReasoning.generateSummary)
    }

    // Test for TextFormat serialization and deserialization
    @Test
    fun `Test TextFormat serialization and deserialization`() {
        val format = Format("markdown")
        val textFormat = TextFormat(format)

        // Test serialization
        val json = Json.encodeToString(textFormat)
        assertTrue(json.contains("\"format\":{"))
        assertTrue(json.contains("\"type\":\"markdown\""))

        // Test deserialization
        val deserializedFormat = Json.decodeFromString<TextFormat>(json)
        assertEquals(textFormat, deserializedFormat)
        assertNotNull(deserializedFormat.format)
        assertEquals("markdown", deserializedFormat.format?.type)
    }

    // Test for TextFormat with null format
    @Test
    fun `Test TextFormat with null format`() {
        val textFormat = TextFormat(null)

        // Test serialization
        val json = Json.encodeToString(textFormat)
        assertTrue(json.contains("{}") || json.contains("\"format\":null"))

        // Test deserialization
        val deserializedFormat = Json.decodeFromString<TextFormat>(json)
        assertEquals(textFormat, deserializedFormat)
        assertNull(deserializedFormat.format)
    }

    // Test for Format serialization and deserialization
    @Test
    fun `Test Format serialization and deserialization`() {
        val format = Format("markdown")

        // Test serialization
        val json = Json.encodeToString(format)
        assertTrue(json.contains("\"type\":\"markdown\""))

        // Test deserialization
        val deserializedFormat = Json.decodeFromString<Format>(json)
        assertEquals(format, deserializedFormat)
        assertEquals("markdown", deserializedFormat.type)
    }

    // Test for Format with null type
    @Test
    fun `Test Format with null type`() {
        val format = Format(null)

        // Test serialization
        val json = Json.encodeToString(format)
        assertTrue(json.contains("{}") || json.contains("\"type\":null"))

        // Test deserialization
        val deserializedFormat = Json.decodeFromString<Format>(json)
        assertEquals(format, deserializedFormat)
        assertNull(deserializedFormat.type)
    }

    // Test for Usage serialization and deserialization
    @Test
    fun `Test Usage serialization and deserialization`() {
        val inputTokensDetails = InputTokensDetails(5)
        val outputTokensDetails = OutputTokensDetails(15)
        val usage = Usage(10, inputTokensDetails, 20, outputTokensDetails, 30)

        // Test serialization
        val json = Json.encodeToString(usage)
        assertTrue(json.contains("\"input_tokens\":10"))
        assertTrue(json.contains("\"input_tokens_details\":{"))
        assertTrue(json.contains("\"cached_tokens\":5"))
        assertTrue(json.contains("\"output_tokens\":20"))
        assertTrue(json.contains("\"output_tokens_details\":{"))
        assertTrue(json.contains("\"reasoning_tokens\":15"))
        assertTrue(json.contains("\"total_tokens\":30"))

        // Test deserialization
        val deserializedUsage = Json.decodeFromString<Usage>(json)
        assertEquals(usage, deserializedUsage)
        assertEquals(10, deserializedUsage.inputTokens)
        assertEquals(5, deserializedUsage.inputTokensDetails?.cachedTokens)
        assertEquals(20, deserializedUsage.outputTokens)
        assertEquals(15, deserializedUsage.outputTokensDetails?.reasoningTokens)
        assertEquals(30, deserializedUsage.totalTokens)
    }

    // Test for Usage with null details
    @Test
    fun `Test Usage with null details`() {
        val usage = Usage(10, null, 20, null, 30)

        // Test serialization
        val json = Json.encodeToString(usage)
        assertTrue(json.contains("\"input_tokens\":10"))
        assertTrue(json.contains("\"input_tokens_details\":null") || !json.contains("\"input_tokens_details\""))
        assertTrue(json.contains("\"output_tokens\":20"))
        assertTrue(json.contains("\"output_tokens_details\":null") || !json.contains("\"output_tokens_details\""))
        assertTrue(json.contains("\"total_tokens\":30"))

        // Test deserialization
        val deserializedUsage = Json.decodeFromString<Usage>(json)
        assertEquals(usage, deserializedUsage)
        assertEquals(10, deserializedUsage.inputTokens)
        assertNull(deserializedUsage.inputTokensDetails)
        assertEquals(20, deserializedUsage.outputTokens)
        assertNull(deserializedUsage.outputTokensDetails)
        assertEquals(30, deserializedUsage.totalTokens)
    }

    // Test for InputTokensDetails serialization and deserialization
    @Test
    fun `Test InputTokensDetails serialization and deserialization`() {
        val inputTokensDetails = InputTokensDetails(5)

        // Test serialization
        val json = Json.encodeToString(inputTokensDetails)
        assertTrue(json.contains("\"cached_tokens\":5"))

        // Test deserialization
        val deserializedDetails = Json.decodeFromString<InputTokensDetails>(json)
        assertEquals(inputTokensDetails, deserializedDetails)
        assertEquals(5, deserializedDetails.cachedTokens)
    }

    // Test for InputTokensDetails with null values
    @Test
    fun `Test InputTokensDetails with null values`() {
        val inputTokensDetails = InputTokensDetails(null)

        // Test serialization
        val json = Json.encodeToString(inputTokensDetails)
        assertTrue(json.contains("{}") || json.contains("\"cached_tokens\":null"))

        // Test deserialization
        val deserializedDetails = Json.decodeFromString<InputTokensDetails>(json)
        assertEquals(inputTokensDetails, deserializedDetails)
        assertNull(deserializedDetails.cachedTokens)
    }

    // Test for OutputTokensDetails serialization and deserialization
    @Test
    fun `Test OutputTokensDetails serialization and deserialization`() {
        val outputTokensDetails = OutputTokensDetails(15)

        // Test serialization
        val json = Json.encodeToString(outputTokensDetails)
        assertTrue(json.contains("\"reasoning_tokens\":15"))

        // Test deserialization
        val deserializedDetails = Json.decodeFromString<OutputTokensDetails>(json)
        assertEquals(outputTokensDetails, deserializedDetails)
        assertEquals(15, deserializedDetails.reasoningTokens)
    }

    // Test for OutputTokensDetails with null values
    @Test
    fun `Test OutputTokensDetails with null values`() {
        val outputTokensDetails = OutputTokensDetails(null)

        // Test serialization
        val json = Json.encodeToString(outputTokensDetails)
        assertTrue(json.contains("{}") || json.contains("\"reasoning_tokens\":null"))

        // Test deserialization
        val deserializedDetails = Json.decodeFromString<OutputTokensDetails>(json)
        assertEquals(outputTokensDetails, deserializedDetails)
        assertNull(deserializedDetails.reasoningTokens)
    }

    // Test for parseOpenAIResponse with null output
    @Test
    fun `Test parseOpenAIResponse with null output field and branch coverage`() {
        // Test with null output field
        val jsonString =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": null
            }
            """.trimIndent()

        val response = parseOpenAIResponse(jsonString)
        assertEquals("test-model", response.model)
        assertEquals(1672531200, response.created_at)
        assertEquals("", response.response)

        // Test with output field but null content
        val jsonString2 =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": [
                    {
                        "type": "text",
                        "id": "output-id",
                        "status": "completed",
                        "role": "assistant",
                        "content": null
                    }
                ]
            }
            """.trimIndent()

        val response2 = parseOpenAIResponse(jsonString2)
        assertEquals("test-model", response2.model)
        assertEquals(1672531200, response2.created_at)
        assertEquals("", response2.response)
    }

    // Test for constructor coverage of all data classes
    @Test
    fun `Test constructor coverage of all data classes`() {
        // Test OutputItem constructor
        val outputItem = OutputItem("type", "id", "status", "role")
        assertEquals("type", outputItem.type)
        assertEquals("id", outputItem.id)
        assertEquals("status", outputItem.status)
        assertEquals("role", outputItem.role)
        assertTrue(outputItem.content.isEmpty())

        // Test ContentItem constructor
        val contentItem = ContentItem("type", "text")
        assertEquals("type", contentItem.type)
        assertEquals("text", contentItem.text)
        assertTrue(contentItem.annotations.isEmpty())

        // Test Reasoning constructor
        val reasoning = Reasoning()
        assertNull(reasoning.effort)
        assertNull(reasoning.generateSummary)

        // Test TextFormat constructor
        val textFormat = TextFormat()
        assertNull(textFormat.format)

        // Test Format constructor
        val format = Format()
        assertNull(format.type)

        // Test Usage constructor with all parameters
        val inputTokensDetails = InputTokensDetails(5)
        val outputTokensDetails = OutputTokensDetails(15)
        val usage = Usage(10, inputTokensDetails, 20, outputTokensDetails, 30)
        assertEquals(10, usage.inputTokens)
        assertEquals(5, usage.inputTokensDetails?.cachedTokens)
        assertEquals(20, usage.outputTokens)
        assertEquals(15, usage.outputTokensDetails?.reasoningTokens)
        assertEquals(30, usage.totalTokens)

        // Test Usage constructor with null details
        val usageWithNulls = Usage(10, null, 20, null, 30)
        assertEquals(10, usageWithNulls.inputTokens)
        assertNull(usageWithNulls.inputTokensDetails)
        assertEquals(20, usageWithNulls.outputTokens)
        assertNull(usageWithNulls.outputTokensDetails)
        assertEquals(30, usageWithNulls.totalTokens)

        // Test InputTokensDetails constructor
        val inputTokensDetailsEmpty = InputTokensDetails()
        assertNull(inputTokensDetailsEmpty.cachedTokens)

        // Test OutputTokensDetails constructor
        val outputTokensDetailsEmpty = OutputTokensDetails()
        assertNull(outputTokensDetailsEmpty.reasoningTokens)
    }

    // Test for branch coverage in toOpenAIResponse
    @Test
    fun `Test branch coverage in toOpenAIResponse`() {
        // Test with empty output array
        val jsonString1 =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": []
            }
            """.trimIndent()

        val response1 = parseOpenAIResponse(jsonString1)
        assertEquals("test-model", response1.model)
        assertEquals(1672531200, response1.created_at)
        assertEquals("", response1.response)

        // Test with output array containing an item with empty content array
        val jsonString2 =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": [
                    {
                        "type": "text",
                        "id": "output-id",
                        "status": "completed",
                        "role": "assistant",
                        "content": []
                    }
                ]
            }
            """.trimIndent()

        val response2 = parseOpenAIResponse(jsonString2)
        assertEquals("test-model", response2.model)
        assertEquals(1672531200, response2.created_at)
        assertEquals("", response2.response)

        // Test with output array containing an item with content array containing an item with empty text
        val jsonString3 =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": [
                    {
                        "type": "text",
                        "id": "output-id",
                        "status": "completed",
                        "role": "assistant",
                        "content": [
                            {
                                "type": "text",
                                "text": ""
                            }
                        ]
                    }
                ]
            }
            """.trimIndent()

        val response3 = parseOpenAIResponse(jsonString3)
        assertEquals("test-model", response3.model)
        assertEquals(1672531200, response3.created_at)
        assertEquals("", response3.response)

        // Test with null output field to cover the branch in toOpenAIResponse
        val jsonString4 =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": null
            }
            """.trimIndent()

        val response4 = parseOpenAIResponse(jsonString4)
        assertEquals("test-model", response4.model)
        assertEquals(1672531200, response4.created_at)
        assertEquals("", response4.response)
    }

    // Test for Usage constructor coverage
    @Test
    fun `Test Usage constructor coverage`() {
        // Create a Usage object with all parameters
        val inputTokensDetails = InputTokensDetails(5)
        val outputTokensDetails = OutputTokensDetails(15)
        val usage = Usage(10, inputTokensDetails, 20, outputTokensDetails, 30)

        // Serialize to JSON
        val json = Json.encodeToString(usage)

        // Deserialize back to object
        val deserializedUsage = Json.decodeFromString<Usage>(json)

        // Verify all properties
        assertEquals(10, deserializedUsage.inputTokens)
        assertEquals(5, deserializedUsage.inputTokensDetails?.cachedTokens)
        assertEquals(20, deserializedUsage.outputTokens)
        assertEquals(15, deserializedUsage.outputTokensDetails?.reasoningTokens)
        assertEquals(30, deserializedUsage.totalTokens)

        // Test with a direct JSON string for Usage
        val usageJson =
            """
            {
                "input_tokens": 10,
                "input_tokens_details": {
                    "cached_tokens": 5
                },
                "output_tokens": 20,
                "output_tokens_details": {
                    "reasoning_tokens": 15
                },
                "total_tokens": 30
            }
            """.trimIndent()

        // Parse the JSON directly
        val parsedUsage = Json.decodeFromString<Usage>(usageJson)

        // Verify all properties
        assertEquals(10, parsedUsage.inputTokens)
        assertEquals(5, parsedUsage.inputTokensDetails?.cachedTokens)
        assertEquals(20, parsedUsage.outputTokens)
        assertEquals(15, parsedUsage.outputTokensDetails?.reasoningTokens)
        assertEquals(30, parsedUsage.totalTokens)
    }

    @Test
    fun `Test Usage with default constructor values`() {
        val test = Usage(inputTokens = 10, outputTokens = 10, totalTokens = 10)

        assertEquals(10, test.inputTokens)
        assertNull(test.inputTokensDetails)
        assertEquals(10, test.outputTokens)
        assertEquals(10, test.totalTokens)
        assertNull(test.outputTokensDetails)
    }

    @Test
    fun `Test OpenAIApiResponse with default constructor values`() {
        val test =
            OpenAIApiResponse(
                id = "id",
                `object` = "object",
                createdAt = 1672531200,
                status = "completed",
                model = "model",
            )

        assertEquals("id", test.id)
        assertEquals("object", test.`object`)
        assertEquals(1672531200, test.createdAt)
        assertEquals("completed", test.status)
        assertNull(test.error)
        assertNull(test.incompleteDetails)
        assertNull(test.instructions)
        assertNull(test.maxOutputTokens)
        assertEquals("model", test.model)
        assertEquals(emptyList(), test.output)
        assertFalse(test.parallelToolCalls)
        assertNull(test.previousResponseId)
        assertNull(test.reasoning)
        assertTrue(test.store)
        assertNull(test.temperature)
        assertNull(test.text)
        assertNull(test.toolChoice)
        assertEquals(emptyList(), test.tools)
        assertNull(test.topP)
        assertNull(test.truncation)
        assertNull(test.usage)
        assertNull(test.user)
        assertNull(test.metadata)
    }

    @Test
    fun `Test parseOpenAIResponse with non-text content type`() {
        val jsonString =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": [
                    {
                        "type": "text",
                        "id": "output-id",
                        "status": "completed",
                        "role": "assistant",
                        "content": [
                            {
                                "type": "image",
                                "text": ""
                            }
                        ]
                    }
                ]
            }
            """.trimIndent()

        val response = parseOpenAIResponse(jsonString)
        assertEquals("test-model", response.model)
        assertEquals(1672531200, response.created_at)
        assertEquals("", response.response)
        assertTrue(response.done)
        assertEquals("stop", response.done_reason)
    }

    @Test
    fun `Test parseOpenAIResponse with multiple content items`() {
        val jsonString =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": [
                    {
                        "type": "text",
                        "id": "output-id",
                        "status": "completed",
                        "role": "assistant",
                        "content": [
                            {
                                "type": "text",
                                "text": "First item"
                            },
                            {
                                "type": "text",
                                "text": "Second item"
                            }
                        ]
                    }
                ]
            }
            """.trimIndent()

        val response = parseOpenAIResponse(jsonString)
        assertEquals("test-model", response.model)
        assertEquals(1672531200, response.created_at)
        assertEquals("First item", response.response)
        assertTrue(response.done)
        assertEquals("stop", response.done_reason)
    }

    @Test
    fun `Test parseOpenAIResponse with output containing non-empty content but no items`() {
        val jsonString =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": [
                    {
                        "type": "text",
                        "id": "output-id",
                        "status": "completed",
                        "role": "assistant",
                        "content": []
                    }
                ]
            }
            """.trimIndent()

        val response = parseOpenAIResponse(jsonString)
        assertEquals("test-model", response.model)
        assertEquals(1672531200, response.created_at)
        assertEquals("", response.response)
        assertTrue(response.done)
        assertEquals("stop", response.done_reason)
    }

    @Test
    fun `Test parseOpenAIResponse with output array containing multiple items`() {
        val jsonString =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": [
                    {
                        "type": "text",
                        "id": "output-id-1",
                        "status": "completed",
                        "role": "assistant",
                        "content": [
                            {
                                "type": "text",
                                "text": "First output item"
                            }
                        ]
                    },
                    {
                        "type": "text",
                        "id": "output-id-2",
                        "status": "completed",
                        "role": "assistant",
                        "content": [
                            {
                                "type": "text",
                                "text": "Second output item"
                            }
                        ]
                    }
                ]
            }
            """.trimIndent()

        val response = parseOpenAIResponse(jsonString)
        assertEquals("test-model", response.model)
        assertEquals(1672531200, response.created_at)
        assertEquals("First output item", response.response)
        assertTrue(response.done)
        assertEquals("stop", response.done_reason)
    }

    @Test
    fun `Test parseOpenAIResponse with output item having content with different structure`() {
        // Create a test with a different structure for the content
        // This might help cover the remaining branches
        val jsonString =
            """
            {
                "id": "test-id",
                "object": "test-object",
                "created_at": 1672531200,
                "status": "completed",
                "model": "test-model",
                "output": [
                    {
                        "type": "text",
                        "id": "output-id",
                        "status": "completed",
                        "role": "assistant",
                        "content": [
                            {
                                "type": "other",
                                "text": "Some text"
                            }
                        ]
                    }
                ]
            }
            """.trimIndent()

        val response = parseOpenAIResponse(jsonString)
        assertEquals("test-model", response.model)
        assertEquals(1672531200, response.created_at)
        assertEquals("Some text", response.response)
        assertTrue(response.done)
        assertEquals("stop", response.done_reason)
    }

    @Test
    fun `Test OpenAIApiResponse with non-default constructor values`() {
        val inputTokensDetails = InputTokensDetails(5)
        val outputTokensDetails = OutputTokensDetails(15)
        val usage = Usage(10, inputTokensDetails, 20, outputTokensDetails, 30)
        val outputItem = OutputItem("type", "id", "status", "role")
        val reasoning = Reasoning("high", "yes")
        val textFormat = TextFormat(Format("markdown"))
        val metadata = buildJsonObject { put("key", JsonPrimitive("value")) }

        val test =
            OpenAIApiResponse(
                id = "id",
                `object` = "object",
                createdAt = 1672531200,
                status = "completed",
                error = "error",
                incompleteDetails = "incompleteDetails",
                instructions = "instructions",
                maxOutputTokens = 100,
                model = "model",
                output = listOf(outputItem),
                parallelToolCalls = true,
                previousResponseId = "previousResponseId",
                reasoning = reasoning,
                store = false,
                temperature = 0.7,
                text = textFormat,
                toolChoice = "toolChoice",
                tools = listOf(JsonPrimitive("tool1"), JsonPrimitive("tool2")),
                topP = 0.9,
                truncation = "truncation",
                usage = usage,
                user = "user",
                metadata = metadata,
            )

        // Verify all properties
        assertEquals("id", test.id)
        assertEquals("object", test.`object`)
        assertEquals(1672531200, test.createdAt)
        assertEquals("completed", test.status)
        assertEquals("error", test.error)
        assertEquals("incompleteDetails", test.incompleteDetails)
        assertEquals("instructions", test.instructions)
        assertEquals(100, test.maxOutputTokens)
        assertEquals("model", test.model)
        assertEquals(1, test.output.size)
        assertEquals(outputItem, test.output[0])
        assertTrue(test.parallelToolCalls)
        assertEquals("previousResponseId", test.previousResponseId)
        assertEquals(reasoning, test.reasoning)
        assertFalse(test.store)
        assertEquals(0.7, test.temperature)
        assertEquals(textFormat, test.text)
        assertEquals("toolChoice", test.toolChoice)
        assertEquals(2, test.tools.size)
        assertEquals(JsonPrimitive("tool1"), test.tools[0])
        assertEquals(JsonPrimitive("tool2"), test.tools[1])
        assertEquals(0.9, test.topP)
        assertEquals("truncation", test.truncation)
        assertEquals(usage, test.usage)
        assertEquals("user", test.user)
        assertEquals(metadata, test.metadata)

        // Verify nested objects
        assertEquals("type", test.output[0].type)
        assertEquals("id", test.output[0].id)
        assertEquals("status", test.output[0].status)
        assertEquals("role", test.output[0].role)

        assertEquals("high", test.reasoning?.effort)
        assertEquals("yes", test.reasoning?.generateSummary)

        assertEquals("markdown", test.text?.format?.type)

        assertEquals(10, test.usage?.inputTokens)
        assertEquals(5, test.usage?.inputTokensDetails?.cachedTokens)
        assertEquals(20, test.usage?.outputTokens)
        assertEquals(15, test.usage?.outputTokensDetails?.reasoningTokens)
        assertEquals(30, test.usage?.totalTokens)
    }
}
