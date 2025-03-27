package chat.routes

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JsonProcessorTest {

    @Test
    fun `Test processRawJsonResponse with valid JSON containing chat and prototype`() {
        val jsonString = """
            {
                "chat": "This is a test chat message",
                "prototype": {
                    "files": {
                        "file1.txt": "content1",
                        "file2.txt": "content2"
                    }
                }
            }
        """.trimIndent()

        val result = JsonProcessor.processRawJsonResponse(jsonString)

        assertEquals("This is a test chat message", result.first)
        assertNotNull(result.second)
        assert(result.second.contains("file1.txt"))
        assert(result.second.contains("content1"))
    }

    @Test
    fun `Test processRawJsonResponse with valid JSON containing only chat`() {
        val jsonString = """
            {
                "chat": "This is a test chat message"
            }
        """.trimIndent()

        val result = JsonProcessor.processRawJsonResponse(jsonString)

        assertEquals("This is a test chat message", result.first)
        assertEquals("{}", result.second)
    }

    @Test
    fun `Test processRawJsonResponse with valid JSON containing only prototype`() {
        val jsonString = """
            {
                "prototype": {
                    "files": {
                        "file1.txt": "content1"
                    }
                }
            }
        """.trimIndent()

        val result = JsonProcessor.processRawJsonResponse(jsonString)

        assertEquals("", result.first)
        assertNotNull(result.second)
        assert(result.second.contains("file1.txt"))
    }

    @Test
    fun `Test processRawJsonResponse with empty JSON`() {
        val jsonString = "{}"

        val result = JsonProcessor.processRawJsonResponse(jsonString)

        assertEquals("", result.first)
        assertEquals("{}", result.second)
    }

    @Test
    fun `Test processRawJsonResponse with invalid JSON`() {
        val jsonString = "This is not valid JSON"

        val result = JsonProcessor.processRawJsonResponse(jsonString)

        assertEquals("", result.first)
        assertEquals("{}", result.second)
    }

    @Test
    fun `Test processRawJsonResponse with chat in message format`() {
        val jsonString = """
            {
                "message": "This is a test chat message"
            }
        """.trimIndent()

        val result = JsonProcessor.processRawJsonResponse(jsonString)

        assertEquals("This is a test chat message", result.first)
        assertEquals("{}", result.second)
    }

    @Test
    fun `Test processRawJsonResponse with nested chat object`() {
        val jsonString = """
            {
                "chat": {
                    "message": "This is a test chat message"
                },
                "prototype": {
                    "files": {
                        "file1.txt": "content1"
                    }
                }
            }
        """.trimIndent()

        val result = JsonProcessor.processRawJsonResponse(jsonString)

        assertEquals("This is a test chat message", result.first)
        assertNotNull(result.second)
        assert(result.second.contains("file1.txt"))
    }

    @Test
    fun `Test extractPrototypeContent with complex nested structure`() {
        val jsonString = """
            {
                "chat": "Test message",
                "prototype": {
                    "files": {
                        "file1.txt": "content1",
                        "nested": {
                            "file2.txt": "content2"
                        }
                    }
                }
            }
        """.trimIndent()

        val result = JsonProcessor.processRawJsonResponse(jsonString)

        assertEquals("Test message", result.first)
        assertNotNull(result.second)
        assert(result.second.contains("file1.txt"))
        assert(result.second.contains("nested"))
    }

    @Test
    fun `Test extractPrototypeContent with malformed files section`() {
        val jsonString = """
            {
                "chat": "Test message",
                "prototype": {
                    "files": "Not an object but a string"
                }
            }
        """.trimIndent()

        val result = JsonProcessor.processRawJsonResponse(jsonString)

        assertEquals("Test message", result.first)
        assertEquals("{}", result.second)
    }

    @Test
    fun `Test extractPrototypeContent with unbalanced braces`() {
        val jsonString = """
            {
                "chat": "Test message",
                "prototype": {
                    "files": {
                        "file1.txt": "content1",
                        "unbalanced": {
                            "missing": "closing brace"
                    }
                }
            }
        """.trimIndent()

        val result = JsonProcessor.processRawJsonResponse(jsonString)

        assertEquals("Test message", result.first)
        // The JsonProcessor is able to handle unbalanced braces and extract the content
        assertTrue(result.second.contains("file1.txt"))
        assertTrue(result.second.contains("content1"))
        assertTrue(result.second.contains("unbalanced"))
        assertTrue(result.second.contains("missing"))
        assertTrue(result.second.contains("closing brace"))
    }

    @Test
    fun `Test extractPrototypeContent with nested opening braces`() {
        val jsonString = """
            {
                "chat": "Test message",
                "prototype": {
                    "files": {
                        "file1.txt": "content1",
                        "nested": {
                            "deeper": {
                                "evenDeeper": "value"
                            }
                        }
                    }
                }
            }
        """.trimIndent()

        val result = JsonProcessor.processRawJsonResponse(jsonString)

        assertEquals("Test message", result.first)
        assertTrue(result.second.contains("file1.txt"))
        assertTrue(result.second.contains("nested"))
        assertTrue(result.second.contains("deeper"))
        assertTrue(result.second.contains("evenDeeper"))
    }

    @Test
    fun `Test extractPrototypeContent with no files section`() {
        val jsonString = """
            {
                "chat": "Test message",
                "prototype": {
                    "noFiles": "This doesn't have a files section"
                }
            }
        """.trimIndent()

        val result = JsonProcessor.processRawJsonResponse(jsonString)

        assertEquals("Test message", result.first)
        assertEquals("{}", result.second)
    }

    @Test
    fun `Test extractPrototypeContent with files section but no opening brace`() {
        val jsonString = """
            {
                "chat": "Test message",
                "prototype": {
                    "files": "Not an object"
                }
            }
        """.trimIndent()

        val result = JsonProcessor.processRawJsonResponse(jsonString)

        assertEquals("Test message", result.first)
        assertEquals("{}", result.second)
    }

    @Test
    fun `Test extractPrototypeContent with incomplete JSON object`() {
        val jsonString = """
            {
                "chat": "Test message",
                "prototype": {
                    "files": {
                        "incomplete": true
                    }
                }

        """.trimIndent()

        val result = JsonProcessor.processRawJsonResponse(jsonString)

        assertEquals("Test message", result.first)
        // Even with incomplete JSON, the processor should extract what it can
        assertTrue(result.second.contains("incomplete"))
    }
}
