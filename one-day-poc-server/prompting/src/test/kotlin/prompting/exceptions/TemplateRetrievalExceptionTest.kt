package prompting.exceptions

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class TemplateRetrievalExceptionTest {
    @Test
    fun `Test that TemplateRetrievalException is created correctly`() {
        val exception = TemplateRetrievalException("Test error message")
        assertEquals(exception.message, "Test error message")
    }
}
