package kcl.seg.rtt.chat_history

import kotlin.test.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class RequestTest {
    @Test
    fun `test Request creation with valid data`() {
        val request = Request(
            userID = "user123",
            time = "2025-01-01T12:00:00",
            prompt = "Hello"
        )

        assertEquals("user123", request.userID)
        assertEquals("2025-01-01T12:00:00", request.time)
        assertEquals("Hello", request.prompt)
    }

    @Test
    fun `test Request serialization to JSON`() {
        val request = Request(
            userID = "user123",
            time = "2025-01-01T12:00:00",
            prompt = "Hello"
        )

        val json = Json.encodeToString(request)
        val expectedJson = """{"userID":"user123","time":"2025-01-01T12:00:00","prompt":"Hello"}"""

        assertEquals(expectedJson, json)
    }

    @Test
    fun `test Request deserialization from JSON`() {
        val jsonString = """{"userID":"user124","time":"2025-01-01T12:00:00","prompt":"Hello"}"""
        val request = Json.decodeFromString<Request>(jsonString)

        assertEquals("user124", request.userID)
        assertEquals("2025-01-01T12:00:00", request.time)
        assertEquals("Hello", request.prompt)
    }

    @Test
    fun `test Request data class equality`() {
        val request1 = Request("user123", "2025-01-01T12:00:00", "Hello")
        val request2 = Request("user123", "2025-01-01T12:00:00", "Hello")
        val request3 = Request("user124", "2025-01-01T12:00:00", "Hello")

        assertEquals(request1, request2)
        assertNotEquals(request1, request3)
    }

    @Test
    fun `test Request copy function`() {
        val original = Request("user123", "2025-01-01T12:00:00", "Hello")
        val copied = original.copy(userID = "user124")

        assertEquals("user124", copied.userID)
        assertEquals(original.time, copied.time)
        assertEquals(original.prompt, copied.prompt)
    }
}