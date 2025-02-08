import helpers.MockEnvironment
import helpers.MockEnvironment.generateEnvironmentFile
import io.mockk.clearAllMocks
import kcl.seg.rtt.database.core.PoCDatabase
import kcl.seg.rtt.utils.environment.EnvironmentLoader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertNotNull
import kotlin.test.assertSame

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PoCDatabaseTest {
    @BeforeEach
    fun setUp() {
        MockEnvironment.postgresContainer.start()
        EnvironmentLoader.reset()
        generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.envFile)
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        MockEnvironment.postgresContainer.stop()
    }

    @Test
    fun `Test database initialises when accessed`() {
        val database = PoCDatabase.database
        assertNotNull(database)
    }

    @Test
    fun `Test database initialises only once`() {
        val db1 = PoCDatabase.database
        val db2 = PoCDatabase.database
        assertSame(db1, db2)
    }
}