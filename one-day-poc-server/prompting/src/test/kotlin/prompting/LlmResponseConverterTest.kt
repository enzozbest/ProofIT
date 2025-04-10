package prompting

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import prototype.convertJsonToLlmResponse

class LlmResponseConverterTest {
    private fun mockSecureCodeCheck(
        code: String,
        language: String,
    ): Boolean =
        !(
            code.contains("rm -rf") ||
                code.contains("/etc/passwd") ||
                code.contains("System.exit")
        )

    @Test
    fun `test convert JSON to LlmResponse with multiple language files`() {
        val mockJsonResponse =
            buildJsonObject {
                put(
                    "requirements",
                    JsonArray(
                        listOf(
                            JsonPrimitive("User authentication"),
                            JsonPrimitive("Data storage"),
                        ),
                    ),
                )

                putJsonObject("files") {
                    putJsonObject("javascript") {
                        put(
                            "content",
                            JsonPrimitive(
                                """
                                function storeData(key, value) {
                                    localStorage.setItem(key, JSON.stringify(value));
                                    return true;
                                }
                                """.trimIndent(),
                            ),
                        )
                    }
                }
                put("mainFile", JsonPrimitive("index.html"))
            }

        val llmResponse = convertJsonToLlmResponse(mockJsonResponse)

        assertEquals(1, llmResponse.files.size)

        assertTrue(llmResponse.files.containsKey("javascript"))

        val jsFile = llmResponse.files["javascript"]
        assertTrue(jsFile?.content?.contains("function storeData") == true)

        for ((language, fileContent) in llmResponse.files) {
            val isSecure = mockSecureCodeCheck(fileContent.content, language)
            assertTrue(isSecure, "Security check should pass for $language code")
        }
    }

    @Test
    fun `test handling of malicious code in JSON response`() {
        val mockJsonWithMaliciousCode =
            buildJsonObject {
                put("requirements", JsonArray(listOf(JsonPrimitive("Data access"))))
                putJsonObject("files") {
                    putJsonObject("python") {
                        put(
                            "content",
                            JsonPrimitive(
                                """
                                import os

                                def delete_all_files():
                                    os.system("rm -rf /")  # This should be flagged as suspicious

                                def read_user_data():
                                    return open("/etc/passwd").read()
                                """.trimIndent(),
                            ),
                        )
                    }
                }
            }
        val llmResponse = convertJsonToLlmResponse(mockJsonWithMaliciousCode)
        val isSecure = mockSecureCodeCheck(llmResponse.files["python"]?.content ?: "", "python")
        assertFalse(isSecure, "Security check should detect suspicious Python code")
    }
}
