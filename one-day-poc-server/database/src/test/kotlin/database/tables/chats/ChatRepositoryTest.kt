package database.tables.chats

import database.core.DatabaseManager
import database.helpers.MockEnvironment
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import utils.environment.EnvironmentLoader
import java.io.File
import java.time.Instant
import java.util.*
import kotlin.test.*

class ChatRepositoryTest {
    private lateinit var db: Database
    private lateinit var repository: ChatRepository

    @BeforeEach
    fun setUp() {
        MockEnvironment.postgresContainer.start()
        EnvironmentLoader.reset()
        MockEnvironment.generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.ENV_FILE)

        db = DatabaseManager.init()
        transaction(db) {
            SchemaUtils.create(ConversationTable)
            SchemaUtils.create(ChatMessageTable)
            SchemaUtils.create(PrototypeTable)
        }
        
        repository = ChatRepository(db)
    }

    @AfterEach
    fun tearDown() {
        transaction(db) {
            SchemaUtils.drop(PrototypeTable)
            SchemaUtils.drop(ChatMessageTable)
            SchemaUtils.drop(ConversationTable)
        }
        File(MockEnvironment.ENV_FILE).delete()
        MockEnvironment.stopContainer()
    }

    // ==== Message Tests ====

    @Test
    fun `test saving and retrieving chat message`() = runTest {
        val messageId = UUID.randomUUID().toString()
        val message = ChatMessage(
            id = messageId,
            conversationId = UUID.randomUUID().toString(),
            senderId = "user1",
            content = "Hello",
            timestamp = Instant.now()
        )

        val saveResult = repository.saveMessage(message)
        assertTrue(saveResult)

        val retrieved = repository.getMessageById(messageId)
        assertNotNull(retrieved)
        assertEquals(message.content, retrieved.content)
    }
    
    @Test
    fun `test saving message with invalid conversationId`() = runTest {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = "invalid-uuid",
            senderId = "user1",
            content = "Hello with invalid parent",
            timestamp = Instant.now()
        )

        val saveResult = repository.saveMessage(message)
        assertTrue(saveResult) // Should still succeed but use a random UUID
    }
    
    @Test
    fun `test saving message with empty conversationId creates new conversation`() = runTest {
        val messageId = UUID.randomUUID().toString()
        val message = ChatMessage(
            id = messageId,
            conversationId = "",
            senderId = "user1",
            content = "Hello with no conversation",
            timestamp = Instant.now()
        )

        val saveResult = repository.saveMessage(message)
        assertTrue(saveResult)
        
        val retrieved = repository.getMessageById(messageId)
        assertNotNull(retrieved)
        assertNotEquals("", retrieved.conversationId) // Should have generated a valid UUID
    }
    
    @Test
    fun `test retrieving conversation messages with paging`() = runTest {
        val conversationId = UUID.randomUUID().toString()
        
        val messages = (1..5).map { 
            ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = "user1",
                content = "Message $it",
                timestamp = Instant.now().plusSeconds(it.toLong())
            )
        }
        
        messages.forEach { repository.saveMessage(it) }
        
        val firstPage = repository.getMessagesByConversation(conversationId, 2, 0)
        assertEquals(2, firstPage.size)
        assertEquals("Message 1", firstPage[0].content)
        assertEquals("Message 2", firstPage[1].content)
        
        val secondPage = repository.getMessagesByConversation(conversationId, 2, 2)
        assertEquals(2, secondPage.size)
        assertEquals("Message 3", secondPage[0].content)
        assertEquals("Message 4", secondPage[1].content)
        
        val thirdPage = repository.getMessagesByConversation(conversationId, 2, 4)
        assertEquals(1, thirdPage.size)
        assertEquals("Message 5", thirdPage[0].content)
    }
    
    @Test
    fun `test deleting message`() = runTest {
        val messageId = UUID.randomUUID().toString()
        val message = ChatMessage(
            id = messageId,
            conversationId = UUID.randomUUID().toString(),
            senderId = "user1",
            content = "Hello for deletion",
            timestamp = Instant.now()
        )
        
        repository.saveMessage(message)
        val deleteResult = repository.deleteMessage(messageId)
        assertTrue(deleteResult)
        
        val retrieved = repository.getMessageById(messageId)
        assertNull(retrieved)
    }

    // ==== Conversation Tests ====
    
    @Test
    fun `test updating conversation name`() = runTest {
        val conversationId = UUID.randomUUID().toString()
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversationId,
            senderId = "user1",
            content = "Hello",
            timestamp = Instant.now()
        )
        
        repository.saveMessage(message)
        
        val updateResult = repository.updateConversationName(conversationId, "Updated Name")
        assertTrue(updateResult)
        
        val conversations = repository.getConversationsByUser("user1")
        assertEquals(1, conversations.size)
        assertEquals("Updated Name", conversations[0].name)
    }
    
    @Test
    fun `test getting conversation message count`() = runTest {
        val conversationId = UUID.randomUUID().toString()
        
        repeat(3) {
            repository.saveMessage(ChatMessage(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                senderId = "user1",
                content = "Message $it",
                timestamp = Instant.now()
            ))
        }
        
        val count = repository.getConversationMessageCount(conversationId)
        assertEquals(3, count)
    }
    
    @Test
    fun `test getting conversations by user`() = runTest {
        val userId = "test-user"
        
        val conversation1Id = UUID.randomUUID().toString()
        val conversation2Id = UUID.randomUUID().toString()
        
        repository.saveMessage(ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversation1Id,
            senderId = userId,
            content = "Message in conversation 1",
            timestamp = Instant.now().minusSeconds(3600)
        ))
        
        repository.saveMessage(ChatMessage(
            id = UUID.randomUUID().toString(),
            conversationId = conversation2Id,
            senderId = userId,
            content = "Message in conversation 2",
            timestamp = Instant.now()
        ))
        
        repository.updateConversationName(conversation1Id, "Conversation 1")
        repository.updateConversationName(conversation2Id, "Conversation 2")
        
        val conversations = repository.getConversationsByUser(userId)
        
        assertEquals(2, conversations.size)
        assertEquals("Conversation 2", conversations[0].name)
        assertEquals("Conversation 1", conversations[1].name)
    }

    // ==== Prototype Tests ====
    
    @Test
    fun `test saving and retrieving prototype`() = runTest {
        val messageId = UUID.randomUUID().toString()
        val message = ChatMessage(
            id = messageId,
            conversationId = UUID.randomUUID().toString(),
            senderId = "user1",
            content = "Hello",
            timestamp = Instant.now()
        )
        repository.saveMessage(message)

        val prototypeId = UUID.randomUUID().toString()
        val prototype = Prototype(
            id = prototypeId,
            messageId = messageId,
            filesJson = """{"files": []}""",
            version = 1,
            isSelected = true,
            timestamp = Instant.now()
        )

        val saveResult = repository.savePrototype(prototype)
        assertTrue(saveResult)

        val retrieved = repository.getPrototypesByMessageId(messageId)
        assertEquals(1, retrieved.size)
        assertEquals(prototype.filesJson, retrieved[0].filesJson)
    }
    
    @Test
    fun `test retrieving prototypes for a message`() = runTest {
        val messageId = UUID.randomUUID().toString()
        val conversationId = UUID.randomUUID().toString()
        
        repository.saveMessage(ChatMessage(
            id = messageId,
            conversationId = conversationId,
            senderId = "user1",
            content = "Hello",
            timestamp = Instant.now()
        ))
        
        val prototype1 = Prototype(
            id = UUID.randomUUID().toString(),
            messageId = messageId,
            filesJson = """{"files": [{"name": "first.js"}]}""",
            version = 1,
            isSelected = true,
            timestamp = Instant.now().minusSeconds(60)
        )
        
        val prototype2 = Prototype(
            id = UUID.randomUUID().toString(),
            messageId = messageId,
            filesJson = """{"files": [{"name": "second.js"}]}""",
            version = 2,
            isSelected = false,
            timestamp = Instant.now()
        )
        
        repository.savePrototype(prototype1)
        repository.savePrototype(prototype2)
        
        val prototypes = repository.getPrototypesByMessageId(messageId)
        assertEquals(2, prototypes.size)
    }
    
    @Test
    fun `test retrieving selected prototype for message`() = runTest {
        val conversationId = UUID.randomUUID().toString()
        val messageId = UUID.randomUUID().toString()
        
        val message = ChatMessage(
            id = messageId,
            conversationId = conversationId,
            senderId = "user1",
            content = "Hello",
            timestamp = Instant.now()
        )
        repository.saveMessage(message)

        val prototype1 = Prototype(
            id = UUID.randomUUID().toString(),
            messageId = messageId,
            filesJson = """{"files": [1]}""",
            version = 1,
            isSelected = true,
            timestamp = Instant.now()
        )
        val prototype2 = Prototype(
            id = UUID.randomUUID().toString(),
            messageId = messageId,
            filesJson = """{"files": [2]}""",
            version = 2,
            isSelected = false,
            timestamp = Instant.now()
        )

        repository.savePrototype(prototype1)
        repository.savePrototype(prototype2)

        val selected = repository.getSelectedPrototypeForMessage(conversationId, messageId)
        assertNotNull(selected)
        assertEquals(prototype1.filesJson, selected.filesJson)
    }
    
    @Test
    fun `test getting all prototypes in a conversation`() = runTest {
        val conversationId = UUID.randomUUID().toString()
        
        val message1Id = UUID.randomUUID().toString()
        val message2Id = UUID.randomUUID().toString()
        
        repository.saveMessage(ChatMessage(
            id = message1Id,
            conversationId = conversationId,
            senderId = "user1",
            content = "Message 1",
            timestamp = Instant.now()
        ))
        
        repository.saveMessage(ChatMessage(
            id = message2Id,
            conversationId = conversationId,
            senderId = "user1",
            content = "Message 2",
            timestamp = Instant.now()
        ))
        
        val prototype1 = Prototype(
            id = UUID.randomUUID().toString(),
            messageId = message1Id,
            filesJson = """{"files": [{"msg": "1"}]}""",
            version = 1,
            isSelected = true,
            timestamp = Instant.now()
        )
        
        val prototype2 = Prototype(
            id = UUID.randomUUID().toString(),
            messageId = message2Id,
            filesJson = """{"files": [{"msg": "2"}]}""",
            version = 1,
            isSelected = true,
            timestamp = Instant.now()
        )
        
        repository.savePrototype(prototype1)
        repository.savePrototype(prototype2)
        
        val prototypes = repository.getAllPrototypesInConversation(conversationId)
        assertEquals(2, prototypes.size)
    }
    
    @Test
    fun `test failure handling for invalid message ID`() = runTest {
        val invalidId = "invalid-uuid"
        val result = repository.getMessageById(invalidId)
        assertEquals(null, result)
    }

    @Test
    fun `test failure handling for non-existent prototype`() = runTest {
        val nonExistentId = UUID.randomUUID().toString()
        val prototypes = repository.getPrototypesByMessageId(nonExistentId)
        assertTrue(prototypes.isEmpty())
    }
    
    @Test
    fun `test failure handling for invalid conversation ID when getting prototypes`() = runTest {
        val invalidId = "invalid-uuid"
        val prototypes = repository.getAllPrototypesInConversation(invalidId)
        assertTrue(prototypes.isEmpty())
    }
}
