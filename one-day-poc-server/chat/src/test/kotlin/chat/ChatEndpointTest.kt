package chat

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ChatEndpointTest {
    @Test
    fun `Test default upload directory`() {
        assertEquals("uploads", ChatEndpoints.UPLOAD_DIR)
    }
}
