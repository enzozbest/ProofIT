package kcl.seg.rtt.chat

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ChatEndpointTest {

    @BeforeEach
    fun setup() {
        ChatEndpoint.resetToDefault()
    }

    @Test
    fun `Test ChatEndpoint can be instantiated`() {
        val chatEndpoint = ChatEndpoint()
        assertNotNull(chatEndpoint)
    }

    @Test
    fun `Test default upload directory`() {
        assertEquals("uploads", ChatEndpoint.getUploadDirectory())
    }

    @Test
    fun `Test setting custom upload directory`() {
        ChatEndpoint.setUploadDirectory("custom_uploads")
        assertEquals("custom_uploads", ChatEndpoint.getUploadDirectory())
    }

    @Test
    fun `Test resetting upload directory to default`() {
        ChatEndpoint.setUploadDirectory("custom_uploads")
        ChatEndpoint.resetToDefault()
        assertEquals("uploads", ChatEndpoint.getUploadDirectory())
    }
}