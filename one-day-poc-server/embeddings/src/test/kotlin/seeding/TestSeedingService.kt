package seeding

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger

class TestSeedingService {
    internal val seeder = Seeder
    private lateinit var mockLogger: Logger
    private lateinit var originalLogger: Logger

    @BeforeEach
    fun setUp() {
        mockkObject(Seeder)
        mockkObject(SeedingService)

        originalLogger = Seeder.logger
        mockLogger = mockk<Logger>(relaxed = true)
        SeedingService.logger = mockLogger
    }

    @AfterEach
    fun tearDown() {
        SeedingService.logger = originalLogger
        unmockkAll()
    }

    @Test
    fun `seedComponents should process component library on success path`() =
        runBlocking {
            SeedingService.seedComponents()

            coVerify(exactly = 1) { Seeder.processComponentLibrary(any()) }
        }

    @Test
    fun `seedComponents should handle exceptions from Seeder`() =
        runBlocking {
            // Mock Seeder to throw an exception
            coEvery { Seeder.processComponentLibrary(any()) } throws RuntimeException("Test exception")

            SeedingService.seedComponents()

            verify { mockLogger.error(any()) }
        }

    @Test
    fun `seedComponents should handle missing resource directory`() =
        runBlocking {
            // Mock SeedingService to simulate a missing resource
            every { SeedingService.getResourceUrl("components/metadata") } returns null

            SeedingService.seedComponents()

            verify { mockLogger.error(any()) }

            // Verify Seeder.processComponentLibrary was not called
            coVerify(exactly = 0) { Seeder.processComponentLibrary(any()) }
        }
}
