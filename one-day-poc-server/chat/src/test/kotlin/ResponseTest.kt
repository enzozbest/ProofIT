package kcl.seg.rtt.chat

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

@kotlinx.serialization.Serializable
data class Response(
    val message: String,
    val time: String
)

class ResponseTest {
    @Test
    fun `Test Response creation with valid parameters`() {
        val response = Response(
            message = "Test message",
            time = "2025-01-01T12:00:00"
        )

        assertEquals("Test message", response.message)
        assertEquals("2025-01-01T12:00:00", response.time)
    }

    @Test
    fun `Test Response serialization`() {
        val response = Response(
            message = "Test message",
            time = "2025-01-01T12:00:00"
        )

        val jsonString = Json.encodeToString(Response.serializer(), response)
        val deserializedResponse = Json.decodeFromString(Response.serializer(), jsonString)

        assertEquals(response, deserializedResponse)
    }

    @Test
    fun `Test Response deserialization fails with missing required fields`() {
        val invalidJson = """{"message":"Test message"}"""

        assertThrows<SerializationException> {
            Json.decodeFromString<Response>(invalidJson)
        }
    }

    @Test
    fun `Test Response deserialization fails with invalid JSON`() {
        val invalidJson = """{"message":123,"time":"2025-01-01T12:00:00"}"""

        assertThrows<SerializationException> {
            Json.decodeFromString<Response>(invalidJson)
        }
    }
}