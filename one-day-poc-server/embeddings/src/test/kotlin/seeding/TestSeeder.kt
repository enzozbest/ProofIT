package seeding

import embeddings.StoreTemplateResponse
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import templates.TemplateService
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.io.path.pathString
import kotlin.test.assertFailsWith

class TestSeeder {
    private lateinit var mockLogger: Logger
    private lateinit var originalLogger: Logger

    @BeforeEach
    fun setUp() {
        mockkObject(TemplateService)

        originalLogger = Seeder.logger
        mockLogger = mockk<Logger>(relaxed = true)
        Seeder.logger = mockLogger
    }

    @AfterEach
    fun tearDown() {
        Seeder.logger = originalLogger
        unmockkAll()
    }

    @Test
    fun `processComponentLibrary should process valid directory`() =
        runBlocking {
            // Mock the embedding service response
            coEvery {
                TemplateService.storeTemplate(any(), any(), any())
            } returns StoreTemplateResponse("success")

            val tempDirPath = createTempDirectory(prefix = "test")
            val tempDir = File(tempDirPath.pathString)

            try {
                val file = File(tempDir, "test.json")
                file.writeText(
                    """
                    {
                        "@context": "https://schema.org",
                        "@type": "Person",
                        "name": "John Doe"
                    }
                    """.trimIndent(),
                )

                // Test processComponentLibrary method
                Seeder.processComponentLibrary(tempDir.absolutePath)

                coVerify {
                    TemplateService.storeTemplate("test", any(), any())
                }
            } finally {
                tempDir.deleteRecursively()
            }
        }

    @Test
    fun `processComponentLibrary should throw exception for invalid directory`() {
        assertFailsWith<IllegalArgumentException> {
            runBlocking {
                Seeder.processComponentLibrary("/non/existent/directory")
            }
        }
    }

    @Test
    fun `processComponentLibrary should process only JSON-LD files`() =
        runBlocking {
            coEvery {
                TemplateService.storeTemplate(any(), any(), any())
            } returns StoreTemplateResponse("success")

            val tempDirPath = createTempDirectory(prefix = "test")
            val tempDir = File(tempDirPath.pathString)

            try {
                val jsonLdFile = File(tempDir, "test-ld.json")
                jsonLdFile.writeText(
                    """
                    {
                        "@context": "https://schema.org",
                        "@type": "Person",
                        "name": "John Doe"
                    }
                    """.trimIndent(),
                )

                // Create non-JSON-LD file
                val jsonFile = File(tempDir, "test.json")
                jsonFile.writeText(
                    """
                    {
                        "name": "Just a regular JSON"
                    }
                    """.trimIndent(),
                )

                // Create non-JSON file
                File(tempDir, "test.txt").writeText("Not a JSON file")

                Seeder.processComponentLibrary(tempDir.absolutePath)

                // Verify embedAndStore was called only once (for the JSON-LD file)
                coVerify(exactly = 1) {
                    TemplateService.storeTemplate(any(), any(), any())
                }

                // Verify embedAndStore was called with the correct file name
                coVerify {
                    TemplateService.storeTemplate("test-ld", any(), any())
                }
            } finally {
                tempDir.deleteRecursively()
            }
        }

    @Test
    fun `processComponentLibrary should handle embedding service errors`() =
        runBlocking {
            coEvery {
                TemplateService.storeTemplate(any(), any(), any())
            } throws RuntimeException("Test exception")

            val tempDirPath = createTempDirectory(prefix = "test")
            val tempDir = File(tempDirPath.pathString)

            try {
                val file = File(tempDir, "test.json")
                file.writeText(
                    """
                    {
                        "@context": "https://schema.org",
                        "@type": "Person",
                        "name": "John Doe"
                    }
                    """.trimIndent(),
                )

                // Test processComponentLibrary method - it should not throw an exception
                Seeder.processComponentLibrary(tempDir.absolutePath)

                // Verify embedAndStore was called
                coVerify {
                    TemplateService.storeTemplate("test", any(), any())
                }

                verify { mockLogger.error(any()) }
            } finally {
                tempDir.deleteRecursively()
            }
        }

    @Test
    fun `processComponentLibrary should handle failed embedding responses`() =
        runBlocking {
            coEvery {
                TemplateService.storeTemplate(any(), any(), any())
            } returns StoreTemplateResponse("failed", "Test failure")

            val tempDirPath = createTempDirectory(prefix = "test")
            val tempDir = File(tempDirPath.pathString)

            try {
                // Create JSON-LD file
                val file = File(tempDir, "test.json")
                file.writeText(
                    """
                    {
                        "@context": "https://schema.org",
                        "@type": "Person",
                        "name": "John Doe"
                    }
                    """.trimIndent(),
                )

                // Test processComponentLibrary method - it should handle the failure
                Seeder.processComponentLibrary(tempDir.absolutePath)

                coVerify {
                    TemplateService.storeTemplate("test", any(), any())
                }

                verify { mockLogger.error(any()) }
            } finally {
                tempDir.deleteRecursively()
            }
        }
}
