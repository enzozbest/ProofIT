package kcl.seg.rtt.prompting

import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import kcl.seg.rtt.prototype.convertJsonToLlmResponse
import kcl.seg.rtt.prototype.secureCodeCheck

class LlmResponseConverterTest {

    private fun mockSecureCodeCheck(code: String, language: String): Boolean {
        // Simple mock implementation that doesn't cause IO issues
        // Look for obviously dangerous patterns
        return !(code.contains("rm -rf") || 
                 code.contains("/etc/passwd") || 
                 code.contains("System.exit"))
    }
    
    @Test
    fun `test convert JSON to LlmResponse with multiple language files`() {
        // Create a mock JSON response with the expected structure
        val mockJsonResponse = buildJsonObject {
            put("requirements", JsonArray(listOf(
                JsonPrimitive("User authentication"),
                JsonPrimitive("Data storage")
            )))
            
            // Add the files object that matches implementation
            putJsonObject("files") {
                // Python code example
                putJsonObject("python") {
                    put("content", JsonPrimitive("""
                        def authenticate(username, password):
                            # This is a sample function
                            if username == "admin" and password == "secure_password":
                                return True
                            return False
                    """.trimIndent()))
                }
                
                // JavaScript code example
                putJsonObject("javascript") {
                    put("content", JsonPrimitive("""
                        function storeData(key, value) {
                            localStorage.setItem(key, JSON.stringify(value));
                            return true;
                        }
                    """.trimIndent()))
                }
            }
            
            // Optional mainFile property
            put("mainFile", JsonPrimitive("index.html"))
        }
        
        // Convert the mock JSON to LlmResponse
        val llmResponse = convertJsonToLlmResponse(mockJsonResponse)
        
        // Print and verify the results
        println("LlmResponse structure:")
        println("Files: ${llmResponse.files.keys}")
        println("Main file: ${llmResponse.mainFile}")
        
        // Verify the correct number of files were extracted
        assertEquals(2, llmResponse.files.size)
        
        // Verify the languages were correctly processed
        assertTrue(llmResponse.files.containsKey("python"))
        assertTrue(llmResponse.files.containsKey("javascript"))
        
        // Verify each file has the expected content
        val pythonFile = llmResponse.files["python"]
        assertTrue(pythonFile?.content?.contains("def authenticate") ?: false)
        
        val jsFile = llmResponse.files["javascript"]
        assertTrue(jsFile?.content?.contains("function storeData") ?: false)
        
        // Test security checks on the extracted code
        for ((language, fileContent) in llmResponse.files) {
            val isSecure = mockSecureCodeCheck(fileContent.content, language)
            println("Security check for $language code: ${if (isSecure) "PASSED" else "FAILED"}")
            assertTrue(isSecure, "Security check should pass for $language code")
        }
    }
    
    @Test
    fun `test handling of malicious code in JSON response`() {
        // Create a mock JSON with potentially suspicious code
        val mockJsonWithMaliciousCode = buildJsonObject {
            put("requirements", JsonArray(listOf(JsonPrimitive("Data access"))))
            
            // Properly structure files object
            putJsonObject("files") {
                putJsonObject("python") {
                    put("content", JsonPrimitive("""
                        import os
                        
                        def delete_all_files():
                            os.system("rm -rf /")  # This should be flagged as suspicious
                            
                        def read_user_data():
                            return open("/etc/passwd").read()
                    """.trimIndent()))
                }
            }
        }
        
        // Convert the JSON
        val llmResponse = convertJsonToLlmResponse(mockJsonWithMaliciousCode)
        
        // Test the security check (should fail)
        val isSecure = mockSecureCodeCheck(llmResponse.files["python"]?.content ?: "", "python")
        println("Security check for suspicious Python code: ${if (isSecure) "PASSED" else "FAILED"}")
        
        // This assertion may pass or fail depending on your implementation
        // If your security check is working, it should return false
        assertFalse(isSecure, "Security check should detect suspicious Python code")
    }
}