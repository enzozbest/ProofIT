package database.core

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import database.tables.templates.TemplateRepository
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import utils.environment.EnvironmentLoader
import kotlin.math.max

/**
 * DatabaseCredentials is a data class that holds the necessary information to connect to a database.
 *
 * @property url The JDBC URL of the database.
 * @property username The username to connect to the database.
 * @property password The password to connect to the database.
 * @property maxPoolSize The maximum number of connections in the connection pool.
 */
internal data class DatabaseCredentials(
    val url: String,
    val username: String,
    val password: String,
    val maxPoolSize: Int = 10,
)

/**
 * DatabaseManager is a singleton object responsible for managing database connections and repositories.
 *
 * This manager handles the lifecycle of database connections, including initialization,
 * configuration, and cleanup. It uses HikariCP for connection pooling and Flyway for
 * database migrations. The object follows a lazy initialization pattern for resources
 * and provides access to repository interfaces.
 */
object DatabaseManager {
    private var database: Database? = null
    private var templateRepository: TemplateRepository? = null
    private var dataSource: HikariDataSource? = null

    /**
     * Resets the database manager state by closing connections and clearing references.
     *
     * This method is primarily used for testing purposes to ensure a clean state
     * between test cases and prevent resource leaks.
     */
    fun reset() {
        dataSource?.close()
        database = null
        dataSource = null
        templateRepository = null
    }

    /**
     * Initializes the database connection and runs all necessary migrations.
     *
     * This method performs the following steps:
     * 1. Retrieves database credentials from environment variables
     * 2. Configures and runs Flyway migrations to ensure schema is up-to-date
     * 3. Sets up the database connection pool using HikariCP
     * 4. Stores the database instance for future use
     *
     * @return The initialized Database connection instance
     * @throws Exception If connection initialization fails for any reason
     */
    internal fun init(): Database {
        val credentials = getDatabaseCredentials()

        return try {
            configureFlyway(credentials)
            val newDatabase = setupDatabase(credentials)
            database = newDatabase
            newDatabase
        } catch (e: Exception) {
            throw e
        }
    }

    fun externalInit() {
        init()
    }

    /**
     * Provides access to the template repository using lazy initialization.
     *
     * This method ensures the database is initialized before creating the repository
     * and caches the repository instance for future calls.
     *
     * @return The template repository instance
     * @throws IllegalStateException If the database has not been initialized
     */
    fun templateRepository(): TemplateRepository {
        val db = checkNotNull(database) { "Database connection not initialized. Call init() first." }
        return templateRepository ?: TemplateRepository(db).also { templateRepository = it }
    }

    /**
     * Sets up the database connection pool using HikariCP.
     *
     * This method configures the connection pool with appropriate settings for
     * performance and reliability, including transaction isolation levels and
     * connection limits.
     *
     * @param credentials The database connection credentials
     * @return A configured Database instance ready for use
     */
    private fun setupDatabase(credentials: DatabaseCredentials): Database {
        val config =
            HikariConfig().apply {
                jdbcUrl = credentials.url
                username = credentials.username
                password = credentials.password
                maximumPoolSize = max(1, credentials.maxPoolSize)
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                driverClassName = "org.postgresql.Driver"
            }
        dataSource = HikariDataSource(config)
        return Database.connect(dataSource!!)
    }

    /**
     * Retrieves the database credentials from environment variables.
     *
     * This method loads database connection settings from the application's
     * environment configuration using the EnvironmentLoader utility.
     *
     * @return A DatabaseCredentials object containing connection parameters
     * @throws Exception If required environment variables are missing
     */
    private fun getDatabaseCredentials(): DatabaseCredentials =
        DatabaseCredentials(
            url = EnvironmentLoader.get("DB_URL"),
            username = EnvironmentLoader.get("DB_USERNAME"),
            password = EnvironmentLoader.get("DB_PASSWORD"),
            maxPoolSize = EnvironmentLoader.get("DB_MAX_POOL_SIZE").toInt(),
        )

    /**
     * Configures and runs Flyway database migrations.
     *
     * This method ensures the database schema is up-to-date by applying
     * any pending migrations from the specified migrations directory.
     *
     * @param credentials The database connection credentials
     */
    private fun configureFlyway(credentials: DatabaseCredentials) {
        Flyway
            .configure()
            .dataSource(credentials.url, credentials.username, credentials.password)
            .locations("classpath:migrations")
            .load()
            .migrate()
    }
}
