package kcl.seg.rtt.prompting.helpers.templates

import embeddings.TemplateEmbedResponse
import embeddings.TemplateSearchResponse
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import prompting.helpers.templates.TemplateInteractor
import prompting.helpers.templates.TemplateStorageUtils
import templates.TemplateService
import templates.TemplateStorageService
import utils.environment.EnvironmentLoader

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
    fun `test fetchTemplates returns content for matching templates`() =
        runBlocking {
            val prompt = "test prompt"
            val embedding = listOf(0.1f, 0.2f, 0.3f)
            val templateIds = listOf("123e4567-e89b-12d3-a456-426614174000", "223e4567-e89b-12d3-a456-426614174000")
            val templateContent1 = "Template content 1"
            val templateContent2 = "Template content 2"

            val embedResponse = mockk<TemplateEmbedResponse>()
            every { embedResponse.embedding } returns embedding

            val searchResponse = mockk<TemplateSearchResponse>()
            every { searchResponse.matches } returns templateIds

            coEvery {
                TemplateService.embed(prompt, "prompt")
            } returns embedResponse

            coEvery {
                TemplateService.search(embedding, prompt)
            } returns searchResponse

            coEvery {
                TemplateStorageService.getTemplateById(templateIds[0])
            } returns
                mockk {
                    every { fileURI } returns "template1.txt"
                }

            coEvery {
                TemplateStorageService.getTemplateById(templateIds[1])
            } returns
                mockk {
                    every { fileURI } returns "template2.txt"
                }

            coEvery {
                TemplateStorageUtils.retrieveFileContent("template1.txt")
            } returns templateContent1.toByteArray()

            coEvery {
                TemplateStorageUtils.retrieveFileContent("template2.txt")
            } returns templateContent2.toByteArray()

            val result = TemplateInteractor.fetchTemplates(prompt)
            assertEquals(2, result.size)
            assertEquals(templateContent1, result[0])
            assertEquals(templateContent2, result[1])
            coVerify {
                TemplateService.embed(prompt, "prompt")
                TemplateService.search(embedding, prompt)
                TemplateStorageService.getTemplateById(templateIds[0])
                TemplateStorageService.getTemplateById(templateIds[1])
                TemplateStorageUtils.retrieveFileContent("template1.txt")
                TemplateStorageUtils.retrieveFileContent("template2.txt")
            }
        }

    @Test
    fun `test fetchTemplates returns empty list when no templates match`() =
        runBlocking {
            val prompt = "test prompt"
            val embedding = listOf(0.1f, 0.2f, 0.3f)

            val embedResponse = mockk<TemplateEmbedResponse>()
            every { embedResponse.embedding } returns embedding

            val searchResponse = mockk<TemplateSearchResponse>()
            every { searchResponse.matches } returns emptyList()

            coEvery {
                TemplateService.embed(prompt, "prompt")
            } returns embedResponse

            coEvery {
                TemplateService.search(embedding, prompt)
            } returns searchResponse

            val result = TemplateInteractor.fetchTemplates(prompt)

            assertTrue(result.isEmpty())

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
    fun `test storeNewTemplate successfully stores template and metadata`() =
        runBlocking {
            val templateID = "123e4567-e89b-12d3-a456-426614174000"
            val templateCode = "Template code"
            val jsonLD = "{\"@context\": \"https://schema.org\", \"@type\": \"SoftwareSourceCode\"}"
            val templateFilePath = "templates/123e4567-e89b-12d3-a456-426614174000.templ"
            val jsonLDFilePath = "templates/metadata/jsonld_123e4567-e89b-12d3-a456-426614174000.json"

            mockkObject(EnvironmentLoader)
            every {
                EnvironmentLoader
                    .get("S3_BUCKET_TEMPLATES")
            } returns "test-bucket"

            coEvery {
                TemplateStorageUtils.storeFile(
                    content = templateCode,
                    filePrefix = "template_${templateID}_",
                    fileSuffix = ".txt",
                    storageConfig = any(),
                )
            } returns templateFilePath

            coEvery {
                TemplateStorageUtils.storeFile(
                    content = jsonLD,
                    filePrefix = "jsonld_${templateID}_",
                    fileSuffix = ".json",
                    storageConfig = any(),
                )
            } returns jsonLDFilePath

            coEvery {
                TemplateStorageService.createTemplate(templateFilePath)
            } returns templateID

            coEvery {
                TemplateService.storeTemplate(templateID, templateFilePath)
            } returns
                mockk {
                    every { status } returns "success"
                }

            val result = TemplateInteractor.storeNewTemplate(templateID, templateCode, jsonLD)
            assertTrue(result)
            coVerify {
                TemplateStorageUtils.storeFile(
                    content = templateCode,
                    filePrefix = "template_${templateID}_",
                    fileSuffix = ".txt",
                    storageConfig = any(),
                )
                TemplateStorageUtils.storeFile(
                    content = jsonLD,
                    filePrefix = "jsonld_${templateID}_",
                    fileSuffix = ".json",
                    storageConfig = any(),
                )
                TemplateStorageService.createTemplate(templateFilePath)
                TemplateService.storeTemplate(templateID, templateFilePath)
            }
        }

    @Test
    fun `test fetchTemplates when embed fails returns empty list`() =
        runBlocking {
            val prompt = "test prompt"

            coEvery {
                TemplateService.embed(prompt, "prompt")
            } throws RuntimeException("Embedding failed")

            val result = TemplateInteractor.fetchTemplates(prompt)

            assertTrue(result.isEmpty())

            coVerify {
                TemplateService.embed(prompt, "prompt")
            }
        }

    @Test
    fun `test fetchTemplates when search fails returns empty list`() =
        runBlocking {
            val prompt = "test prompt"
            val embedding = listOf(0.1f, 0.2f, 0.3f)

            val embedResponse = mockk<TemplateEmbedResponse>()
            every { embedResponse.embedding } returns embedding

            coEvery {
                TemplateService.embed(prompt, "prompt")
            } returns embedResponse

            coEvery {
                TemplateService.search(embedding, prompt)
            } throws RuntimeException("Search failed")

            val result = TemplateInteractor.fetchTemplates(prompt)

            assertTrue(result.isEmpty())

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
    fun `test fetchTemplates when template not found returns empty list`() =
        runBlocking {
            val prompt = "test prompt"
            val embedding = listOf(0.1f, 0.2f, 0.3f)
            val templateId = "123e4567-e89b-12d3-a456-426614174000"

            val embedResponse = mockk<TemplateEmbedResponse>()
            every { embedResponse.embedding } returns embedding

            val searchResponse = mockk<TemplateSearchResponse>()
            every { searchResponse.matches } returns listOf(templateId)

            coEvery {
                TemplateService.embed(prompt, "prompt")
            } returns embedResponse

            coEvery {
                TemplateService.search(embedding, prompt)
            } returns searchResponse

            coEvery {
                TemplateStorageService.getTemplateById(templateId)
            } returns null

            val result = TemplateInteractor.fetchTemplates(prompt)

            assertTrue(result.isEmpty())

            coVerify {
                TemplateService.embed(prompt, "prompt")
                TemplateService.search(embedding, prompt)
                TemplateStorageService.getTemplateById(templateId)
            }
            coVerify(exactly = 0) {
                TemplateStorageUtils.retrieveFileContent(any())
            }
        }

    @Test
    fun `test fetchTemplates when template not found by ID returns empty list`() =
        runBlocking {
            val prompt = "test prompt"
            val embedding = listOf(0.1f, 0.2f, 0.3f)
            val templateId = "123e4567-e89b-12d3-a456-426614174000"

            val embedResponse = mockk<TemplateEmbedResponse>()
            every { embedResponse.embedding } returns embedding

            val searchResponse = mockk<TemplateSearchResponse>()
            every { searchResponse.matches } returns listOf(templateId)

            coEvery {
                TemplateService.embed(prompt, "prompt")
            } returns embedResponse

            coEvery {
                TemplateService.search(embedding, prompt)
            } returns searchResponse

            coEvery {
                TemplateStorageService.getTemplateById(templateId)
            } returns null

            val result = TemplateInteractor.fetchTemplates(prompt)

            assertTrue(result.isEmpty())

            coVerify {
                TemplateService.embed(prompt, "prompt")
                TemplateService.search(embedding, prompt)
                TemplateStorageService.getTemplateById(templateId)
            }
            coVerify(exactly = 0) {
                TemplateStorageUtils.retrieveFileContent(any())
            }
        }

    @Test
    fun `test storeNewTemplate when template file storage fails returns false`() =
        runBlocking {
            val templateID = "123e4567-e89b-12d3-a456-426614174000"
            val templateCode = "Template code"
            val jsonLD = "{\"@context\": \"https://schema.org\", \"@type\": \"SoftwareSourceCode\"}"

            mockkObject(EnvironmentLoader)
            every {
                EnvironmentLoader
                    .get("S3_BUCKET_TEMPLATES")
            } returns "test-bucket"

            coEvery {
                TemplateStorageUtils.storeFile(
                    content = templateCode,
                    filePrefix = "template_${templateID}_",
                    fileSuffix = ".txt",
                    storageConfig = any(),
                )
            } returns "" // Empty path indicates failure

            val result = TemplateInteractor.storeNewTemplate(templateID, templateCode, jsonLD)

            assertFalse(result)

            coVerify {
                TemplateStorageUtils.storeFile(
                    content = templateCode,
                    filePrefix = "template_${templateID}_",
                    fileSuffix = ".txt",
                    storageConfig = any(),
                )
            }
            coVerify(exactly = 0) {
                TemplateStorageUtils.storeFile(
                    content = jsonLD,
                    filePrefix = "jsonld_${templateID}_",
                    fileSuffix = ".json",
                    storageConfig = any(),
                )
                TemplateStorageService.createTemplate(any())
                TemplateService.storeTemplate(any(), any())
            }
        }

    @Test
    fun `test storeNewTemplate when jsonLD file storage fails returns false`() =
        runBlocking {
            val templateID = "123e4567-e89b-12d3-a456-426614174000"
            val templateCode = "Template code"
            val jsonLD = "{\"@context\": \"https://schema.org\", \"@type\": \"SoftwareSourceCode\"}"
            val templateFilePath = "templates/123e4567-e89b-12d3-a456-426614174000.templ"

            mockkObject(EnvironmentLoader)
            every {
                EnvironmentLoader
                    .get("S3_BUCKET_TEMPLATES")
            } returns "test-bucket"

            coEvery {
                TemplateStorageUtils.storeFile(
                    content = templateCode,
                    filePrefix = "template_${templateID}_",
                    fileSuffix = ".txt",
                    storageConfig = any(),
                )
            } returns templateFilePath

            coEvery {
                TemplateStorageUtils.storeFile(
                    content = jsonLD,
                    filePrefix = "jsonld_${templateID}_",
                    fileSuffix = ".json",
                    storageConfig = any(),
                )
            } returns "" // Empty path indicates failure

            val result = TemplateInteractor.storeNewTemplate(templateID, templateCode, jsonLD)

            assertFalse(result)

            coVerify {
                TemplateStorageUtils.storeFile(
                    content = templateCode,
                    filePrefix = "template_${templateID}_",
                    fileSuffix = ".txt",
                    storageConfig = any(),
                )
                TemplateStorageUtils.storeFile(
                    content = jsonLD,
                    filePrefix = "jsonld_${templateID}_",
                    fileSuffix = ".json",
                    storageConfig = any(),
                )
            }
            coVerify(exactly = 0) {
                TemplateStorageService.createTemplate(any())
                TemplateService.storeTemplate(any(), any())
            }
        }

    @Test
    fun `test storeNewTemplate when template creation fails returns false`() =
        runBlocking {
            val templateID = "123e4567-e89b-12d3-a456-426614174000"
            val templateCode = "Template code"
            val jsonLD = "{\"@context\": \"https://schema.org\", \"@type\": \"SoftwareSourceCode\"}"
            val templateFilePath = "templates/123e4567-e89b-12d3-a456-426614174000.templ"
            val jsonLDFilePath = "templates/metadata/jsonld_123e4567-e89b-12d3-a456-426614174000.json"

            mockkObject(EnvironmentLoader)
            every {
                EnvironmentLoader
                    .get("S3_BUCKET_TEMPLATES")
            } returns "test-bucket"

            coEvery {
                TemplateStorageUtils.storeFile(
                    content = templateCode,
                    filePrefix = "template_${templateID}_",
                    fileSuffix = ".txt",
                    storageConfig = any(),
                )
            } returns templateFilePath

            coEvery {
                TemplateStorageUtils.storeFile(
                    content = jsonLD,
                    filePrefix = "jsonld_${templateID}_",
                    fileSuffix = ".json",
                    storageConfig = any(),
                )
            } returns jsonLDFilePath

            coEvery {
                TemplateStorageService.createTemplate(templateFilePath)
            } returns null // Null ID indicates failure

            val result = TemplateInteractor.storeNewTemplate(templateID, templateCode, jsonLD)

            assertFalse(result)

            coVerify {
                TemplateStorageUtils.storeFile(
                    content = templateCode,
                    filePrefix = "template_${templateID}_",
                    fileSuffix = ".txt",
                    storageConfig = any(),
                )
                TemplateStorageUtils.storeFile(
                    content = jsonLD,
                    filePrefix = "jsonld_${templateID}_",
                    fileSuffix = ".json",
                    storageConfig = any(),
                )
                TemplateStorageService.createTemplate(templateFilePath)
            }
            coVerify(exactly = 0) {
                TemplateService.storeTemplate(any(), any())
            }
        }

    @Test
    fun `test storeNewTemplate when template service storage fails returns false`() =
        runBlocking {
            val templateID = "123e4567-e89b-12d3-a456-426614174000"
            val templateCode = "Template code"
            val jsonLD = "{\"@context\": \"https://schema.org\", \"@type\": \"SoftwareSourceCode\"}"
            val templateFilePath = "templates/123e4567-e89b-12d3-a456-426614174000.templ"
            val jsonLDFilePath = "templates/metadata/jsonld_123e4567-e89b-12d3-a456-426614174000.json"

            mockkObject(EnvironmentLoader)
            every {
                EnvironmentLoader
                    .get("S3_BUCKET_TEMPLATES")
            } returns "test-bucket"

            coEvery {
                TemplateStorageUtils.storeFile(
                    content = templateCode,
                    filePrefix = "template_${templateID}_",
                    fileSuffix = ".txt",
                    storageConfig = any(),
                )
            } returns templateFilePath

            coEvery {
                TemplateStorageUtils.storeFile(
                    content = jsonLD,
                    filePrefix = "jsonld_${templateID}_",
                    fileSuffix = ".json",
                    storageConfig = any(),
                )
            } returns jsonLDFilePath

            coEvery {
                TemplateStorageService.createTemplate(templateFilePath)
            } returns templateID

            coEvery {
                TemplateService.storeTemplate(templateID, templateFilePath)
            } returns
                mockk {
                    every { status } returns "error"
                }

            val result = TemplateInteractor.storeNewTemplate(templateID, templateCode, jsonLD)

            assertFalse(result)

            coVerify {
                TemplateStorageUtils.storeFile(
                    content = templateCode,
                    filePrefix = "template_${templateID}_",
                    fileSuffix = ".txt",
                    storageConfig = any(),
                )
                TemplateStorageUtils.storeFile(
                    content = jsonLD,
                    filePrefix = "jsonld_${templateID}_",
                    fileSuffix = ".json",
                    storageConfig = any(),
                )
                TemplateStorageService.createTemplate(templateFilePath)
                TemplateService.storeTemplate(templateID, templateFilePath)
            }
        }

    @Test
    fun `test storeNewTemplate when exception is thrown returns false`() =
        runBlocking {
            val templateID = "123e4567-e89b-12d3-a456-426614174000"
            val templateCode = "Template code"
            val jsonLD = "{\"@context\": \"https://schema.org\", \"@type\": \"SoftwareSourceCode\"}"

            mockkObject(EnvironmentLoader)
            every {
                EnvironmentLoader
                    .get("S3_BUCKET_TEMPLATES")
            } throws RuntimeException("Environment variable not found")

            val result = TemplateInteractor.storeNewTemplate(templateID, templateCode, jsonLD)
            assertFalse(result)
            verify {
                EnvironmentLoader.get("S3_BUCKET_TEMPLATES")
            }

            coVerify(exactly = 0) {
                TemplateStorageUtils.storeFile(any(), any(), any(), any())
                TemplateStorageService.createTemplate(any())
                TemplateService.storeTemplate(any(), any())
            }
        }
}
