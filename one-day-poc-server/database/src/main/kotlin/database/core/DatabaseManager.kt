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
 * DatabaseManager is an object that manages the connection to the database.
 * It initializes the database connection and runs the necessary migrations.
 */
object DatabaseManager {
    private var database: Database? = null
    private var templateRepository: TemplateRepository? = null
    private var dataSource: HikariDataSource? = null

    /**
     * Resets the database manager state. Used for testing purposes.
     */
    fun reset() {
        dataSource?.close()
        database = null
        dataSource = null
        templateRepository = null
    }

    /**
     * Initializes the database connection and runs the necessary migrations.
     *
     * @return The database connection.
     */
    fun init(): Database {
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

    /**
     * Provides access to the template repository
     * @return The template repository instance
     */
    fun templateRepository(): TemplateRepository {
        val db = checkNotNull(database)
        return templateRepository ?: TemplateRepository(db).also { templateRepository = it }
    }

    /**
     * Sets up the database connection.
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
     * Retrieves the database credentials from the .env file.
     */
    private fun getDatabaseCredentials(): DatabaseCredentials =
        DatabaseCredentials(
            url = EnvironmentLoader.get("DB_URL"),
            username = EnvironmentLoader.get("DB_USERNAME"),
            password = EnvironmentLoader.get("DB_PASSWORD"),
            maxPoolSize = EnvironmentLoader.get("DB_MAX_POOL_SIZE").toInt(),
        )

    /**
     * Configures Flyway to run the necessary migrations.
     */
    private fun configureFlyway(credentials: DatabaseCredentials) {
        Flyway
            .configure()
            .dataSource(credentials.url, credentials.username, credentials.password)
            .locations("database/src/main/resources/db/migrations")
            .load()
            .migrate()
    }
}
