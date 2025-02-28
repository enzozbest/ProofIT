import kcl.seg.rtt.prototype.OllamaResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class OllamaResponseTest {
    @Test
    fun `Test OllamaResponse Class`() {
        val response = OllamaResponse("model", "created_at", "response", false, "done_reason")
        assertEquals("model", response.model)
        assertEquals("created_at", response.createdAt)
        assertEquals("response", response.response)
        assertFalse(response.done)
        assertEquals("done_reason", response.doneReason)
    }

    @Test
    fun `Test OllamaResponse serialises to JSON`() {
        val response = OllamaResponse("model", "created_at", "response", false, "done_reason")
        val json = Json.encodeToString(response)
        val expected =
            """{"model":"model","created_at":"created_at","response":"response","done":false,"done_reason":"done_reason"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `Test OllamaResponse de-serialises from JSON`() {
        val expected = OllamaResponse("model", "created_at", "response", false, "done_reason")
        val response =
            """
            {
            "model":"model",
            "created_at":"created_at",
            "response":"response",
            "done":false,
            "done_reason":"done_reason"
            }
            """.trimIndent()
        val json = Json.decodeFromString<OllamaResponse>(response)
        assertEquals(expected, json)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test AuthenticatedSession deserialization missing a field`() {
        val json = """{"model":"model", "created_at":"created_at", "response":"response", "done":false}"""
        assertFailsWith<MissingFieldException> {
            Json.decodeFromString<OllamaResponse>(json)
        }
    }
}
