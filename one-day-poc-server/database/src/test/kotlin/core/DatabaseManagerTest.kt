package core

import com.zaxxer.hikari.HikariDataSource
import helpers.MockEnvironment
import kcl.seg.rtt.utils.environment.EnvironmentLoader
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import tables.templates.TemplateRepository
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseManagerTest {
    @BeforeAll
    fun setup() {
        // Ensure container is started and environment file is created with correct connection details
        MockEnvironment.generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.ENV_FILE)
    }

    @BeforeEach
    fun setupEach() {
        // Reset state but keep container running
        DatabaseManager.reset()
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.ENV_FILE)
    }

    @AfterEach
    fun cleanupEach() {
        // Reset state but keep container running for next test
        DatabaseManager.reset()
        EnvironmentLoader.reset()
    }

    @AfterAll
    fun cleanup() {
        // Clean up everything after all tests are done
        DatabaseManager.reset()
        EnvironmentLoader.reset()
        MockEnvironment.stopContainer()
        File(MockEnvironment.ENV_FILE).delete()
    }

    @Test
    fun `test successful database initialization`() {
        val database = DatabaseManager.init()
        assertNotNull(database)
    }

    @Test
    fun `test template repository access after initialization`() {
        DatabaseManager.init()
        val repository = DatabaseManager.templateRepository()
        assertNotNull(repository)
        assertTrue(repository is TemplateRepository)
    }

    @Test
    fun `test template repository access before initialization throws exception`() {
        val exception =
            assertThrows<IllegalStateException> {
                DatabaseManager.templateRepository()
            }
        assertEquals(
            "Database connection not initialized. Call init() first.",
            exception.message,
        )
    }

    @Test
    fun `test database initialization with invalid credentials`() {
        val invalidEnv =
            """
            DB_URL=jdbc:postgresql://invalid:5432/nonexistent
            DB_USERNAME=${MockEnvironment.postgresContainer.username}
            DB_PASSWORD=${MockEnvironment.postgresContainer.password}
            DB_MAX_POOL_SIZE=10
            """.trimIndent()
        File(MockEnvironment.ENV_FILE).writeText(invalidEnv)
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.ENV_FILE)

        val exception =
            assertThrows<Exception> {
                DatabaseManager.init()
            }
        assertNotNull(exception)

        MockEnvironment.generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.ENV_FILE)
    }

    @Test
    fun `test database initialization with minimum pool size`() {
        val minPoolEnv =
            """
            DB_URL=${MockEnvironment.postgresContainer.jdbcUrl}
            DB_USERNAME=${MockEnvironment.postgresContainer.username}
            DB_PASSWORD=${MockEnvironment.postgresContainer.password}
            DB_MAX_POOL_SIZE=0
            """.trimIndent()
        File(MockEnvironment.ENV_FILE).writeText(minPoolEnv)
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.ENV_FILE)

        val database = DatabaseManager.init()
        assertNotNull(database)

        MockEnvironment.generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.ENV_FILE)
    }

    @Test
    fun `test database initialization with missing credentials`() {
        val missingEnv =
            """
            DB_URL=
            DB_USERNAME=
            DB_PASSWORD=
            DB_MAX_POOL_SIZE=10
            """.trimIndent()
        File(MockEnvironment.ENV_FILE).writeText(missingEnv)
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.ENV_FILE)

        val exception =
            assertThrows<org.flywaydb.core.api.FlywayException> {
                DatabaseManager.init()
            }
        assertNotNull(exception)
        assertTrue(exception.message?.isNotBlank() == true, "Exception message should not be empty")

        MockEnvironment.generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.ENV_FILE)
    }

    @Test
    fun `test database connection parameters`() {
        val connectionEnv =
            """
            DB_URL=${MockEnvironment.postgresContainer.jdbcUrl}
            DB_USERNAME=${MockEnvironment.postgresContainer.username}
            DB_PASSWORD=${MockEnvironment.postgresContainer.password}
            DB_MAX_POOL_SIZE=5
            """.trimIndent()
        File(MockEnvironment.ENV_FILE).writeText(connectionEnv)
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.ENV_FILE)

        val database = DatabaseManager.init()
        assertNotNull(database)

        val result =
            org.jetbrains.exposed.sql.transactions.transaction(database) {
                exec("SELECT 1") {
                    it.next()
                    it.getInt(1)
                }
            }
        assertEquals(1, result)

        MockEnvironment.generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(MockEnvironment.ENV_FILE)
    }

    @Test
    fun `test database connection cleanup on reset`() {
        DatabaseManager.init()
        val initialDataSource =
            DatabaseManager::class.java
                .getDeclaredField("dataSource")
                .apply { isAccessible = true }
                .get(DatabaseManager) as HikariDataSource

        assertFalse(initialDataSource.isClosed, "DataSource should be open after initialization")

        DatabaseManager.reset()
        assertTrue(initialDataSource.isClosed, "DataSource should be closed after reset")
    }

    @Test
    fun `test template repository creation and caching`() {
        DatabaseManager.init()

        val repoField =
            DatabaseManager::class.java
                .getDeclaredField("templateRepository")
                .apply { isAccessible = true }

        assertNull(repoField.get(DatabaseManager))

        val repo1 = DatabaseManager.templateRepository()
        assertNotNull(repoField.get(DatabaseManager))

        val repo2 = DatabaseManager.templateRepository()
        assertSame(repo1, repo2)
    }

    @Test
    fun `test flyway migration execution`() {
        val database = DatabaseManager.init()
        assertNotNull(database)

        val tablesExist =
            org.jetbrains.exposed.sql.transactions.transaction(database) {
                exec(
                    """
                        SELECT EXISTS (
                            SELECT FROM information_schema.tables 
                            WHERE table_schema = 'public' 
                            AND table_name = 'flyway_schema_history'
                        )
                    """,
                ) {
                    it.next()
                    it.getBoolean(1)
                } == true
            }
        assertTrue(tablesExist, "Flyway migrations should create flyway_schema_history table")
    }
}
