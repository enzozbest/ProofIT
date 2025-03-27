package chat.routes

import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import prompting.PromptingMain
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class PromptingMainProviderTest {
    
    @AfterEach
    fun tearDown() {
        // Reset the instance after each test to ensure tests don't affect each other
        PromptingMainProvider.resetInstance()
    }
    
    @Test
    fun `Test getInstance when instance is not initialized`() {
        // Given
        // No instance is initialized
        
        // When
        val instance = PromptingMainProvider.getInstance()
        
        // Then
        assertTrue(instance is PromptingMain)
    }
    
    @Test
    fun `Test getInstance when instance is already initialized`() {
        // Given
        val firstInstance = PromptingMainProvider.getInstance()
        
        // When
        val secondInstance = PromptingMainProvider.getInstance()
        
        // Then
        assertEquals(firstInstance, secondInstance)
    }
    
    @Test
    fun `Test setInstance with custom instance`() {
        // Given
        val customInstance = mockk<PromptingMain>()
        
        // When
        PromptingMainProvider.setInstance(customInstance)
        val retrievedInstance = PromptingMainProvider.getInstance()
        
        // Then
        assertEquals(customInstance, retrievedInstance)
    }
    
    @Test
    fun `Test resetInstance`() {
        // Given
        val customInstance = mockk<PromptingMain>()
        PromptingMainProvider.setInstance(customInstance)
        
        // When
        PromptingMainProvider.resetInstance()
        val newInstance = PromptingMainProvider.getInstance()
        
        // Then
        assertNotSame(customInstance, newInstance)
        assertTrue(newInstance is PromptingMain)
    }
    
    @Test
    fun `Test getInstance returns same instance after multiple calls`() {
        // Given
        val instance1 = PromptingMainProvider.getInstance()
        
        // When
        val instance2 = PromptingMainProvider.getInstance()
        val instance3 = PromptingMainProvider.getInstance()
        
        // Then
        assertEquals(instance1, instance2)
        assertEquals(instance1, instance3)
    }
    
    @Test
    fun `Test setInstance followed by getInstance returns the set instance`() {
        // Given
        val customInstance = mockk<PromptingMain>()
        
        // When
        PromptingMainProvider.setInstance(customInstance)
        val instance1 = PromptingMainProvider.getInstance()
        val instance2 = PromptingMainProvider.getInstance()
        
        // Then
        assertEquals(customInstance, instance1)
        assertEquals(customInstance, instance2)
    }
}