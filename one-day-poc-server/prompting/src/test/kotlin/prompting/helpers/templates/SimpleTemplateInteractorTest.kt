package prompting.helpers.templates

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SimpleTemplateInteractorTest {
    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(TemplateInteractor)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test fetchTemplates returns templates for a prompt`() =
        runBlocking {
            val prompt = "test prompt"
            val expectedTemplates = listOf("Template 1", "Template 2")

            coEvery {
                TemplateInteractor.fetchTemplates(prompt)
            } returns expectedTemplates

            val result = TemplateInteractor.fetchTemplates(prompt)
            assertEquals(expectedTemplates, result)
            coVerify {
                TemplateInteractor.fetchTemplates(prompt)
            }
        }

    @Test
    fun `test fetchTemplates returns empty list when no templates match`() =
        runBlocking {
            val prompt = "test prompt with no matches"
            coEvery {
                TemplateInteractor.fetchTemplates(prompt)
            } returns emptyList()

            val result = TemplateInteractor.fetchTemplates(prompt)
            assertTrue(result.isEmpty())
            coVerify {
                TemplateInteractor.fetchTemplates(prompt)
            }
        }

    @Test
    fun `test storeNewTemplate successfully stores template and metadata`() =
        runBlocking {
            val templateID = "123e4567-e89b-12d3-a456-426614174000"
            val templateCode = "Template code"
            val jsonLD = "{\"@context\": \"https://schema.org\", \"@type\": \"SoftwareSourceCode\"}"

            coEvery {
                TemplateInteractor.storeNewTemplate(templateID, templateCode, jsonLD)
            } returns true

            val result = TemplateInteractor.storeNewTemplate(templateID, templateCode, jsonLD)
            assertTrue(result)
            coVerify {
                TemplateInteractor.storeNewTemplate(templateID, templateCode, jsonLD)
            }
        }

    @Test
    fun `test storeNewTemplate returns false when storage fails`() =
        runBlocking {
            val templateID = "123e4567-e89b-12d3-a456-426614174000"
            val templateCode = "Template code"
            val jsonLD = "{\"@context\": \"https://schema.org\", \"@type\": \"SoftwareSourceCode\"}"

            coEvery {
                TemplateInteractor.storeNewTemplate(templateID, templateCode, jsonLD)
            } returns false

            val result = TemplateInteractor.storeNewTemplate(templateID, templateCode, jsonLD)
            assertFalse(result)
            coVerify {
                TemplateInteractor.storeNewTemplate(templateID, templateCode, jsonLD)
            }
        }
}
