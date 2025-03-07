import core.DatabaseManager
import helpers.MockEnvironment
import helpers.MockEnvironment.generateEnvironmentFile
import helpers.MockEnvironment.postgresContainer
import kcl.seg.rtt.utils.environment.EnvironmentLoader
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import tables.templates.Template
import tables.templates.TemplateRepository
import tables.templates.Templates
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertNull

class TemplateRepositoryTest {
    private lateinit var db: Database
    private lateinit var repository: TemplateRepository

    @BeforeEach
    fun setUp() {
        EnvironmentLoader.reset()
        generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.envFile)

        db = DatabaseManager.init()
        transaction(db) {
            SchemaUtils.drop(Templates)
            SchemaUtils.create(Templates)
        }
        repository = TemplateRepository(db)
    }

    @AfterEach
    fun tearDown() {
        transaction(db) {
            SchemaUtils.drop(Templates)
        }
        File(MockEnvironment.envFile).delete()
    }

    @Test
    fun `Test database initialises correctly`() {
        transaction(db) {
            val tables = db.dialect.allTablesNames()
            assertTrue(tables.contains("public.templates"))
        }
    }

    @Test
    fun `Test saves template successfully`() = runTest {
        val result = createTemplate()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `Test retrieves template`() = runTest {
        val id = "test-template-id"
        val result = createTemplate(id)
        assertTrue(result.isSuccess)

        val retrieved = repository.getTemplateFromDB(id)
        assertNotNull(retrieved)
        assertEquals("test-file-uri", retrieved.fileURI)
    }

    @Test
    fun `Test retrieve non-existent template`() = runTest {
        val retrieved = repository.getTemplateFromDB("non-existent-id")
        assertNull(retrieved)
    }

    @Test
    fun `Test save template with existing id updates the template`() = runTest {
        val id = "test-template-id"
        val result1 = createTemplate(id, "original-uri")
        assertTrue(result1.isSuccess)

        val result2 = createTemplate(id, "updated-uri")
        assertTrue(result2.isSuccess)

        val retrieved = repository.getTemplateFromDB(id)
        assertNotNull(retrieved)
        assertEquals("updated-uri", retrieved.fileURI)
    }

    @Test
    fun `Test save template with empty id fails`() = runTest {
        val result = createTemplate("")
        assertTrue(result.isFailure)
    }

    @Test
    fun `Test transaction rollback on error`() = runTest {
        // First create a valid template
        val id = "test-template-id"
        val result1 = createTemplate(id)
        assertTrue(result1.isSuccess)

        // Try to create an invalid template
        val result2 = createTemplate("")
        assertTrue(result2.isFailure)

        // Verify the original template is still intact
        val retrieved = repository.getTemplateFromDB(id)
        assertNotNull(retrieved)
        assertEquals("test-file-uri", retrieved.fileURI)

        // Verify no other templates were created
        transaction(db) {
            val count = Templates.selectAll().count()
            assertEquals(1, count)
        }
    }

    private suspend fun createTemplate(id: String = "test-template-id", fileURI: String = "test-file-uri"): Result<Unit> {
        val template = Template(
            id = id,
            fileURI = fileURI
        )
        return repository.saveTemplateToDB(template)
    }

    @Test
    fun `Test database error handling in getTemplateFromDB`() = runTest {
        // Setup logger capture
        val logger = LoggerFactory.getLogger(TemplateRepository::class.java) as Logger
        val listAppender = ListAppender<ILoggingEvent>()
        listAppender.start()
        logger.addAppender(listAppender)

        try {
            // First create a template
            val id = "test-template-id"
            val result = createTemplate(id)
            assertTrue(result.isSuccess)

            // Close the database connection to force an error
            transaction(db) {
                SchemaUtils.drop(Templates)
            }

            // Try to retrieve the template, which should now fail but return null
            val retrieved = repository.getTemplateFromDB(id)
            assertNull(retrieved)

            // Verify error was logged
            val logMessages = listAppender.list
            assertTrue(logMessages.isNotEmpty())
            assertTrue(logMessages.any { event ->
                event.level.toString() == "ERROR" &&
                event.message.contains("Error retrieving template with ID $id")
            })
        } finally {
            // Cleanup logger
            logger.detachAppender(listAppender)
            listAppender.stop()
        }
    }
}
