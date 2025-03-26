package database.tables.chats

import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ChatModelsTest {
    @Test
    fun `test default chatMessage initialization`() {
        val message = ChatMessage(
            conversationId = "1",
            senderId = "user1",
            content = "Hello, World!"
        )

        assertNotNull(message.id, "ID should be generated")
        assertNotNull(message.timestamp, "Timestamp should be generated")
        assertEquals("1", message.conversationId)
        assertEquals("user1", message.senderId)
        assertEquals("Hello, World!", message.content)
    }

    @Test
    fun `test default Conversation initialization`() {
        val now = Instant.now().toString()
        val conversation = Conversation(
            id = "1",
            name ="new conversation",
            lastModified = now,
            messageCount=0,
            userId="1"

        )

        assertNotNull(conversation.id, "ID should be generated")
        assertEquals("1", conversation.id)
        assertEquals("new conversation", conversation.name)
        assertEquals(now, conversation.lastModified)
        assertEquals(0, conversation.messageCount)
        assertEquals("1", conversation.userId)
    }
}