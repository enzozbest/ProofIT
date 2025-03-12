package prototype.security

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrototypeSecurityTest {
    private val mockRunCompilerCheck = mockk<(String, String) -> Boolean>()
    private fun mockCompilerCheck(result: Boolean) {
        every { mockRunCompilerCheck(any(), any()) } returns result
    }

    @Test
    fun `Test valid Python code passes`() {
        val pythonCode = """
            def hello():
                print("Hello, World!")
        """.trimIndent()
        mockCompilerCheck(true)

        val result = secureCodeCheck(pythonCode, "python")

        assertTrue(result, "Expected code to pass all security checks")
    }

    @Test
    fun `Test invalid Python code fails`() {
        val pythonCode = """
            def hello()
                print("Hello, World!")
        """.trimIndent()

        mockCompilerCheck(false)

        val result = secureCodeCheck(pythonCode, "python")

        assertFalse(result, "Expected code to fail due to compilation error")
    }

    @Test
    fun `Test unsupported language fails`() {
        val code = "some code"
        mockCompilerCheck(false)
        val result = secureCodeCheck(code, "rust")

        assertFalse(result, "Expected code to fail for unsupported language")
    }

    @Test
    fun `Test valid HTML code passes`() {
        val htmlCode = "<html><body><p>Hello, World!</p></body></html>"

        mockCompilerCheck(true)
        val result = secureCodeCheck(htmlCode, "html")

        assertTrue(result, "Expected HTML code to pass security checks")
    }

    @Test
    fun `Test valid CSS code passes`() {
        val cssCode = """
            body {
                background-color: #f0f0f0;
                color: #333;
            }
        """.trimIndent()

        mockCompilerCheck(true)
        val result = secureCodeCheck(cssCode, "css")

        assertTrue(result, "Expected CSS code to pass security checks")
    }

    @Test
    fun `Test valid Javascript code passes`() {
        val jsCode = """
            function hello() {
                console.log("Hello, World!");
            }
            greet();
        """.trimIndent()

        mockCompilerCheck(true)
        val result = secureCodeCheck(jsCode, "javascript")

        assertTrue(result, "Expected JavaScript  code to pass security checks")
    }


}
