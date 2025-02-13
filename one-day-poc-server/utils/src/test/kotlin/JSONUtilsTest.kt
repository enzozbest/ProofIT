<<<<<<< HEAD
import kcl.seg.rtt.utils.JSON.PoCJSON
=======
import kcl.seg.rtt.utils.json.PoCJSON
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JSONUtilsTest {
<<<<<<< HEAD

=======
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
    @Test
    fun `Test parses JSON correctly`() {
        val json = PoCJSON.readJsonFile("src/test/resources/test.json")
        assertEquals("value", json["key"]!!.jsonPrimitive.content)
    }

    @Test
    fun `Test returns attribute when present`() {
<<<<<<< HEAD
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
=======
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
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
        assertEquals("test@example.com", PoCJSON.findCognitoUserAttribute(jsonArray, "email"))
    }

    @Test
    fun `Test returns null when not present`() {
<<<<<<< HEAD
        val jsonArray = buildJsonArray {
            add(buildJsonObject {
                put("Name", "sub")
                put("Value", "123456")
            })
        }
=======
        val jsonArray =
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("Name", "sub")
                        put("Value", "123456")
                    },
                )
            }
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
        assertNull(PoCJSON.findCognitoUserAttribute(jsonArray, "email"))
    }

    @Test
    fun `Test returns null when Name key is missing`() {
<<<<<<< HEAD
        val jsonArray = buildJsonArray {
            add(buildJsonObject {
                put("Value", "test@example.com")
            })
        }
=======
        val jsonArray =
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("Value", "test@example.com")
                    },
                )
            }
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
        assertNull(PoCJSON.findCognitoUserAttribute(jsonArray, "email"))
    }

    @Test
    fun `Test returns null when Value key is missing`() {
<<<<<<< HEAD
        val jsonArray = buildJsonArray {
            add(buildJsonObject {
                put("Name", "email")
            })
        }
=======
        val jsonArray =
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("Name", "email")
                    },
                )
            }
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
        assertNull(PoCJSON.findCognitoUserAttribute(jsonArray, "email"))
    }

    @Test
    fun `Test returns null when Name is not a JSON primitive`() {
<<<<<<< HEAD
        val jsonArray = buildJsonArray {
            add(buildJsonObject {
                put("Name", buildJsonObject { put("unexpected", "value") })
                put("Value", "test@example.com")
            })
        }
=======
        val jsonArray =
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("Name", buildJsonObject { put("unexpected", "value") })
                        put("Value", "test@example.com")
                    },
                )
            }
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
        assertNull(PoCJSON.findCognitoUserAttribute(jsonArray, "email"))
    }

    @Test
    fun `Test returns null when element is not a JSON object`() {
<<<<<<< HEAD
        val jsonArray = buildJsonArray {
            add(JsonPrimitive("not an object"))
        }
=======
        val jsonArray =
            buildJsonArray {
                add(JsonPrimitive("not an object"))
            }
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
        assertNull(PoCJSON.findCognitoUserAttribute(jsonArray, "email"))
    }

    @Test
    fun `Test returns null when Value is not a JSON primitive`() {
<<<<<<< HEAD
        val jsonArray = buildJsonArray {
            add(buildJsonObject {
                put("Name", "email")
                put("Value", buildJsonObject { put("nested", "value") })
            })
        }
=======
        val jsonArray =
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("Name", "email")
                        put("Value", buildJsonObject { put("nested", "value") })
                    },
                )
            }
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
        assertNull(PoCJSON.findCognitoUserAttribute(jsonArray, "email"))
    }

    @Test
    fun `Test returns null when Name is JsonNull`() {
<<<<<<< HEAD
        val jsonArray = buildJsonArray {
            add(buildJsonObject {
                put("Name", JsonNull)
                put("Value", "test@example.com")
            })
        }
=======
        val jsonArray =
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("Name", JsonNull)
                        put("Value", "test@example.com")
                    },
                )
            }
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
        val result = PoCJSON.findCognitoUserAttribute(jsonArray, "email")
        assertNull(result)
    }

    @Test
    fun `Test handles case insensitivity correctly`() {
<<<<<<< HEAD
        val jsonArray = buildJsonArray {
            add(buildJsonObject {
                put("Name", "email")
                put("Value", "test@example.com")
            })
        }
=======
        val jsonArray =
            buildJsonArray {
                add(
                    buildJsonObject {
                        put("Name", "email")
                        put("Value", "test@example.com")
                    },
                )
            }
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
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
<<<<<<< HEAD
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
=======
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
>>>>>>> fa550d0623b36f1e3b6380a38a3cd7b555ee1f94
        val result = PoCJSON.findCognitoUserAttribute(jsonArray, "email")
        assertEquals("first@example.com", result)
    }
}
