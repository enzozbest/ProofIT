import core.DatabaseManager
import helpers.MockEnvironment
import helpers.MockEnvironment.generateEnvironmentFile
import helpers.MockEnvironment.postgresContainer
import kcl.seg.rtt.database.repositories.Prototype
import kcl.seg.rtt.database.repositories.Prototypes
import kcl.seg.rtt.utils.environment.EnvironmentLoader
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tables.prototypes.PrototypeRepository
import java.io.File
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PrototypeRepositoryTest {


    private lateinit var db: Database
    private lateinit var repository: PrototypeRepository

    @BeforeEach
    fun setUp() {
        postgresContainer.start()

        EnvironmentLoader.reset()
        generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.envFile)

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
        postgresContainer.stop()
        File(MockEnvironment.envFile).delete()
    }

    @Test
    fun `Test database initialises correctly`() {
        transaction(db) {
            val tables = db.dialect.allTablesNames()
            assertTrue(tables.contains("public.prototypes"))
        }
    }

    @Test
    fun `Test creates prototype`() = runTest {
        val result = createPrototype()
        assertTrue(result.isSuccess)
    }

    @Test
    fun `Test retrieves prototype`() = runTest {
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
    fun `Test retrieve by user id`() = runTest {
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
    fun `Test retrieve non-existent prototype`() = runTest {
        val retrieved = repository.getPrototype(UUID.randomUUID()).getOrNull()
        kotlin.test.assertNull(retrieved)
    }

    private suspend fun createPrototype(id: UUID = UUID.randomUUID()): Result<Unit> {
        val prototype = Prototype(
            id = id,
            userId = "testuserId",
            userPrompt = "Hello",
            fullPrompt = "Hello, World!",
            s3key = "testKey",
            createdAt = Instant.now()
        )
        return repository.createPrototype(prototype)
    }


}
