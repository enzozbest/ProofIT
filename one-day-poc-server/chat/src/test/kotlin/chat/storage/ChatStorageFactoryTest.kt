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
        // Get the repository for the first time
        val repository1 = ChatStorageFactory.getChatRepository()

        // Verify that the initialization methods were called exactly once
        verify(exactly = 1) { DatabaseManager.externalInit() }
        verify(exactly = 1) { DatabaseManager.chatRepository() }

        // Verify that the repository is the mock repository we set up
        assertEquals(mockRepository, repository1)

        // Clear verification marks to reset the call count
        clearMocks(DatabaseManager, verificationMarks = true)

        // Get the repository again
        val repository2 = ChatStorageFactory.getChatRepository()

        // Verify that the initialization methods were not called again
        verify(exactly = 0) { DatabaseManager.externalInit() }
        verify(exactly = 0) { DatabaseManager.chatRepository() }

        // Verify that both calls return the same instance (lazy initialization caching)
        assertSame(repository1, repository2)
    }
}
