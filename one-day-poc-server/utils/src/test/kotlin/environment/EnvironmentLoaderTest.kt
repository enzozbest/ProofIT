package environment

import kcl.seg.rtt.utils.environment.EnvironmentLoader
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class EnvironmentLoaderTest {
    private val envFile = "test.env"

    @BeforeEach
    fun setup() {
        generateEnvironmentFile()
        EnvironmentLoader.loadEnvironmentFile(envFile)
    }

    @AfterEach
    fun teardown() {
        File(envFile).delete()
    }

    private fun generateEnvironmentFile() {
        val env = """
            DB_URL=testUrl
            DB_USER=testUser
            DB_PASSWORD=testPassword
        """.trimIndent()
        File(envFile).writeText(env)
    }

    @Test
    fun `Test get environment variable`() {
        val url = EnvironmentLoader.get("DB_URL")
        val user = EnvironmentLoader.get("DB_USER")
        val password = EnvironmentLoader.get("DB_PASSWORD")
        assert(url == "testUrl")
        assert(user == "testUser")
        assert(password == "testPassword")
    }

    @Test
    fun `Test getting non-existent environment variable`() {
        val result = EnvironmentLoader.get("NON_EXISTENT")
        assertEquals("", result)
    }

    @Test
    fun `Test loading non-existent environment file`() {
        EnvironmentLoader.reset()
        EnvironmentLoader.loadEnvironmentFile("non-existent.env")
        val result = EnvironmentLoader.get("DB_URL")
        assertEquals("", result)
    }

}