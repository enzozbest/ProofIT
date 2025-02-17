import kcl.seg.rtt.prototype.ErrorResponse
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ErrorResponseTest {
    @Test
    fun `Test ErrorResponse Class`() {
        val response = ErrorResponse("error")
        assertEquals("error", response.error)
    }

    @Test
    fun `Test ErrorResponse serialises to JSON`() {
        val response = ErrorResponse("error")
        val json = Json.encodeToString(response)
        val expected = """{"error":"error"}"""
        assertEquals(expected, json)
    }

    @Test
    fun `Test ErrorResponse de-serialises from JSON`() {
        val expected = ErrorResponse("error")
        val response =
            """
            {
            "error":"error"
            }
            """.trimIndent()
        val json = Json.decodeFromString<ErrorResponse>(response)
        assertEquals(expected, json)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `Test ErrorResponse deserialization missing a field`() {
        val json = """{}"""
        assertFailsWith<MissingFieldException> {
            Json.decodeFromString<ErrorResponse>(json)
        }
    }
}
