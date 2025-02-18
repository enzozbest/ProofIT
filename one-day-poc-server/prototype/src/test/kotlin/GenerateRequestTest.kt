import kcl.seg.rtt.prototype.GenerateRequest
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GenerateRequestTest {
    @Test
    fun `Test GenerateRequest Class`() {
        val request = GenerateRequest("prompt")
        assertEquals("prompt", request.prompt)
    }

    @Test
    fun `Test GenerateRequest serialises to JSON`() {
        val request = GenerateRequest("prompt")
        val json = Json.encodeToString(request)
        val expected = """{"prompt":"prompt"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `Test GenerateRequest de-serialises from JSON`() {
        val expected = GenerateRequest("prompt")
        val request =
            """
            {
            "prompt":"prompt"
            }
            """.trimIndent()
        val json = Json.decodeFromString<GenerateRequest>(request)
        assertEquals(expected, json)
    }


    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test GenerateRequest deserialization missing a field`() {
        val json = """{}"""
        assertFailsWith<MissingFieldException> {
            Json.decodeFromString<GenerateRequest>(json)
        }
    }
}
