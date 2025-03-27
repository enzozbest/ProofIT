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

        assertEquals("Missing 'files' field in LLM response", exception.message)
    }

    @Test
    fun `Test PrototypeService rethrows non-PromptException as PromptException`() {
        val mockJson = mockk<JsonObject>()

        every { mockJson["files"] } throws RuntimeException("Test exception")

        val exception = assertThrows(PromptException::class.java) {
            convertJsonToLlmResponse(mockJson)
        }

        assertEquals("Failed to parse LLM response: Test exception", exception.message)
    }

    @Test
    fun `Test PrototypeService handles unexpected exception in forEach`() {
        // Create a JSON structure that will cause a ClassCastException during the forEach loop
        val json = buildJsonObject {
            put("files", buildJsonObject {
                put("javascript", buildJsonArray {})
            })
        }

        val exception = assertThrows(PromptException::class.java) {
            convertJsonToLlmResponse(json)
        }

        assertEquals("Unexpected format for file content in language: javascript", exception.message)
    }

    @Test
    fun `Test PrototypeService defaults mainFile when not a JsonPrimitive`() {
        val json = buildJsonObject {
            put("mainFile", buildJsonObject {})
            put("files", buildJsonObject {
                put("javascript", JsonPrimitive("console.log('Hello World');"))
            })
        }

        val response = convertJsonToLlmResponse(json)

        assertEquals("html", response.mainFile)
    }

    @Test
    fun `Test PrototypeService handles null mainFile field`() {
        val mockJsonObject = mockk<JsonObject>()
        val mockFiles = mockk<JsonObject>()

        every { mockFiles.entries } returns emptySet()

        every { mockJsonObject["files"] } returns mockFiles
        every { mockJsonObject["mainFile"] } returns null

        val response = convertJsonToLlmResponse(mockJsonObject)

        assertEquals("html", response.mainFile)
    }

    @Test
    fun `Test PrototypeService falls back to content field when code is not a JsonPrimitive`() {
        val json = buildJsonObject {
            put("files", buildJsonObject {
                put("javascript", buildJsonObject {
                    put("code", buildJsonObject {})
                    put("content", JsonPrimitive("console.log('Hello World');"))
                })
            })
        }

        val response = convertJsonToLlmResponse(json)

        assertEquals("console.log('Hello World');", response.files["javascript"]?.content)
    }

    @Test
    fun `Test PrototypeService handles null code field`() {
        val mockJsonObject = mockk<JsonObject>()
        val mockFileContent = mockk<JsonObject>()

        every { mockFileContent["code"] } returns null
        every { mockFileContent["content"] } returns JsonPrimitive("console.log('Hello World');")
        every { mockFileContent.entries } returns emptySet()
        every { mockFileContent.containsKey(any()) } returns false

        val mockFiles = mockk<JsonObject>()
        every { mockFiles.entries } returns setOf(AbstractMap.SimpleEntry("javascript", mockFileContent))

        every { mockJsonObject["files"] } returns mockFiles
        every { mockJsonObject["mainFile"] } returns JsonPrimitive("index.js")

        val response = convertJsonToLlmResponse(mockJsonObject)

        assertEquals("console.log('Hello World');", response.files["javascript"]?.content)
    }
}
