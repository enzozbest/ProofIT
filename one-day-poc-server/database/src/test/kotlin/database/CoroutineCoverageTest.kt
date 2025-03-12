package database

import database.core.DatabaseManager
import database.helpers.MockEnvironment
import database.tables.prototypes.Prototype
import database.tables.prototypes.PrototypeRepository
import database.tables.prototypes.Prototypes
import database.tables.templates.Template
import database.tables.templates.TemplateRepository
import database.tables.templates.Templates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import utils.environment.EnvironmentLoader
import java.io.File
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineCoverageTest {
    private lateinit var db: Database
    private lateinit var prototypeRepository: PrototypeRepository
    private lateinit var templateRepository: TemplateRepository
    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    // Invalid database for testing failure paths
    private val invalidDb = Database.connect(
        url = "jdbc:postgresql://localhost:5432/nonexistentdb",
        driver = "org.postgresql.Driver",
        user = "invalid",
        password = "invalid"
    )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        MockEnvironment.postgresContainer.start()

        EnvironmentLoader.reset()
        MockEnvironment.generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.ENV_FILE)

        db = DatabaseManager.init()
        transaction(db) {
            SchemaUtils.create(Prototypes)
            SchemaUtils.create(Templates)
        }
        prototypeRepository = PrototypeRepository(db)
        templateRepository = TemplateRepository(db)
    }

    @AfterEach
    fun tearDown() {
        transaction(db) {
            SchemaUtils.drop(Prototypes)
            SchemaUtils.drop(Templates)
        }
        File(MockEnvironment.ENV_FILE).delete()
        MockEnvironment.stopContainer()

        Dispatchers.resetMain()
    }

    @Test
    fun `Test createPrototype with StandardTestDispatcher`() = runTest(testDispatcher) {
        val prototype = Prototype(
            id = UUID.randomUUID(),
            userId = "testuserId",
            userPrompt = "Hello",
            fullPrompt = "Hello, World!",
            s3key = "testKey",
            createdAt = Instant.now(),
        )

        val result = prototypeRepository.createPrototype(prototype)
        testScheduler.advanceUntilIdle() // This is key to ensure all coroutines complete

        assertTrue(result.isSuccess)
    }

    @Test
    fun `Test getPrototype with StandardTestDispatcher`() = runTest(testDispatcher) {
        val id = UUID.randomUUID()
        val prototype = Prototype(
            id = id,
            userId = "testuserId",
            userPrompt = "Hello",
            fullPrompt = "Hello, World!",
            s3key = "testKey",
            createdAt = Instant.now(),
        )

        val createResult = prototypeRepository.createPrototype(prototype)
        testScheduler.advanceUntilIdle()
        assertTrue(createResult.isSuccess)

        val result = prototypeRepository.getPrototype(id)
        testScheduler.advanceUntilIdle()

        assertTrue(result.isSuccess)
        val retrievedPrototype = result.getOrNull()
        assertNotNull(retrievedPrototype)
        assertEquals("Hello", retrievedPrototype.userPrompt)
    }

    @Test
    fun `Test getPrototypesByUserId with StandardTestDispatcher`() = runTest(testDispatcher) {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()

        val prototype1 = Prototype(
            id = id1,
            userId = "testuserId",
            userPrompt = "Hello 1",
            fullPrompt = "Hello, World! 1",
            s3key = "testKey1",
            createdAt = Instant.now(),
        )

        val prototype2 = Prototype(
            id = id2,
            userId = "testuserId",
            userPrompt = "Hello 2",
            fullPrompt = "Hello, World! 2",
            s3key = "testKey2",
            createdAt = Instant.now(),
        )

        val createResult1 = prototypeRepository.createPrototype(prototype1)
        testScheduler.advanceUntilIdle()
        assertTrue(createResult1.isSuccess)

        val createResult2 = prototypeRepository.createPrototype(prototype2)
        testScheduler.advanceUntilIdle()
        assertTrue(createResult2.isSuccess)

        val result = prototypeRepository.getPrototypesByUserId("testuserId")
        testScheduler.advanceUntilIdle()

        assertTrue(result.isSuccess)
        val prototypes = result.getOrNull()
        assertNotNull(prototypes)
        assertEquals(2, prototypes.size)
    }

    @Test
    fun `Test saveTemplateToDB with StandardTestDispatcher`() = runTest(testDispatcher) {
        val template = Template(
            id = "test-template-id",
            fileURI = "test-file-uri"
        )

        val result = templateRepository.saveTemplateToDB(template)
        testScheduler.advanceUntilIdle()

        assertTrue(result.isSuccess)
    }

    @Test
    fun `Test getTemplateFromDB with StandardTestDispatcher`() = runTest(testDispatcher) {
        val template = Template(
            id = "test-template-id",
            fileURI = "test-file-uri"
        )

        val saveResult = templateRepository.saveTemplateToDB(template)
        testScheduler.advanceUntilIdle()
        assertTrue(saveResult.isSuccess)

        val result = templateRepository.getTemplateFromDB("test-template-id")
        testScheduler.advanceUntilIdle()

        assertNotNull(result)
        assertEquals("test-file-uri", result.fileURI)
    }

    @Test
    fun `Test createPrototype failure path with StandardTestDispatcher`() = runTest(testDispatcher) {
        val invalidRepository = PrototypeRepository(invalidDb)

        val prototype = Prototype(
            id = UUID.randomUUID(),
            userId = "testuserId",
            userPrompt = "Hello",
            fullPrompt = "Hello, World!",
            s3key = "testKey",
            createdAt = Instant.now(),
        )

        val result = invalidRepository.createPrototype(prototype)
        testScheduler.advanceUntilIdle()

        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
    }

    @Test
    fun `Test getPrototype failure path with StandardTestDispatcher`() = runTest(testDispatcher) {
        val invalidRepository = PrototypeRepository(invalidDb)

        val result = invalidRepository.getPrototype(UUID.randomUUID())
        testScheduler.advanceUntilIdle()

        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
    }

    @Test
    fun `Test getPrototypesByUserId failure path with StandardTestDispatcher`() = runTest(testDispatcher) {
        val invalidRepository = PrototypeRepository(invalidDb)

        val result = invalidRepository.getPrototypesByUserId("testuserId")
        testScheduler.advanceUntilIdle()

        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
    }

    @Test
    fun `Test saveTemplateToDB failure path with StandardTestDispatcher`() = runTest(testDispatcher) {
        val invalidRepository = TemplateRepository(invalidDb)

        val template = Template(
            id = "test-template-id",
            fileURI = "test-file-uri"
        )

        val result = invalidRepository.saveTemplateToDB(template)
        testScheduler.advanceUntilIdle()

        assertFalse(result.isSuccess)
        assertTrue(result.isFailure)
    }

    @Test
    fun `Test getTemplateFromDB failure path with StandardTestDispatcher`() = runTest(testDispatcher) {
        val invalidRepository = TemplateRepository(invalidDb)

        val result = invalidRepository.getTemplateFromDB("test-template-id")
        testScheduler.advanceUntilIdle()

        assertNull(result)
    }
}
