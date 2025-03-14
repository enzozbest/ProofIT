package kcl.seg.rtt.prototype.prototype.helpers

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import prototype.helpers.OllamaException


class OllamaExceptionTest {
    @Test
    fun `Test OllamaException is created correctly`() {
        val exception = OllamaException("Test error message")
        assertEquals("Test error message", exception.message)
    }
}