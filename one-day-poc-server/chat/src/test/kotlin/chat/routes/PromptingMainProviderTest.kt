package chat.routes

import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import prompting.PromptingMain
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class PromptingMainProviderTest {
    @AfterEach
    fun tearDown() {
        PromptingMainProvider.resetInstance()
        unmockkAll()
    }

    @Test
    fun `Test getInstance when instance is not initialized`() {
        PromptingMainProvider.getInstance()
    }

    @Test
    fun `Test getInstance when instance is already initialized`() {
        val firstInstance = PromptingMainProvider.getInstance()
        val secondInstance = PromptingMainProvider.getInstance()
        assertEquals(firstInstance, secondInstance)
    }

    @Test
    fun `Test setInstance with custom instance`() {
        val customInstance = mockk<PromptingMain>()
        PromptingMainProvider.setInstance(customInstance)
        val retrievedInstance = PromptingMainProvider.getInstance()
        assertEquals(customInstance, retrievedInstance)
    }

    @Test
    fun `Test resetInstance`() {
        val customInstance = mockk<PromptingMain>()
        PromptingMainProvider.setInstance(customInstance)

        PromptingMainProvider.resetInstance()
        val newInstance = PromptingMainProvider.getInstance()

        assertNotSame(customInstance, newInstance)
    }

    @Test
    fun `Test getInstance returns same instance after multiple calls`() {
        val instance1 = PromptingMainProvider.getInstance()

        val instance2 = PromptingMainProvider.getInstance()
        val instance3 = PromptingMainProvider.getInstance()

        assertEquals(instance1, instance2)
        assertEquals(instance1, instance3)
    }

    @Test
    fun `Test setInstance followed by getInstance returns the set instance`() {
        val customInstance = mockk<PromptingMain>()

        PromptingMainProvider.setInstance(customInstance)
        val instance1 = PromptingMainProvider.getInstance()
        val instance2 = PromptingMainProvider.getInstance()

        assertEquals(customInstance, instance1)
        assertEquals(customInstance, instance2)
    }
}
