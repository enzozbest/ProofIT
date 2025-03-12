package chat

import chat.Request
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class RequestTest {
    @Test
    fun `Test Request creation with valid parameters`() {
        val request =
            Request(
                userID = "testUser",
                time = "2025-01-01T12:00:00",
                prompt = "Hello",
            )

        assertEquals("testUser", request.userID)
        assertEquals("2025-01-01T12:00:00", request.time)
        assertEquals("Hello", request.prompt)
    }

    @Test
    fun `Test Request serialization`() {
        val request =
            Request(
                userID = "testUser",
                time = "2025-01-01T12:00:00",
                prompt = "Hello",
            )

        val jsonString = Json.encodeToString(Request.serializer(), request)
        val deserializedRequest = Json.decodeFromString(Request.serializer(), jsonString)

        assertEquals(request, deserializedRequest)
    }

    @Test
    fun `Test Request deserialization fails with missing required fields`() {
        val invalidJson = """{"userID":"testUser","time":"2025-01-01T12:00:00"}"""

        assertThrows<SerializationException> {
            Json.decodeFromString<Request>(invalidJson)
        }
    }

    @Test
    fun `Test Request deserialization fails with invalid JSON`() {
        val invalidJson = """{"userID":123,"time":"2025-01-01T12:00:00","prompt":"Hello"}"""

        assertThrows<SerializationException> {
            Json.decodeFromString<Request>(invalidJson)
        }
    }
}
