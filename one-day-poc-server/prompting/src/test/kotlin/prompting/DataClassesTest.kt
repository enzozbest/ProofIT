package prompting

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

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

        val filesJson = buildJsonObject {
            put("file1.js", JsonPrimitive("console.log('Hello')"))
            put("file2.css", JsonPrimitive("body { color: black; }"))
        }

        val prototypeResponse = PrototypeResponse(files = filesJson)

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
        assertEquals(2, deserialized.prototype!!.files.size)
        assertEquals("console.log('Hello')", deserialized.prototype!!.files["file1.js"]?.jsonPrimitive?.content)
        assertEquals("body { color: black; }", deserialized.prototype!!.files["file2.css"]?.jsonPrimitive?.content)
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
        val filesJson = buildJsonObject {
            put("file1.js", JsonPrimitive("console.log('Hello')"))
            put("file2.css", JsonPrimitive("body { color: black; }"))
            putJsonObject("nested") {
                put("file3.html", JsonPrimitive("<html></html>"))
            }
        }

        val prototypeResponse = PrototypeResponse(files = filesJson)

        // Serialize to JSON string
        val jsonString = json.encodeToString(prototypeResponse)

        // Deserialize from JSON string
        val deserialized = json.decodeFromString<PrototypeResponse>(jsonString)

        // Verify files are correctly serialized and deserialized
        assertEquals(3, deserialized.files.size)
        assertEquals("console.log('Hello')", deserialized.files["file1.js"]?.jsonPrimitive?.content)
        assertEquals("body { color: black; }", deserialized.files["file2.css"]?.jsonPrimitive?.content)

        val nested = deserialized.files["nested"] as? JsonObject
        assertNotNull(nested)
        assertEquals("<html></html>", nested["file3.html"]?.jsonPrimitive?.content)
    }

    @Test
    fun `test PrototypeResponse with empty files`() {
        val filesJson = buildJsonObject {}

        val prototypeResponse = PrototypeResponse(files = filesJson)

        // Serialize to JSON string
        val jsonString = json.encodeToString(prototypeResponse)

        // Deserialize from JSON string
        val deserialized = json.decodeFromString<PrototypeResponse>(jsonString)

        // Verify files are empty
        assertEquals(0, deserialized.files.size)
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

        val filesJson = buildJsonObject {
            put("file1.js", JsonPrimitive("console.log('Hello')"))
            putJsonObject("directory") {
                put("file2.css", JsonPrimitive("body { color: black; }"))
                putJsonArray("array") {
                    add(JsonPrimitive("item1"))
                    add(JsonPrimitive("item2"))
                }
                putJsonObject("nested") {
                    put("file3.html", JsonPrimitive("<html></html>"))
                }
            }
        }

        val prototypeResponse = PrototypeResponse(files = filesJson)

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
        assertEquals("console.log('Hello')", deserialized.prototype!!.files["file1.js"]?.jsonPrimitive?.content)

        val directory = deserialized.prototype!!.files["directory"] as? JsonObject
        assertNotNull(directory)
        assertEquals("body { color: black; }", directory["file2.css"]?.jsonPrimitive?.content)

        val array = directory["array"] as? JsonArray
        assertNotNull(array)
        assertEquals(2, array.size)
        assertEquals("item1", array[0].jsonPrimitive.content)
        assertEquals("item2", array[1].jsonPrimitive.content)

        val nested = directory["nested"] as? JsonObject
        assertNotNull(nested)
        assertEquals("<html></html>", nested["file3.html"]?.jsonPrimitive?.content)
    }
}
