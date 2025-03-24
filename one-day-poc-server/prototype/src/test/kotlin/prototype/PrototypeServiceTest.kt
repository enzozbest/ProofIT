package prototype

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import prototype.helpers.PromptException
import java.util.AbstractMap

class PrototypeServiceTest {
    @Test
    fun `Test PrototypeService parses valid json with code field successfully`() {
        val json = buildJsonObject {
            put("mainFile", JsonPrimitive("index.js"))
            put("files", buildJsonObject {
                put("javascript", buildJsonObject {
                    put("code", JsonPrimitive("console.log('Hello World');"))
                })
            })
        }

        val response = convertJsonToLlmResponse(json)

        assertEquals("index.js", response.mainFile)
        assertEquals("console.log('Hello World');", response.files["javascript"]?.content)
    }

    @Test
    fun `Test PrototypeService parses valid json with content field successfully`() {
        val json = buildJsonObject {
            put("mainFile", JsonPrimitive("index.js"))
            put("files", buildJsonObject {
                put("javascript", buildJsonObject {
                    put("content", JsonPrimitive("console.log('Hello World');"))
                })
            })
        }

        val response = convertJsonToLlmResponse(json)

        assertEquals("index.js", response.mainFile)
        assertEquals("console.log('Hello World');", response.files["javascript"]?.content)
    }

    @Test
    fun `Test PrototypeService parses valid json with primitive content successfully`() {
        val json = buildJsonObject {
            put("mainFile", JsonPrimitive("index.js"))
            put("files", buildJsonObject {
                put("javascript", JsonPrimitive("console.log('Hello World');"))
            })
        }

        val response = convertJsonToLlmResponse(json)

        assertEquals("index.js", response.mainFile)
        assertEquals("console.log('Hello World');", response.files["javascript"]?.content)
    }

    @Test
    fun `Test PrototypeService throws exception when files field is missing`() {
        val json = buildJsonObject {
            put("mainFile", JsonPrimitive("index.js"))
        }

        val exception = assertThrows(PromptException::class.java) {
            convertJsonToLlmResponse(json)
        }
        assertEquals("Missing 'files' field in LLM response", exception.message)
    }

    @Test
    fun `Test PrototypeService throws exception when code and content fields are missing`() {
        val json = buildJsonObject {
            put("mainFile", JsonPrimitive("index.js"))
            put("files", buildJsonObject {
                put("javascript", buildJsonObject {})
            })
        }

        val exception = assertThrows(PromptException::class.java) {
            convertJsonToLlmResponse(json)
        }
        assertEquals("Missing 'code' or 'content' field in file for language: javascript", exception.message)
    }

    @Test
    fun `Test PrototypeService throws exception when file content is an unexpected format`() {
        val json = buildJsonObject {
            put("mainFile", JsonPrimitive("index.js"))
            put("files", buildJsonObject {
                put("javascript", buildJsonArray {
                    add(JsonPrimitive("unexpected"))
                })
            })
        }

        val exception = assertThrows(PromptException::class.java) {
            convertJsonToLlmResponse(json)
        }
        assertEquals("Unexpected format for file content in language: javascript", exception.message)
    }

    @Test
    fun `Test PrototypeService defaults mainFile to html if missing`() {
        val json = buildJsonObject {
            put("files", buildJsonObject {
                put("javascript", JsonPrimitive("console.log('Hello World');"))
            })
        }

        val response = convertJsonToLlmResponse(json)

        assertEquals("html", response.mainFile)
        assertEquals("console.log('Hello World');", response.files["javascript"]?.content)
    }

    @Test
    fun `Test PrototypeService handles unexpected exception during processing`() {
        // Create a JSON structure that will cause a ClassCastException during processing
        // We'll use a JsonArray for files, which will cause a ClassCastException when accessed as a JsonObject
        val json = buildJsonObject {
            put("files", buildJsonArray {})
        }

        val exception = assertThrows(PromptException::class.java) {
            convertJsonToLlmResponse(json)
        }

        println("[DEBUG_LOG] Exception message: ${exception.message}")

        // The exception should be a PromptException with the expected message
        assertEquals("Missing 'files' field in LLM response", exception.message)
    }

