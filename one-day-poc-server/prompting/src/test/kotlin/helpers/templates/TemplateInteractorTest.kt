package kcl.seg.rtt.prompting.helpers.templates

import TemplateService
import TemplateStorageService
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.UUID
import TemplateEmbedResponse
import TemplateSearchResponse

class TemplateInteractorTest {

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(TemplateService)
        mockkObject(TemplateStorageService)
        mockkObject(TemplateStorageUtils)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test fetchTemplates returns content for matching templates`() = runBlocking {
        // Arrange
        val prompt = "test prompt"
        val embedding = listOf(0.1f, 0.2f, 0.3f)
        val templateIds = listOf("123e4567-e89b-12d3-a456-426614174000", "223e4567-e89b-12d3-a456-426614174000")
        val templateContent1 = "Template content 1"
        val templateContent2 = "Template content 2"

        // Create mock response objects
        val embedResponse = mockk<TemplateEmbedResponse>()
        every { embedResponse.embedding } returns embedding

        val searchResponse = mockk<TemplateSearchResponse>()
        every { searchResponse.matches } returns templateIds

        // Mock TemplateService.embed
        coEvery { 
            TemplateService.embed(prompt, "prompt") 
        } returns embedResponse

        // Mock TemplateService.search
        coEvery { 
            TemplateService.search(embedding, prompt) 
        } returns searchResponse

        // Mock TemplateStorageService.getTemplateById for first template
        coEvery { 
            TemplateStorageService.getTemplateById(UUID.fromString(templateIds[0])) 
        } returns mockk {
            every { fileURI } returns "template1.txt"
        }

        // Mock TemplateStorageService.getTemplateById for second template
        coEvery { 
            TemplateStorageService.getTemplateById(UUID.fromString(templateIds[1])) 
        } returns mockk {
            every { fileURI } returns "template2.txt"
        }

        // Mock TemplateStorageUtils.retrieveFileContent
        coEvery { 
            TemplateStorageUtils.retrieveFileContent("template1.txt") 
        } returns templateContent1.toByteArray()

        coEvery { 
            TemplateStorageUtils.retrieveFileContent("template2.txt") 
        } returns templateContent2.toByteArray()

        // Act
        val result = TemplateInteractor.fetchTemplates(prompt)

        // Assert
        assertEquals(2, result.size)
        assertEquals(templateContent1, result[0])
        assertEquals(templateContent2, result[1])

        // Verify
        coVerify { 
            TemplateService.embed(prompt, "prompt")
            TemplateService.search(embedding, prompt)
            TemplateStorageService.getTemplateById(UUID.fromString(templateIds[0]))
            TemplateStorageService.getTemplateById(UUID.fromString(templateIds[1]))
            TemplateStorageUtils.retrieveFileContent("template1.txt")
            TemplateStorageUtils.retrieveFileContent("template2.txt")
        }
    }

    @Test
    fun `test fetchTemplates returns empty list when no templates match`() = runBlocking {
        // Arrange
        val prompt = "test prompt"
        val embedding = listOf(0.1f, 0.2f, 0.3f)

        // Create mock response objects
        val embedResponse = mockk<TemplateEmbedResponse>()
        every { embedResponse.embedding } returns embedding

        val searchResponse = mockk<TemplateSearchResponse>()
        every { searchResponse.matches } returns emptyList()

        // Mock TemplateService.embed
        coEvery { 
            TemplateService.embed(prompt, "prompt") 
        } returns embedResponse

        // Mock TemplateService.search with empty matches
        coEvery { 
            TemplateService.search(embedding, prompt) 
        } returns searchResponse

        // Act
        val result = TemplateInteractor.fetchTemplates(prompt)

        // Assert
        assertTrue(result.isEmpty())

        // Verify
        coVerify { 
            TemplateService.embed(prompt, "prompt")
            TemplateService.search(embedding, prompt)
        }
        coVerify(exactly = 0) { 
            TemplateStorageService.getTemplateById(any())
            TemplateStorageUtils.retrieveFileContent(any())
        }
    }

    @Test
    fun `test storeNewTemplate successfully stores template and metadata`() = runBlocking {
        // Arrange
        val templateID = "123e4567-e89b-12d3-a456-426614174000"
        val templateCode = "Template code"
        val jsonLD = "{\"@context\": \"https://schema.org\", \"@type\": \"SoftwareSourceCode\"}"
        val templateFilePath = "templates/123e4567-e89b-12d3-a456-426614174000.templ"
        val jsonLDFilePath = "templates/metadata/jsonld_123e4567-e89b-12d3-a456-426614174000.json"

        // Mock environment loader
        mockkObject(kcl.seg.rtt.utils.environment.EnvironmentLoader)
        every { 
            kcl.seg.rtt.utils.environment.EnvironmentLoader.get("S3_BUCKET_TEMPLATES") 
        } returns "test-bucket"

        // Mock TemplateStorageUtils.storeFile for template
        coEvery { 
            TemplateStorageUtils.storeFile(
                content = templateCode,
                filePrefix = "template_${templateID}_",
                fileSuffix = ".txt",
                storageConfig = any()
            ) 
        } returns templateFilePath

        // Mock TemplateStorageUtils.storeFile for JSON-LD
        coEvery { 
            TemplateStorageUtils.storeFile(
                content = jsonLD,
                filePrefix = "jsonld_${templateID}_",
                fileSuffix = ".json",
                storageConfig = any()
            ) 
        } returns jsonLDFilePath

        // Mock TemplateStorageService.createTemplate
        coEvery { 
            TemplateStorageService.createTemplate(templateFilePath) 
        } returns templateID

        // Mock TemplateService.storeTemplate
        coEvery { 
            TemplateService.storeTemplate(templateID, templateFilePath, jsonLD) 
        } returns mockk {
            every { status } returns "success"
        }

        // Act
        val result = TemplateInteractor.storeNewTemplate(templateID, templateCode, jsonLD)

        // Assert
        assertTrue(result)

        // Verify
        coVerify { 
            TemplateStorageUtils.storeFile(
                content = templateCode,
                filePrefix = "template_${templateID}_",
                fileSuffix = ".txt",
                storageConfig = any()
            )
            TemplateStorageUtils.storeFile(
                content = jsonLD,
                filePrefix = "jsonld_${templateID}_",
                fileSuffix = ".json",
                storageConfig = any()
            )
            TemplateStorageService.createTemplate(templateFilePath)
            TemplateService.storeTemplate(templateID, templateFilePath, jsonLD)
        }

    }
}
