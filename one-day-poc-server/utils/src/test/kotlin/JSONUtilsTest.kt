import kcl.seg.rtt.utils.JSON.PoCJSON
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JSONUtilsTest {

    @Test
    fun testJSONReader() {
        val json = PoCJSON.readJsonFile("src/test/resources/test.json")
        assertEquals("value", json["key"]!!.jsonPrimitive.content)
    }

    @Test
    fun `Test returns attribute when present`() {
        val jsonArray = buildJsonArray {
            add(buildJsonObject {
                put("Name", "email")
                put("Value", "test@example.com")
            })
            add(buildJsonObject {
                put("Name", "sub")
                put("Value", "123456")
            })
        }

        val result = PoCJSON.findCognitoUserAttribute(jsonArray, "email")
        assertEquals("test@example.com", result)
    }

    @Test
    fun `Test returns null when not present`() {
        val jsonArray = buildJsonArray {
            add(buildJsonObject {
                put("Name", "sub")
                put("Value", "123456")
            })
        }

        val result = PoCJSON.findCognitoUserAttribute(jsonArray, "email")
        assertNull(result)
    }

    @Test
    fun `Test returns null when Name key is missing`() {
        val jsonArray = buildJsonArray {
            add(buildJsonObject {
                put("Value", "test@example.com")
            })
        }

        val result = PoCJSON.findCognitoUserAttribute(jsonArray, "email")
        assertNull(result)
    }

    @Test
    fun `Test returns null when Value key is missing`() {
        val jsonArray = buildJsonArray {
            add(buildJsonObject {
                put("Name", "email")
            })
        }

        val result = PoCJSON.findCognitoUserAttribute(jsonArray, "email")
        assertNull(result)
    }

    @Test
    fun `Test handles case insensitivity correctly`() {
        val jsonArray = buildJsonArray {
            add(buildJsonObject {
                put("Name", "email")
                put("Value", "test@example.com")
            })
        }

        val result = PoCJSON.findCognitoUserAttribute(jsonArray, "Email")
        assertEquals("test@example.com", result)
    }

    @Test
    fun `Test returns null when array is empty`() {
        val jsonArray = buildJsonArray {}

        val result = PoCJSON.findCognitoUserAttribute(jsonArray, "email")
        assertNull(result)
    }

    @Test
    fun `Test handles multiple attributes correctly`() {
        val jsonArray = buildJsonArray {
            add(buildJsonObject {
                put("Name", "email")
                put("Value", "first@example.com")
            })
            add(buildJsonObject {
                put("Name", "email")
                put("Value", "second@example.com")
            })
        }

        val result = PoCJSON.findCognitoUserAttribute(jsonArray, "email")
        assertEquals("first@example.com", result)
    }
}
