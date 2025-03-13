package database.helpers

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File
import java.time.Duration

object MockEnvironment {
    const val ENV_FILE = "test.env"
    private val lock = Any()
    private var container: PostgreSQLContainer<*>? = null

    val postgresContainer: PostgreSQLContainer<*>
        get() =
            synchronized(lock) {
                container ?: createAndStartContainer().also { container = it }
            }

    private fun createAndStartContainer(): PostgreSQLContainer<*> {
        val newContainer =
            PostgreSQLContainer("postgres:15").apply {
                withDatabaseName("testdb")
                withUsername("testuser")
                withPassword("testpassword")
                withStartupTimeout(Duration.ofSeconds(60))
                waitingFor(
                    Wait
                        .forLogMessage(".*database system is ready to accept connections.*\\n", 2)
                        .withStartupTimeout(Duration.ofSeconds(60)),
                )
                withReuse(true)
                setCommand("postgres", "-c", "fsync=off")
            }

        try {
            newContainer.start()
            // Additional verification that container is truly ready
            var retries = 5
            while (retries > 0) {
                try {
                    newContainer.createConnection("").use { connection ->
                        if (connection.isValid(5)) {
                            return newContainer
                        }
                    }
                } catch (e: Exception) {
                    if (retries == 1) throw e
                }
                Thread.sleep(1000)
                retries--
            }
            throw IllegalStateException("Container started but connection test failed")
        } catch (e: Exception) {
            newContainer.stop()
            throw e
        }
    }

    fun stopContainer() {
        synchronized(lock) {
            try {
                container?.let { cont ->
                    if (cont.isRunning) {
                        cont.stop()
                    }
                }
            } finally {
                container = null
            }
        }
    }

    fun generateEnvironmentFile() {
        val cont = postgresContainer // Ensure container is started before getting connection details
        val env =
            """
            DB_URL=${cont.jdbcUrl}
            DB_USERNAME=${cont.username}
            DB_PASSWORD=${cont.password}
            DB_MAX_POOL_SIZE=10
            """.trimIndent()
        File(ENV_FILE).writeText(env)
    }
}
