package chat.storage

import database.core.DatabaseManager
import database.tables.chats.ChatRepository
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Test class for ChatStorageFactory
 * 
 * This class tests the behavior of the ChatStorageFactory object, focusing on:
 * 1. The lazy initialization of the repository property
 * 2. The getChatRepository() method returning the expected repository
 */
class ChatStorageFactoryTest {

    private val mockRepository = mockk<ChatRepository>()

    @BeforeEach
    fun setUp() {
        mockkObject(DatabaseManager)
        every { DatabaseManager.externalInit() } just runs
        every { DatabaseManager.chatRepository() } returns mockRepository
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    /**
     * Test that verifies the behavior of ChatStorageFactory
     * 
     * This test covers both the initialization and caching behavior in a single test
     * to ensure that the state is consistent throughout the test.
     */
    @Test
    fun `test ChatStorageFactory initialization and caching behavior`() {
        mockkObject(ChatStorageFactory)
        
        val repository1 = ChatStorageFactory.getChatRepository()
        
        verify(exactly = 1) { DatabaseManager.externalInit() }
        verify(exactly = 1) { DatabaseManager.chatRepository() }
        
        assertEquals(mockRepository, repository1)
        
        clearMocks(DatabaseManager, verificationMarks = true)
        
        val repository2 = ChatStorageFactory.getChatRepository()
        
        verify(exactly = 0) { DatabaseManager.externalInit() }
        verify(exactly = 0) { DatabaseManager.chatRepository() }
        
        assertSame(repository1, repository2)
    }
}