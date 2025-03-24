package chat.storage

import database.core.DatabaseManager
import database.tables.chats.ChatRepository
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ChatStorageFactoryTest {

    private val mockRepository = mockk<ChatRepository>()

    @BeforeEach
    fun setUp() {
        // Mock DatabaseManager
        mockkObject(DatabaseManager)
        every { DatabaseManager.externalInit() } just runs
        every { DatabaseManager.chatRepository() } returns mockRepository
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test getChatRepository initializes repository on first call`() {
        // First call should initialize the repository
        val repository = ChatStorageFactory.getChatRepository()
        
        // Verify that externalInit and chatRepository were called
        verify(exactly = 1) { DatabaseManager.externalInit() }
        verify(exactly = 1) { DatabaseManager.chatRepository() }
        
        // Verify that the returned repository is the mock repository
        assertEquals(mockRepository, repository)
    }

    @Test
    fun `test getChatRepository returns cached repository on subsequent calls`() {
        // First call
        val repository1 = ChatStorageFactory.getChatRepository()
        
        // Second call
        val repository2 = ChatStorageFactory.getChatRepository()
        
        // Verify that externalInit and chatRepository were called only once
        verify(exactly = 1) { DatabaseManager.externalInit() }
        verify(exactly = 1) { DatabaseManager.chatRepository() }
        
        // Verify that both calls return the same instance
        assertSame(repository1, repository2)
    }
}