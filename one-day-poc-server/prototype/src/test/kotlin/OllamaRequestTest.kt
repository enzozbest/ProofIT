import kcl.seg.rtt.prototype.OllamaRequest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class OllamaRequestTest {
    @Test
    fun `Test OllamaRequest Class`() {
        val request = OllamaRequest("model", "prompt", false)
        assertEquals("model", request.model)
        assertEquals("prompt", request.prompt)
        assertFalse(request.stream)
    }

    @Test
    fun `Test OllamaRequest serialises to JSON`() {
        val request = OllamaRequest("model", "prompt", false)
        val json = Json.encodeToString<OllamaRequest>(request)
        val expected =
            """
            {"model":"model","prompt":"prompt","stream":false}
            """.trimIndent()
        assertEquals(expected, json)
    }

    @Test
    fun `Test OllamaRequest de-serialises from JSON`() {
        val expected = OllamaRequest("model", "prompt", false)
        val request =
            """
            {
            "model":"model",
            "prompt":"prompt",
            "stream":false
            }
            """.trimIndent()
        val json = Json.decodeFromString<OllamaRequest>(request)
        assertEquals(expected, json)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test OllamaRequest deserialization missing a field`() {
        val json = """{"model":"model", "prompt":"prompt"}"""
        assertFailsWith<MissingFieldException> {
            Json.decodeFromString<OllamaRequest>(json)
        }
    }
}
