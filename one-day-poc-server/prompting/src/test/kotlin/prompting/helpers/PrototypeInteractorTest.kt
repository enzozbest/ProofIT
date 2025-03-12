package kcl.seg.rtt.prompting.helpers.helpers

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkConstructor
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

            coEvery { anyConstructed<PrototypeMain>().prompt(any()) } returns expectedResponse
            val result = PrototypeInteractor.prompt(testPrompt, testModel)
            assertEquals(expectedResponse, result)
            coVerify { anyConstructed<PrototypeMain>().prompt(testPrompt) }
        }

    @Test
    fun `Test prompt call with null response`() =
        runBlocking {
            val testPrompt = "test prompt"
            val testModel = "llama2"

            coEvery { anyConstructed<PrototypeMain>().prompt(any()) } returns null

            val result = PrototypeInteractor.prompt(testPrompt, testModel)

            assertNull(result)
            coVerify { anyConstructed<PrototypeMain>().prompt(testPrompt) }
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

            coEvery { anyConstructed<PrototypeMain>().prompt(any()) } returns expectedResponse

            val result = PrototypeInteractor.prompt(testPrompt, testModel)

            assertEquals(expectedResponse, result)
            coVerify { anyConstructed<PrototypeMain>().prompt(testPrompt) }
        }
}
