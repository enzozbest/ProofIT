import core.DatabaseManager
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import tables.templates.TemplateRepository
import tables.templates.Template
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
    fun `getTemplateById returns template when found`() = runBlocking {
        val fileURI = "file:///templates/sample.json"

        // Mock the repository to return success
        coEvery {
            templateRepository.saveTemplateToDB(any())
        } answers { Result.success(Unit) }

        val templateId = TemplateStorageService.createTemplate(fileURI)

        assert(templateId?.isNotEmpty() == true)
    }

    @Test
    fun `getTemplateById should return template when successfully retrieving template`() = runBlocking {
        val templateId = UUID.randomUUID()
        val expectedTemplate = Template(id = templateId.toString(), fileURI = "file:///templates/sample.json")

        coEvery {
            templateRepository.getTemplateFromDB(templateId.toString())
        } returns expectedTemplate

        val result = TemplateStorageService.getTemplateById(templateId)

        assertEquals(expectedTemplate, result)
    }






}