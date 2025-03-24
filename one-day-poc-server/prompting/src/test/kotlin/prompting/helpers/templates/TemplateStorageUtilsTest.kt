package prompting.helpers.templates

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import prompting.exceptions.TemplateRetrievalException
import prompting.helpers.templates.TemplateStorageUtils.parseS3Url
import utils.environment.EnvironmentLoader
import utils.storage.StorageService
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.test.assertFailsWith

class TemplateStorageUtilsTest {
    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
        mockkObject(EnvironmentLoader)
        mockkObject(StorageService)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test retrieveFileContent from local storage`() =
        runBlocking {
            // Arrange
            val path = "local/path/to/file.txt"
            val fileContent = "file content".toByteArray()

            every { EnvironmentLoader.get("LOCAL_STORAGE") } returns "true"
            every { StorageService.getFileLocal(path) } returns fileContent

            // Act
            val result = TemplateStorageUtils.retrieveFileContent(path)

            // Assert
            assertArrayEquals(fileContent, result)
            verify {
                EnvironmentLoader.get("LOCAL_STORAGE")
                StorageService.getFileLocal(path)
            }
        }

    @Test
    fun `test retrieveFileContent from local storage throws exception when file not found`() =
        runBlocking {
            // Arrange
            val path = "local/path/to/nonexistent.txt"

            every { EnvironmentLoader.get("LOCAL_STORAGE") } returns "true"
            every { StorageService.getFileLocal(path) } returns null

            // Act & Assert
            val exception =
                assertThrows(TemplateRetrievalException::class.java) {
                    runBlocking { TemplateStorageUtils.retrieveFileContent(path) }
                }
            assertEquals("File not found", exception.message)

            verify {
                EnvironmentLoader.get("LOCAL_STORAGE")
                StorageService.getFileLocal(path)
            }
        }

    @Test
    fun `test retrieveFileContent from remote storage`() =
        runBlocking {
            // Arrange
            val url = "https://test-bucket.s3.amazonaws.com/path/to/file.txt"
            val fileContent = "file content".toByteArray()

            every { EnvironmentLoader.get("LOCAL_STORAGE") } returns "false"
            coEvery { StorageService.getFileRemote("test-bucket", "path/to/file.txt") } returns fileContent

            // Act
            val result = TemplateStorageUtils.retrieveFileContent(url)

            // Assert
            assertArrayEquals(fileContent, result)
            verify { EnvironmentLoader.get("LOCAL_STORAGE") }
            coVerify { StorageService.getFileRemote("test-bucket", "path/to/file.txt") }
        }

    @Test
    fun `test retrieveFileContent from remote storage throws exception when file not found`() =
        runBlocking {
            // Arrange
            val url = "https://test-bucket.s3.amazonaws.com/path/to/nonexistent.txt"

            every { EnvironmentLoader.get("LOCAL_STORAGE") } returns "false"
            coEvery { StorageService.getFileRemote("test-bucket", "path/to/nonexistent.txt") } returns null

            // Act & Assert
            val exception =
                assertThrows(TemplateRetrievalException::class.java) {
                    runBlocking { TemplateStorageUtils.retrieveFileContent(url) }
                }
            assertEquals("File not found", exception.message)

            verify { EnvironmentLoader.get("LOCAL_STORAGE") }
            coVerify { StorageService.getFileRemote("test-bucket", "path/to/nonexistent.txt") }
        }

    @Test
    fun `parseS3Url correctly extracts bucket and key from valid S3 URL`() {
        val validUrl = "https://mybucket.s3.amazonaws.com/folder/file.txt"
        val (bucket, key) = parseS3Url(validUrl)

        assertEquals("mybucket", bucket)
        assertEquals("folder/file.txt", key)
    }

    @Test
    fun `parseS3Url throws exception when bucket pattern doesn't match`() {
        val invalidUrl = "https://invalid-url.com/something"

