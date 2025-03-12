package prototype

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import prototype.helpers.PromptException

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
}
