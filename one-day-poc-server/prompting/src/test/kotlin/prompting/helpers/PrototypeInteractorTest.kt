package kcl.seg.rtt.prompting.helpers.helpers

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import prompting.helpers.PrototypeInteractor
import prototype.PrototypeMain
import prototype.helpers.OllamaResponse
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PrototypeInteractorTest {
    @BeforeEach
    fun setUp() {
        mockkConstructor(PrototypeMain::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Test successful prompt call`() =
        runBlocking {
            val testPrompt = "test prompt"
            val testModel = "llama2"
            val expectedResponse =
                OllamaResponse(
                    model = testModel,
                    created_at = "2024-01-01T00:00:00Z",
                    response = "test response",
                    done = true,
                    done_reason = "stop",
                )

            // Use a more explicit mocking approach
            coEvery { 
                anyConstructed<PrototypeMain>().prompt(eq(testPrompt)) 
            } returns expectedResponse

            val result = PrototypeInteractor.prompt(testPrompt, testModel)
            assertEquals(expectedResponse, result)
            coVerify(exactly = 1) { anyConstructed<PrototypeMain>().prompt(testPrompt) }
        }

    @Test
    fun `Test prompt call with null response`() =
        runBlocking {
            val testPrompt = "test prompt"
            val testModel = "llama2"

            // Use a more explicit mocking approach
            coEvery { 
                anyConstructed<PrototypeMain>().prompt(eq(testPrompt)) 
            } returns null

            val result = PrototypeInteractor.prompt(testPrompt, testModel)

            assertNull(result)
            coVerify(exactly = 1) { anyConstructed<PrototypeMain>().prompt(testPrompt) }
        }

    @Test
    fun `Test prompt call with different models`() =
        runBlocking {
            val testPrompt = "test prompt"
            val testModel = "gpt4"
            val expectedResponse =
                OllamaResponse(
                    model = testModel,
                    created_at = "2024-01-01T00:00:00Z",
                    response = "test response",
                    done = true,
                    done_reason = "stop",
                )

            // Use a more explicit mocking approach
            coEvery { 
                anyConstructed<PrototypeMain>().prompt(eq(testPrompt)) 
            } returns expectedResponse

            val result = PrototypeInteractor.prompt(testPrompt, testModel)

            assertEquals(expectedResponse, result)
            coVerify(exactly = 1) { anyConstructed<PrototypeMain>().prompt(testPrompt) }
        }
}
