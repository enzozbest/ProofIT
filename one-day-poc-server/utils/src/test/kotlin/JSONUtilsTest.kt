import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import utils.json.PoCJSON
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JSONUtilsTest {
    @Test
    fun `Test parses JSON correctly`() {
        val json = PoCJSON.readJsonFile("src/test/resources/test.json")
        assertEquals("value", json["key"]!!.jsonPrimitive.content)
    }

    @Test
    fun `Test returns attribute when present`() {
        val jsonArray =
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("Name", "email")
                        put("Value", "test@example.com")
                    },
                )
                add(
                    buildJsonObject {
                        put("Name", "sub")
                        put("Value", "123456")
                    },
                )
            }
        assertEquals("test@example.com", PoCJSON.findCognitoUserAttribute(jsonArray, "email"))
    }

    @Test
    fun `Test returns null when not present`() {
        val jsonArray =
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("Name", "sub")
                        put("Value", "123456")
                    },
                )
            }
        assertNull(PoCJSON.findCognitoUserAttribute(jsonArray, "email"))
    }

    @Test
    fun `Test returns null when Name key is missing`() {
        val jsonArray =
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("Value", "test@example.com")
                    },
                )
            }
        assertNull(PoCJSON.findCognitoUserAttribute(jsonArray, "email"))
    }

    @Test
    fun `Test returns null when Value key is missing`() {
        val jsonArray =
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("Name", "email")
                    },
                )
            }
        assertNull(PoCJSON.findCognitoUserAttribute(jsonArray, "email"))
    }

    @Test
    fun `Test returns null when Name is not a JSON primitive`() {
        val jsonArray =
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("Name", buildJsonObject { put("unexpected", "value") })
                        put("Value", "test@example.com")
                    },
                )
            }
        assertNull(PoCJSON.findCognitoUserAttribute(jsonArray, "email"))
    }

    @Test
    fun `Test returns null when element is not a JSON object`() {
        val jsonArray =
            buildJsonArray {
                add(JsonPrimitive("not an object"))
            }
        assertNull(PoCJSON.findCognitoUserAttribute(jsonArray, "email"))
    }

    @Test
    fun `Test returns null when Value is not a JSON primitive`() {
        val jsonArray =
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("Name", "email")
                        put("Value", buildJsonObject { put("nested", "value") })
                    },
                )
            }
        assertNull(PoCJSON.findCognitoUserAttribute(jsonArray, "email"))
    }

    @Test
    fun `Test returns null when Name is JsonNull`() {
        val jsonArray =
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("Name", JsonNull)
                        put("Value", "test@example.com")
                    },
                )
            }
        val result = PoCJSON.findCognitoUserAttribute(jsonArray, "email")
        assertNull(result)
    }

    @Test
    fun `Test handles case insensitivity correctly`() {
        val jsonArray =
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("Name", "email")
                        put("Value", "test@example.com")
                    },
                )
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
        val jsonArray =
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("Name", "email")
                        put("Value", "first@example.com")
                    },
                )
                add(
                    buildJsonObject {
                        put("Name", "email")
                        put("Value", "second@example.com")
                    },
                )
            }
        val result = PoCJSON.findCognitoUserAttribute(jsonArray, "email")
        assertEquals("first@example.com", result)
    }
}
