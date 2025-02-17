import kcl.seg.rtt.prototype.FileContent
import kcl.seg.rtt.prototype.LlmResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LlmResponseTest {
    @Test
    fun `Test LlmResponse Class`() {
        val response = LlmResponse("mainFile", mapOf("file" to FileContent("content")))
        assertEquals("mainFile", response.mainFile)
        assertEquals("content", response.files["file"]?.content)
    }

    @Test
    fun `Test LlmResponse serialises to JSON`() {
        val response = LlmResponse("mainFile", mapOf("file" to FileContent("content")))
        val json = Json.encodeToString(response)
        val expected = """{"mainFile":"mainFile","files":{"file":{"content":"content"}}}"""
        assertEquals(expected, json)
    }

    @Test
    fun `Test LlmResponse de-serialises from JSON`() {
        val expected = LlmResponse("mainFile", mapOf("file" to FileContent("content")))
        val response =
            """
            {
            "mainFile":"mainFile",
            "files":{
                "file":{
                    "content":"content"
                }
            }
            }
            """.trimIndent()
        val json = Json.decodeFromString<LlmResponse>(response)
        assertEquals(expected, json)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test AuthenticatedSession deserialization missing a field`() {
        val json = """{"mainFile":"mainFile"}"""
        assertFailsWith<MissingFieldException> {
            Json.decodeFromString<LlmResponse>(json)
        }
    }
}