    @Test
    fun `Test PrototypeService rethrows non-PromptException as PromptException`() {
        // Use MockK to create a JsonObject that throws a RuntimeException when accessed
        val mockJson = mockk<JsonObject>()

        // Make the mock throw a RuntimeException when get("files") is called
        every { mockJson["files"] } throws RuntimeException("Test exception")

        val exception = assertThrows(PromptException::class.java) {
            convertJsonToLlmResponse(mockJson)
        }

        println("[DEBUG_LOG] Exception message: ${exception.message}")

        // Check that the exception is a rethrown non-PromptException
        assertEquals("Failed to parse LLM response: Test exception", exception.message)
    }

    @Test
    fun `Test PrototypeService handles unexpected exception in forEach`() {
        // Create a JSON structure that will cause a ClassCastException during the forEach loop
        val json = buildJsonObject {
            put("files", buildJsonObject {
                put("javascript", buildJsonArray {})  // This will cause a ClassCastException in the when statement
            })
        }

        val exception = assertThrows(PromptException::class.java) {
            convertJsonToLlmResponse(json)
        }

        println("[DEBUG_LOG] Exception message: ${exception.message}")

        // The exception should be a PromptException with the expected message
        assertEquals("Unexpected format for file content in language: javascript", exception.message)
    }

    @Test
    fun `Test PrototypeService defaults mainFile when not a JsonPrimitive`() {
        // Test the branch where mainFile is not a JsonPrimitive
        val json = buildJsonObject {
            put("mainFile", buildJsonObject {})  // Use a JsonObject instead of JsonPrimitive
            put("files", buildJsonObject {
                put("javascript", JsonPrimitive("console.log('Hello World');"))
            })
        }

        val response = convertJsonToLlmResponse(json)

        // Should default to "html"
        assertEquals("html", response.mainFile)
    }

    @Test
    fun `Test PrototypeService handles null mainFile field`() {
        // Test the branch where mainFile field is null
        val mockJsonObject = mockk<JsonObject>()
        val mockFiles = mockk<JsonObject>()

        // Setup the mock to return an empty set of entries for files
        every { mockFiles.entries } returns emptySet()

        // Setup the mock to return null for the "mainFile" field
        every { mockJsonObject["files"] } returns mockFiles
        every { mockJsonObject["mainFile"] } returns null

        val response = convertJsonToLlmResponse(mockJsonObject)

        // Should default to "html"
        assertEquals("html", response.mainFile)
    }

    @Test
    fun `Test PrototypeService falls back to content field when code is not a JsonPrimitive`() {
        // Test the branch where code field is not a JsonPrimitive
        val json = buildJsonObject {
            put("files", buildJsonObject {
                put("javascript", buildJsonObject {
                    put("code", buildJsonObject {})  // Use a JsonObject instead of JsonPrimitive
                    put("content", JsonPrimitive("console.log('Hello World');"))
                })
            })
        }

        val response = convertJsonToLlmResponse(json)

        // Should use content field as fallback
        assertEquals("console.log('Hello World');", response.files["javascript"]?.content)
    }

    @Test
    fun `Test PrototypeService handles null code field`() {
        // Test the branch where code field is null
        val mockJsonObject = mockk<JsonObject>()
        val mockFileContent = mockk<JsonObject>()

        // Setup the mock to return null for the "code" field
        every { mockFileContent["code"] } returns null
        every { mockFileContent["content"] } returns JsonPrimitive("console.log('Hello World');")
        every { mockFileContent.entries } returns emptySet()
        every { mockFileContent.containsKey(any()) } returns false

        val mockFiles = mockk<JsonObject>()
        every { mockFiles.entries } returns setOf(AbstractMap.SimpleEntry("javascript", mockFileContent))

        every { mockJsonObject["files"] } returns mockFiles
        every { mockJsonObject["mainFile"] } returns JsonPrimitive("index.js")

        val response = convertJsonToLlmResponse(mockJsonObject)

        // Should use content field as fallback
        assertEquals("console.log('Hello World');", response.files["javascript"]?.content)
    }
}