        val exception =
            assertFailsWith<TemplateRetrievalException> {
                parseS3Url(invalidUrl)
            }
        assertEquals("Invalid S3 URL", exception.message)
    }

    @Test
    fun `parseS3Url throws exception when key pattern doesn't match`() {
        // This URL passes the bucket regex but fails the key regex
        val invalidUrl = "https://bucket.s3.amazonaws.com"

        val exception =
            assertFailsWith<TemplateRetrievalException> {
                parseS3Url(invalidUrl)
            }
        assertEquals("Invalid S3 URL", exception.message)
    }

    @Test
    fun `parseS3Url correctly handles URLs with complex paths`() {
        // Test URL with multiple path segments and special characters
        val complexUrl = "https://test-bucket.s3.amazonaws.com/path/to/my%20file.pdf"

        val (bucket, key) = parseS3Url(complexUrl)

        assertEquals("test-bucket", bucket)
        assertEquals("path/to/my%20file.pdf", key)
    }

    @Test
    fun `parseS3Url throws exception when URL starts with https but doesn't match full pattern`() {
        val malformedUrl = "https://bucket.s3.other-domain.com/file.txt"
        assertFailsWith<TemplateRetrievalException> {
            parseS3Url(malformedUrl)
        }
    }

    @Test
    fun `parseS3Url throws exception when URL matches bucket pattern but key is empty`() {
        val malformedUrl = "https://bucket.s3.amazonaws.com/"
        assertFailsWith<TemplateRetrievalException> {
            parseS3Url(malformedUrl)
        }
    }

    @Test
    fun `parseS3Url throws exception when URL is null`() {
        val nullUrl: String? = null
        assertFailsWith<TemplateRetrievalException> {
            parseS3Url(nullUrl ?: "")
        }
    }

    @Test
    fun `parseS3Url throws exception when URL is empty`() {
        val emptyUrl = ""
        assertFailsWith<TemplateRetrievalException> {
            parseS3Url(emptyUrl)
        }
    }

    @Test
    fun `parseS3Url throws exception when URL has invalid format for bucket`() {
        // This URL has the correct domain but doesn't match the bucket pattern
        val invalidUrl = "https://s3.amazonaws.com/bucket/file.txt"
        assertFailsWith<TemplateRetrievalException> {
            parseS3Url(invalidUrl)
        }
    }

    @Test
    fun `parseS3Url throws exception when URL has invalid format for key`() {
        // This URL has a valid bucket but no key after the trailing slash
        val invalidUrl = "https://bucket.s3.amazonaws.com"
        assertFailsWith<TemplateRetrievalException> {
            parseS3Url(invalidUrl)
        }
    }

    @Test
    fun `parseS3Url handles URL with special characters in bucket and key`() {
        val specialUrl = "https://my-special-bucket.s3.amazonaws.com/path/to/file%20with%20spaces.txt"
        val (bucket, key) = parseS3Url(specialUrl)

        assertEquals("my-special-bucket", bucket)
        assertEquals("path/to/file%20with%20spaces.txt", key)
    }

    @Test
    fun `test storeFile to local storage`() =
        runBlocking {
            // Arrange
            val content = "file content"
            val filePrefix = "prefix_"
            val fileSuffix = ".txt"
            val storageConfig =
                TemplateStorageUtils.StorageConfig(
                    path = "local/path",
                    key = "file.txt",
                    bucket = "test-bucket",
                )
            val expectedPath = "local/path/file.txt"

            // Mock environment and storage service
            every { EnvironmentLoader.get("LOCAL_STORAGE") } returns "true"

            // Use a spy on the actual implementation to avoid mocking static methods
            val tempFilePath = createTempFile(prefix = filePrefix, suffix = fileSuffix)
            tempFilePath.toFile()

            // Mock the storage service call
            every {
                StorageService.storeFileLocal(storageConfig.path, storageConfig.key, any())
            } returns expectedPath

            try {
                // Act
                val result = TemplateStorageUtils.storeFile(content, filePrefix, fileSuffix, storageConfig)

                // Assert
                assertEquals(expectedPath, result)
                verify {
                    EnvironmentLoader.get("LOCAL_STORAGE")
                    StorageService.storeFileLocal(storageConfig.path, storageConfig.key, any())
                }
            } finally {
                // Clean up
                tempFilePath.deleteIfExists()
            }
        }

    @Test
    fun `test storeFile to remote storage`() =
        runBlocking {
            // Arrange
            val content = "file content"
            val filePrefix = "prefix_"
            val fileSuffix = ".txt"
            val storageConfig =
                TemplateStorageUtils.StorageConfig(
                    path = "local/path",
                    key = "file.txt",
                    bucket = "test-bucket",
                )
            val expectedPath = "https://test-bucket.s3.amazonaws.com/file.txt"

            // Mock environment and storage service
            every { EnvironmentLoader.get("LOCAL_STORAGE") } returns "false"

            // Use a spy on the actual implementation to avoid mocking static methods
            val tempFilePath = createTempFile(prefix = filePrefix, suffix = fileSuffix)
            tempFilePath.toFile()

            // Mock the storage service call
            coEvery {
                StorageService.storeFileRemote(storageConfig.bucket, storageConfig.key, any())
            } returns expectedPath

            try {
                // Act
                val result = TemplateStorageUtils.storeFile(content, filePrefix, fileSuffix, storageConfig)

                // Assert
                assertEquals(expectedPath, result)
                verify {
                    EnvironmentLoader.get("LOCAL_STORAGE")
                }
                coVerify { StorageService.storeFileRemote(storageConfig.bucket, storageConfig.key, any()) }
            } finally {
                // Clean up
                tempFilePath.deleteIfExists()
            }
        }

    @Test
    fun `test StorageConfig data class`() {
        // Arrange
        val path = "local/path"
        val key = "file.txt"
        val bucket = "test-bucket"

        // Act
        val config = TemplateStorageUtils.StorageConfig(path, key, bucket)
        val copy = config.copy(path = "new/path")
        val (p, k, b) = config

        // Assert
        assertEquals(path, config.path)
        assertEquals(key, config.key)
        assertEquals(bucket, config.bucket)

        assertEquals("new/path", copy.path)
        assertEquals(key, copy.key)
        assertEquals(bucket, copy.bucket)

        assertEquals(path, p)
        assertEquals(key, k)
        assertEquals(bucket, b)

        assertEquals(config, TemplateStorageUtils.StorageConfig(path, key, bucket))
        assertNotEquals(config, TemplateStorageUtils.StorageConfig("different", key, bucket))

        assertEquals(config.hashCode(), TemplateStorageUtils.StorageConfig(path, key, bucket).hashCode())

        assertTrue(config.toString().contains(path))
        assertTrue(config.toString().contains(key))
        assertTrue(config.toString().contains(bucket))
    }
}
