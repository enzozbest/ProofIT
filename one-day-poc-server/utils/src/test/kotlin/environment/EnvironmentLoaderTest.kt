package environment

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import utils.environment.EnvironmentLoader
import utils.environment.SystemEnvironment
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

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
        unmockkAll()
    }

    private fun generateEnvironmentFile() {
        val env =
            """
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

    @Test
    fun `Test reset clears environment`() {
        // First verify we can get a value from the loaded environment
        val beforeReset = EnvironmentLoader.get("DB_URL")
        assertNotEquals("", beforeReset)

        // Reset the environment
        EnvironmentLoader.reset()

        // Now try to get the same value, but with no system env fallback
        val afterReset = EnvironmentLoader.get("DB_URL")
        assertEquals("", afterReset)
    }

    @Test
    fun `Test system environment variable fallback with null return`() {
        // This test verifies that the system environment variable is used as a fallback
        // when the .env file doesn't have the variable, but the system env also returns null

        EnvironmentLoader.reset()
        val result = EnvironmentLoader.get("NON_EXISTENT_SYSTEM_VAR")
        assertEquals("", result)
    }

    @Test
    fun `Test system environment variable fallback with non-null return`() {
        EnvironmentLoader.reset()

        mockkObject(SystemEnvironment)
        val testKey = "TEST_SYSTEM_VAR"
        val expectedValue = "system_value"
        every { SystemEnvironment.readSystemVariable(testKey) } returns expectedValue

        val result = EnvironmentLoader.get(testKey)

        assertEquals(expectedValue, result)
        verify { SystemEnvironment.readSystemVariable(testKey) }
    }
}
