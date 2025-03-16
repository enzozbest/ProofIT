package database

import database.core.DatabaseManager
import database.helpers.MockEnvironment
import database.tables.prototypes.Prototype
import database.tables.prototypes.PrototypeRepository
import database.tables.prototypes.Prototypes
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import utils.environment.EnvironmentLoader
import java.io.File
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class PrototypeRepositoryTest {
    private lateinit var db: Database
    private lateinit var repository: PrototypeRepository

    @BeforeEach
    fun setUp() {
        MockEnvironment.postgresContainer.start()

        EnvironmentLoader.reset()
        MockEnvironment.generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.ENV_FILE)

        db = DatabaseManager.init()
        transaction(db) {
            SchemaUtils.create(Prototypes)
        }
        repository = PrototypeRepository(db)
    }

    @AfterEach
    fun tearDown() {
        transaction(db) {
            SchemaUtils.drop(Prototypes)
        }
        File(MockEnvironment.ENV_FILE).delete()
        MockEnvironment.stopContainer()
    }

    @Test
    fun `Test database initialises correctly`() {
        transaction(db) {
            val tables = db.dialect.allTablesNames()
            assertTrue(tables.contains("public.prototypes"))
        }
    }

    @Test
    fun `Test creates prototype`() =
        runTest {
            val result = createPrototype()
            assertTrue(result.isSuccess)
        }

    @Test
    fun `Test retrieves prototype`() =
        runTest {
            val id = UUID.randomUUID()
            val result = createPrototype(id)
            assertTrue(result.isSuccess)

            transaction(db) {
                val count = Prototypes.selectAll().count()
                println("PTTS: $count")
            }

            val retrieved = repository.getPrototype(id).getOrNull()
            assertNotNull(retrieved)
            assertEquals("Hello", retrieved.userPrompt)
        }

    @Test
    fun `Test retrieve by user id`() =
        runTest {
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()

            val result1 = createPrototype(id1)
            assertTrue(result1.isSuccess)

            val result2 = createPrototype(id2)
            assertTrue(result2.isSuccess)

            val results = repository.getPrototypesByUserId("testuserId").getOrNull()
            assertNotNull(results)
            assertEquals(2, results.size)
        }

    @Test
    fun `Test retrieve non-existent prototype`() =
        runTest {
            val retrieved = repository.getPrototype(UUID.randomUUID()).getOrNull()
            assertNull(retrieved)
        }

    @Test
    fun `Test createPrototype failure path`() =
        runTest {
            // Create a repository with an invalid database connection
            val invalidDb = Database.connect(
                url = "jdbc:postgresql://localhost:5432/nonexistentdb",
                driver = "org.postgresql.Driver",
                user = "invalid",
                password = "invalid"
            )
            val invalidRepository = PrototypeRepository(invalidDb)

            val prototype = Prototype(
                id = UUID.randomUUID(),
                userId = "testuserId",
                userPrompt = "Hello",
                fullPrompt = "Hello, World!",
                s3key = "testKey",
                createdAt = Instant.now(),
                projectName = "Hello program",
            )

            val result = invalidRepository.createPrototype(prototype)
            assertFalse(result.isSuccess)
            assertTrue(result.isFailure)
        }

    @Test
    fun `Test getPrototype failure path`() =
        runTest {
            // Create a repository with an invalid database connection
            val invalidDb = Database.connect(
                url = "jdbc:postgresql://localhost:5432/nonexistentdb",
                driver = "org.postgresql.Driver",
                user = "invalid",
                password = "invalid"
            )
            val invalidRepository = PrototypeRepository(invalidDb)

            val result = invalidRepository.getPrototype(UUID.randomUUID())
            assertFalse(result.isSuccess)
            assertTrue(result.isFailure)
        }

    @Test
    fun `Test getPrototypesByUserId failure path`() =
        runTest {
            // Create a repository with an invalid database connection
            val invalidDb = Database.connect(
                url = "jdbc:postgresql://localhost:5432/nonexistentdb",
                driver = "org.postgresql.Driver",
                user = "invalid",
                password = "invalid"
            )
            val invalidRepository = PrototypeRepository(invalidDb)

            val result = invalidRepository.getPrototypesByUserId("testuserId")
            assertFalse(result.isSuccess)
            assertTrue(result.isFailure)
        }

    private suspend fun createPrototype(id: UUID = UUID.randomUUID()): Result<Unit> {
        val prototype =
            Prototype(
                id = id,
                userId = "testuserId",
                userPrompt = "Hello",
                fullPrompt = "Hello, World!",
                s3key = "testKey",
                createdAt = Instant.now(),
                projectName = "Hello program",
            )
        return repository.createPrototype(prototype)
    }
}
