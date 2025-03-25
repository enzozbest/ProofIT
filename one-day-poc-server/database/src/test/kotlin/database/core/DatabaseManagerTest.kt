package database.core

import com.zaxxer.hikari.HikariDataSource
import database.helpers.MockEnvironment
import database.helpers.MockEnvironment.ENV_FILE
import database.helpers.MockEnvironment.generateEnvironmentFile
import database.tables.templates.TemplateRepository
import org.flywaydb.core.api.FlywayException
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import utils.environment.EnvironmentLoader
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatabaseManagerTest {
    @BeforeAll
    fun setup() {
        MockEnvironment.postgresContainer.start()
        generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(ENV_FILE)
    }

    @BeforeEach
    fun setupEach() {
        DatabaseManager.reset()
        EnvironmentLoader.loadEnvironmentFile(ENV_FILE)
    }

    @AfterEach
    fun cleanupEach() {
        DatabaseManager.reset()
        EnvironmentLoader.reset()
    }

    @AfterAll
    fun cleanup() {
        DatabaseManager.reset()
        EnvironmentLoader.reset()
        MockEnvironment.stopContainer()
        File(ENV_FILE).delete()
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
        File(ENV_FILE).writeText(invalidEnv)
        EnvironmentLoader.loadEnvironmentFile(ENV_FILE)

        val exception =
            assertThrows<Exception> {
                DatabaseManager.init()
            }
        assertNotNull(exception)

        generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(ENV_FILE)
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
        File(ENV_FILE).writeText(minPoolEnv)
        EnvironmentLoader.loadEnvironmentFile(ENV_FILE)

        val database = DatabaseManager.init()
        assertNotNull(database)

        generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(ENV_FILE)
    }

    @Test
    fun `test default maxPoolSize is 10`() {
        val credentials = DatabaseCredentials(
            url = "jdbc:postgresql://localhost:5432/testdb",
            username = "test_user",
            password = "test_pass",
        )

        assertEquals(10, credentials.maxPoolSize, "Default maxPoolSize should be 10")
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
        File(ENV_FILE).writeText(missingEnv)
        EnvironmentLoader.loadEnvironmentFile(ENV_FILE)

        val exception =
            assertThrows<FlywayException> {
                DatabaseManager.init()
            }
        assertNotNull(exception)
        assertTrue(exception.message?.isNotBlank() == true, "Exception message should not be empty")

        generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(ENV_FILE)
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
        File(ENV_FILE).writeText(connectionEnv)
        EnvironmentLoader.loadEnvironmentFile(ENV_FILE)

        val database = DatabaseManager.init()
        assertNotNull(database)

        val result =
            transaction(database) {
                exec("SELECT 1") {
                    it.next()
                    it.getInt(1)
                }
            }
        assertEquals(1, result)

        generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(ENV_FILE)
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
    fun `test chat repository creation and caching`() {
        DatabaseManager.init()

        val repoField =
            DatabaseManager::class.java
                .getDeclaredField("chatRepository")
                .apply { isAccessible = true }

        assertNull(repoField.get(DatabaseManager))

        val repo1 = DatabaseManager.chatRepository()
        assertNotNull(repoField.get(DatabaseManager))

        val repo2 = DatabaseManager.chatRepository()
        assertSame(repo1, repo2)
    }

    @Test
    fun `test chat repository throws exception when database is uninitialized`() {
        val exception = assertThrows<IllegalStateException> {
            DatabaseManager.chatRepository()
        }
        assertEquals("Database connection not initialized. Call init() first.", exception.message)
    }

    @Test
    fun `test flyway migration execution`() {
        val database = DatabaseManager.init()
        assertNotNull(database)

        val tablesExist =
            transaction(database) {
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

    @Test
    fun `test tables created by migrations`() {
        val database = DatabaseManager.init()
        assertNotNull(database)

        val templatesTableExists =
            transaction(database) {
                exec(
                    """
                        SELECT EXISTS (
                            SELECT FROM information_schema.tables 
                            WHERE table_schema = 'public' 
                            AND table_name = 'templates'
                        )
                    """,
                ) {
                    it.next()
                    it.getBoolean(1)
                } == true
            }
        assertTrue(templatesTableExists, "Migrations should create templates table")

        val prototypesTableExists =
            transaction(database) {
                exec(
                    """
                        SELECT EXISTS (
                            SELECT FROM information_schema.tables 
                            WHERE table_schema = 'public' 
                            AND table_name = 'prototypes'
                        )
                    """,
                ) {
                    it.next()
                    it.getBoolean(1)
                } == true
            }
        assertTrue(prototypesTableExists, "Migrations should create prototypes table")
    }
}
