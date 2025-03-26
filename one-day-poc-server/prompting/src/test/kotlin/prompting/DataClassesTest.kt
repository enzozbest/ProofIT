package prompting

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DataClassesTest {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    @Test
    fun `test ChatResponse serialization and deserialization`() {
        val timestamp = Instant.now().toString()
        val original = ChatResponse(
            message = "Test message",
            role = "Test role",
            timestamp = timestamp,
            messageId = "123"
        )

        // Serialize to JSON string
        val jsonString = json.encodeToString(original)

        // Deserialize from JSON string
        val deserialized = json.decodeFromString<ChatResponse>(jsonString)

        // Verify all fields are correctly serialized and deserialized
        assertEquals(original.message, deserialized.message)
        assertEquals(original.role, deserialized.role)
        assertEquals(original.timestamp, deserialized.timestamp)
        assertEquals(original.messageId, deserialized.messageId)
    }

    @Test
    fun `test ChatResponse with default role`() {
        val timestamp = Instant.now().toString()
        val chatResponse = ChatResponse(
            message = "Test message",
            timestamp = timestamp,
            messageId = "123"
        )

        // Verify default role is "LLM"
        assertEquals("LLM", chatResponse.role)

        // Serialize and deserialize to verify default value is preserved
        val jsonString = json.encodeToString(chatResponse)
        val deserialized = json.decodeFromString<ChatResponse>(jsonString)

        assertEquals("LLM", deserialized.role)
    }

    @Test
    fun `test ServerResponse serialization and deserialization with prototype`() {
        val timestamp = Instant.now().toString()
        val chatResponse = ChatResponse(
            message = "Test message",
            role = "Test role",
            timestamp = timestamp,
            messageId = "123"
        )

        // Create a JSON string for files
        val filesJsonString = """
            {
                "file1.js": "console.log('Hello')",
                "file2.css": "body { color: black; }"
            }
        """.trimIndent()

        val prototypeResponse = PrototypeResponse(files = filesJsonString)

        val serverResponse = ServerResponse(
            chat = chatResponse,
            prototype = prototypeResponse
        )

        // Serialize to JSON string
        val jsonString = json.encodeToString(serverResponse)

        // Deserialize from JSON string
        val deserialized = json.decodeFromString<ServerResponse>(jsonString)

        // Verify all fields are correctly serialized and deserialized
        assertEquals(serverResponse.chat.message, deserialized.chat.message)
        assertEquals(serverResponse.chat.role, deserialized.chat.role)
        assertEquals(serverResponse.chat.timestamp, deserialized.chat.timestamp)
        assertEquals(serverResponse.chat.messageId, deserialized.chat.messageId)

        assertNotNull(deserialized.prototype)
        assertTrue(deserialized.prototype!!.files.contains("file1.js"))
        assertTrue(deserialized.prototype!!.files.contains("console.log('Hello')"))
        assertTrue(deserialized.prototype!!.files.contains("file2.css"))
        assertTrue(deserialized.prototype!!.files.contains("body { color: black; }"))
    }

    @Test
    fun `test ServerResponse serialization and deserialization without prototype`() {
        val timestamp = Instant.now().toString()
        val chatResponse = ChatResponse(
            message = "Test message",
            role = "Test role",
            timestamp = timestamp,
            messageId = "123"
        )

        val serverResponse = ServerResponse(
            chat = chatResponse,
            prototype = null
        )

        // Serialize to JSON string
        val jsonString = json.encodeToString(serverResponse)

        // Deserialize from JSON string
        val deserialized = json.decodeFromString<ServerResponse>(jsonString)

        // Verify all fields are correctly serialized and deserialized
        assertEquals(serverResponse.chat.message, deserialized.chat.message)
        assertEquals(serverResponse.chat.role, deserialized.chat.role)
        assertEquals(serverResponse.chat.timestamp, deserialized.chat.timestamp)
        assertEquals(serverResponse.chat.messageId, deserialized.chat.messageId)

        // Verify prototype is null
        assertNull(deserialized.prototype)
    }

    @Test
    fun `test ServerResponse with default prototype value`() {
        val timestamp = Instant.now().toString()
        val chatResponse = ChatResponse(
            message = "Test message",
            role = "Test role",
            timestamp = timestamp,
            messageId = "123"
        )

        // Create ServerResponse using the constructor without specifying the prototype parameter
        val serverResponse = ServerResponse(chat = chatResponse)

        // Verify that the default value for prototype is null
        assertNull(serverResponse.prototype)

        // Serialize to JSON string
        val jsonString = json.encodeToString(serverResponse)

        // Deserialize from JSON string
        val deserialized = json.decodeFromString<ServerResponse>(jsonString)

        // Verify all fields are correctly serialized and deserialized
        assertEquals(serverResponse.chat.message, deserialized.chat.message)
        assertEquals(serverResponse.chat.role, deserialized.chat.role)
        assertEquals(serverResponse.chat.timestamp, deserialized.chat.timestamp)
        assertEquals(serverResponse.chat.messageId, deserialized.chat.messageId)

        // Verify prototype is still null after deserialization
        assertNull(deserialized.prototype)
    }

    @Test
    fun `test PrototypeResponse serialization and deserialization`() {
        // Create a JSON string for files
        val filesJsonString = """
            {
                "file1.js": "console.log('Hello')",
                "file2.css": "body { color: black; }",
                "nested": {
                    "file3.html": "<html></html>"
                }
            }
        """.trimIndent()

        val prototypeResponse = PrototypeResponse(files = filesJsonString)

        // Serialize to JSON string
        val jsonString = json.encodeToString(prototypeResponse)

        // Deserialize from JSON string
        val deserialized = json.decodeFromString<PrototypeResponse>(jsonString)

        // Verify files are correctly serialized and deserialized
        assertTrue(deserialized.files.contains("file1.js"))
        assertTrue(deserialized.files.contains("console.log('Hello')"))
        assertTrue(deserialized.files.contains("file2.css"))
        assertTrue(deserialized.files.contains("body { color: black; }"))
        assertTrue(deserialized.files.contains("nested"))
        assertTrue(deserialized.files.contains("file3.html"))
        assertTrue(deserialized.files.contains("<html></html>"))
    }

    @Test
    fun `test PrototypeResponse with empty files`() {
        // Create an empty JSON object string
        val emptyJsonString = "{}"

        val prototypeResponse = PrototypeResponse(files = emptyJsonString)

        // Serialize to JSON string
        val jsonString = json.encodeToString(prototypeResponse)

        // Deserialize from JSON string
        val deserialized = json.decodeFromString<PrototypeResponse>(jsonString)

        // Verify files are empty
        assertEquals("{}", deserialized.files)
    }

    @Test
    fun `test ChatResponse with special characters`() {
        val timestamp = Instant.now().toString()
        val original = ChatResponse(
            message = "Test message with special characters: !@#$%^&*()_+{}|:<>?",
            role = "Test role",
            timestamp = timestamp,
            messageId = "123"
        )

        // Serialize to JSON string
        val jsonString = json.encodeToString(original)

        // Deserialize from JSON string
        val deserialized = json.decodeFromString<ChatResponse>(jsonString)

        // Verify message with special characters is correctly serialized and deserialized
        assertEquals(original.message, deserialized.message)
    }

    @Test
    fun `test ServerResponse with complex nested structure`() {
        val timestamp = Instant.now().toString()
        val chatResponse = ChatResponse(
            message = "Test message",
            role = "Test role",
            timestamp = timestamp,
            messageId = "123"
        )

        // Create a complex nested JSON string
        val filesJsonString = """
            {
                "file1.js": "console.log('Hello')",
                "directory": {
                    "file2.css": "body { color: black; }",
                    "array": ["item1", "item2"],
                    "nested": {
                        "file3.html": "<html></html>"
                    }
                }
            }
        """.trimIndent()

        val prototypeResponse = PrototypeResponse(files = filesJsonString)

        val serverResponse = ServerResponse(
            chat = chatResponse,
            prototype = prototypeResponse
        )

        // Serialize to JSON string
        val jsonString = json.encodeToString(serverResponse)

        // Deserialize from JSON string
        val deserialized = json.decodeFromString<ServerResponse>(jsonString)

        // Verify complex nested structure is correctly serialized and deserialized
        assertNotNull(deserialized.prototype)

        // Check that the files string contains all the expected elements
        val filesString = deserialized.prototype!!.files
        assertTrue(filesString.contains("file1.js"))
        assertTrue(filesString.contains("console.log('Hello')"))
        assertTrue(filesString.contains("directory"))
        assertTrue(filesString.contains("file2.css"))
        assertTrue(filesString.contains("body { color: black; }"))
        assertTrue(filesString.contains("array"))
        assertTrue(filesString.contains("item1"))
        assertTrue(filesString.contains("item2"))
        assertTrue(filesString.contains("nested"))
        assertTrue(filesString.contains("file3.html"))
        assertTrue(filesString.contains("<html></html>"))
    }
}
