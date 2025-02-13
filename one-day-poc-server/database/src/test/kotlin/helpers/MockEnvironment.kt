package helpers

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.File

object MockEnvironment {
    const val envFile = "test.env"


    val postgresContainer = PostgreSQLContainer("postgres:15").apply {
        withDatabaseName("testdb")
        withUsername("testuser")
        withPassword("testpassword")
        waitingFor(Wait.forListeningPort())
    }

    fun generateEnvironmentFile() {
        val env = """
            DB_URL=${postgresContainer.jdbcUrl}  
            DB_USERNAME=${postgresContainer.username}
            DB_PASSWORD=${postgresContainer.password} 
            DB_MAX_POOL_SIZE=10
        """.trimIndent()
        File(envFile).writeText(env)
    }
}