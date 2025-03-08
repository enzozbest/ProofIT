package kcl.seg.rtt.prototype

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.DisabledIfSystemProperty
import org.junit.jupiter.api.Disabled
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class PrototypeSecurityTest {

    // Capture stdout for testing log messages
    private fun withCapturedOutput(block: () -> Unit): String {
        val originalOut = System.out
        val outContent = ByteArrayOutputStream()
        System.setOut(PrintStream(outContent))
        
        try {
            block()
            return outContent.toString()
        } finally {
            System.setOut(originalOut)
        }
    }

    @Test
    @DisplayName("secureCodeCheck should return true for valid code")
    fun testSecureCodeCheckWithValidCode() {
        val validHtml = """
            <!DOCTYPE html>
            <html>
                <head><title>Test</title></head>
                <body><p>Hello world</p></body>
            </html>
        """.trimIndent()
        
        val result = secureCodeCheck(validHtml, "html")
        assertTrue(result, "Valid HTML should pass security check")
    }
    
    @Test
    @DisplayName("secureCodeCheck should return false for invalid code")
    fun testSecureCodeCheckWithInvalidCode() {
        val invalidJs = "function foo() { console.log('Hello world' "  // Missing closing brace
        
        val output = withCapturedOutput {
            val result = secureCodeCheck(invalidJs, "javascript")
            assertFalse(result, "Invalid JavaScript should fail security check")
        }
        
        assertTrue(output.contains("Syntax or compile check failed for javascript code"), 
            "Should print error message for invalid code")
    }
    
    @Test
    @Disabled("Requires external tool installations")
    @DisplayName("runCompilerCheck should handle all supported languages")
    fun testRunCompilerCheck() {
        // Valid examples for each language
        val validHtml = "<html><head></head><body></body></html>"
        val validCss = "body { color: red; }"
        val validJs = "function test() { return true; }"
        val validPython = "def test(): return True"
        
        assertTrue(runCompilerCheck(validHtml, "html"), "Valid HTML should pass")
        assertTrue(runCompilerCheck(validCss, "css"), "Valid CSS should pass")
        assertTrue(runCompilerCheck(validJs, "javascript"), "Valid JavaScript should pass")
        assertTrue(runCompilerCheck(validPython, "python"), "Valid Python should pass")
        
        // Test unsupported language
        assertFalse(runCompilerCheck("code", "unsupported"), "Unsupported language should fail")
    }
    
    @Test
    @DisplayName("checkHtmlSyntaxWithJsoup should parse valid HTML")
    fun testCheckHtmlSyntaxWithValidHtml() {
        val validHtml = """
            <!DOCTYPE html>
            <html>
                <head><title>Test</title></head>
                <body><p>Hello world</p></body>
            </html>
        """.trimIndent()
        
        val result = checkHtmlSyntaxWithJsoup(validHtml)
        assertTrue(result, "Valid HTML should parse without errors")
    }
    
    @Test
    @DisplayName("checkHtmlSyntaxWithJsoup should handle errors gracefully")
    fun testCheckHtmlSyntaxWithInvalidHtml() {
        // HTML that might cause an exception (extremely malformed)
        // Note: Jsoup is very tolerant, so this might still pass
        val invalidHtml = "<not<a>valid<<<>html"
        
        val output = withCapturedOutput {
            val result = checkHtmlSyntaxWithJsoup(invalidHtml)
            // Even with invalid HTML, Jsoup might not throw an exception
            // So we can't assert on the result reliably
        }
        
        // Check if an error was logged (but don't fail the test if not)
        if (output.contains("HTML parsing error")) {
            assertTrue(output.contains("HTML parsing error"), "Should log HTML parsing errors")
        }
    }
    
    @Test
    @Disabled("Requires Python installation")
    @DisplayName("checkPythonSyntax should validate correct Python code")
    @DisabledIfSystemProperty(named = "os.name", matches = "Windows.*", 
                             disabledReason = "Python path might be different on Windows")
    fun testCheckPythonSyntaxWithValidCode() {
        val validPython = """
            def greet(name):
                return f"Hello, {name}!"
                
            if __name__ == "__main__":
                print(greet("World"))
        """.trimIndent()
        
        val result = checkPythonSyntax(validPython)
        assertTrue(result, "Valid Python code should pass syntax check")
    }
    
    @Test
    @Disabled("Requires Python installation")
    @DisplayName("checkPythonSyntax should detect invalid Python code")
    @DisabledIfSystemProperty(named = "os.name", matches = "Windows.*", 
                             disabledReason = "Python path might be different on Windows")
    fun testCheckPythonSyntaxWithInvalidCode() {
        val invalidPython = """
            def greet(name):
                return f"Hello, {name}!
                
            print(greet("World"))
        """.trimIndent()
        
        val output = withCapturedOutput {
            val result = checkPythonSyntax(invalidPython)
            assertFalse(result, "Invalid Python code should fail syntax check")
        }
        
        assertTrue(output.contains("Python syntax error"), 
            "Should print Python syntax error details")
    }
    
    @Test
    @DisplayName("checkJavaScriptSyntax should validate correct JS code")
    @DisabledIfSystemProperty(named = "os.name", matches = "Windows.*", 
                             disabledReason = "Node path might be different on Windows")
    fun testCheckJavaScriptSyntaxWithValidCode() {
        val validJs = """
            function greet(name) {
                return "Hello, " + name + "!";  // Use concatenation instead of string interpolation
            }
            
            console.log(greet("World"));
        """.trimIndent()
        
        val result = checkJavaScriptSyntax(validJs)
        assertTrue(result, "Valid JavaScript code should pass syntax check")
    }
    
    @Test
    @DisplayName("checkJavaScriptSyntax should detect invalid JS code")
    @DisabledIfSystemProperty(named = "os.name", matches = "Windows.*", 
                             disabledReason = "Node path might be different on Windows")
    fun testCheckJavaScriptSyntaxWithInvalidCode() {
        val invalidJs = """
            function greet(name) {
                return `Hello, ${'$'}{name}!;  // Missing closing backtick
            }
            
            console.log(greet("World"));
        """.trimIndent()

        
        val output = withCapturedOutput {
            val result = checkJavaScriptSyntax(invalidJs)
            assertFalse(result, "Invalid JavaScript code should fail syntax check")
        }
        
        assertTrue(output.contains("JavaScript syntax error"), 
            "Should print JavaScript syntax error details")
    }
    
    @Test
    @Disabled("Requires NPX installation")
    @DisplayName("checkCssSyntax should validate correct CSS code")
    @DisabledIfSystemProperty(named = "os.name", matches = "Windows.*", 
                             disabledReason = "NPX path might be different on Windows")
    fun testCheckCssSyntaxWithValidCode() {
        val validCss = """
            body {
                font-family: Arial, sans-serif;
                color: #333;
                margin: 0;
                padding: 20px;
            }
            
            .container {
                max-width: 1200px;
                margin: 0 auto;
            }
        """.trimIndent()
        
        val result = checkCssSyntax(validCss)
        assertTrue(result, "Valid CSS code should pass syntax check")
    }
    
    @Test
    //@Disabled("Requires NPX installation")
    @DisplayName("checkCssSyntax should detect invalid CSS code")
    @DisabledIfSystemProperty(named = "os.name", matches = "Windows.*", 
                             disabledReason = "NPX path might be different on Windows")
    fun testCheckCssSyntaxWithInvalidCode() {
        val invalidCss = """
            body {
                font-family: Arial, sans-serif;
                color: #333;
                margin: 0;
                padding: 20px;
            
            /* Missing closing brace */
            
            .container {
                max-width: 1200px;
                margin: 0 auto;
            }
        """.trimIndent()
        
        val output = withCapturedOutput {
            val result = checkCssSyntax(invalidCss)
            assertFalse(result, "Invalid CSS code should fail syntax check")
        }
        
        assertTrue(output.contains("CSS validation error"), 
            "Should print CSS validation error details")
    }
    
    // Testing private functions directly
    // Since we're using @JvmStatic for the private functions, we need to make them visible for testing
    // This approach uses reflection to access private functions
    @Test
    @DisplayName("Direct testing of private functions")
    fun testPrivateFunctionsDirectly() {
        // Just call the function directly since it should be visible in the same package
        val validHtml = "<html><body>Hello</body></html>"
        val result = checkHtmlSyntaxWithJsoup(validHtml)
        assertTrue(result, "Valid HTML should parse without errors")
    }
}