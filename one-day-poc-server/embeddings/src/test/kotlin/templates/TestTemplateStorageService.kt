package templates

import database.core.DatabaseManager
import database.tables.templates.Template
import database.tables.templates.TemplateRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import java.util.*

class TestTemplateStorageService {
    private lateinit var templateRepository: TemplateRepository
    private lateinit var mockLogger: Logger
    private lateinit var originalLogger: Logger

    @BeforeEach
    fun setUp() {
        mockkObject(DatabaseManager)
        templateRepository = mockk()
        coEvery { DatabaseManager.templateRepository() } returns templateRepository
        originalLogger = TemplateStorageService.logger
        mockLogger = mockk<Logger>(relaxed = true)
        TemplateStorageService.logger = mockLogger
    }

    @AfterEach
    fun tearDown() {
        TemplateStorageService.logger = originalLogger
        unmockkAll()
    }

    @Test
    fun `createTemplate returns template id when successful`() =
        runBlocking {
            val fileURI = "file:///templates/sample.json"

            unmockkAll()
            mockkObject(DatabaseManager)

            val mockRepo = mockk<TemplateRepository>()
            coEvery { DatabaseManager.templateRepository() } returns mockRepo

            coEvery {
                mockRepo.saveTemplateToDB(any())
            } returns Result.success(Unit)

            val templateId = TemplateStorageService.createTemplate(fileURI)

            assert(templateId?.isNotEmpty() == true)
        }

    @Test
    fun `createTemplate handles repository failure`() =
        runBlocking {
            val fileURI = "file:///templates/sample-failure.json"

            // Add debug logging
            println("[DEBUG_LOG] Starting createTemplate handles repository failure test")

            // Create a fresh mock for each test
            unmockkAll()

            // Mock the TemplateStorageService.logger to avoid issues with the real logger
            val mockLogger = mockk<Logger>(relaxed = true)
            val originalLogger = TemplateStorageService.logger
            TemplateStorageService.logger = mockLogger

            try {
                // Mock the DatabaseManager
                mockkObject(DatabaseManager)

                // Create a strict mock for the repository
                val mockRepo = mockk<TemplateRepository>(relaxed = false)

                // Mock the templateRepository method to return our mock repository
                coEvery { DatabaseManager.templateRepository() } returns mockRepo

                // Mock the saveTemplateToDB method to throw an exception
                coEvery { 
                    mockRepo.saveTemplateToDB(any()) 
                } answers { 
                    println("[DEBUG_LOG] Mock throwing exception")
                    throw RuntimeException("Database operation failed")
                }

                println("[DEBUG_LOG] Mocks set up, calling createTemplate")

                // Call the function under test
                val templateId = TemplateStorageService.createTemplate(fileURI)

                println("[DEBUG_LOG] createTemplate returned: $templateId")

                // Assert that the function returns null when the repository operation fails
                assertNull(templateId)
            } finally {
                // Restore the original logger
                TemplateStorageService.logger = originalLogger
                unmockkAll()
            }
        }

    @Test
    fun `createTemplate handles repository exception`() =
        runBlocking {
            val fileURI = "file:///templates/sample-exception.json"

            val mockRepo = mockk<TemplateRepository>()

            unmockkAll()

            mockkObject(DatabaseManager)
            coEvery { DatabaseManager.templateRepository() } returns mockRepo

            coEvery {
                mockRepo.saveTemplateToDB(any())
            } throws Exception("Database connection error")

            val templateId = TemplateStorageService.createTemplate(fileURI)

            assertNull(templateId)
        }

    @Test
    fun `createTemplate handles DatabaseManager templateRepository exception when database not initialized`() =
        runBlocking {
            val fileURI = "file:///templates/sample-db-not-initialized.json"

            // Create a fresh mock for each test
            unmockkAll()

            // Mock the DatabaseManager
            mockkObject(DatabaseManager)

            // Mock the templateRepository method to throw an IllegalStateException
            coEvery { 
                DatabaseManager.templateRepository() 
            } throws IllegalStateException("Database connection not initialized. Call init() first.")

            // Call the function under test
            val templateId = TemplateStorageService.createTemplate(fileURI)

            // Assert that the function returns null when DatabaseManager.templateRepository() throws an exception
            assertNull(templateId)
        }

    @Test
    fun `getTemplateById should return template when successfully retrieving template`() =
        runBlocking {
            val templateId = UUID.randomUUID()
            val expectedTemplate = Template(id = templateId.toString(), fileURI = "file:///templates/sample.json")

            coEvery {
                templateRepository.getTemplateFromDB(templateId.toString())
            } returns expectedTemplate

            val result = TemplateStorageService.getTemplateById(templateId)

            assertEquals(expectedTemplate, result)
        }

    @Test
    fun `getTemplateById should return null when exception is thrown`() =
        runBlocking {
            val templateId = UUID.randomUUID()

            coEvery {
                templateRepository.getTemplateFromDB(templateId.toString())
            } throws Exception("Database error")

            val result = TemplateStorageService.getTemplateById(templateId)

            assertNull(result)
        }

    @Test
    fun `getTemplateById should return null when template is not found`() =
        runBlocking {
            val templateId = UUID.randomUUID()

            coEvery {
                templateRepository.getTemplateFromDB(templateId.toString())
            } returns null

            val result = TemplateStorageService.getTemplateById(templateId)

            assertNull(result)
        }

    @Test
    fun `getTemplateById handles DatabaseManager templateRepository exception when database not initialized`() =
        runBlocking {
            val templateId = UUID.randomUUID()

            // Create a fresh mock for each test
            unmockkAll()

            // Mock the DatabaseManager
            mockkObject(DatabaseManager)

            // Mock the templateRepository method to throw an IllegalStateException
            coEvery { 
                DatabaseManager.templateRepository() 
            } throws IllegalStateException("Database connection not initialized. Call init() first.")

            // Call the function under test
            val result = TemplateStorageService.getTemplateById(templateId)

            // Assert that the function returns null when DatabaseManager.templateRepository() throws an exception
            assertNull(result)
        }
}
