package prompting.helpers.templates

import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import prompting.exceptions.TemplateRetrievalException
import utils.environment.EnvironmentLoader
import utils.storage.StorageService
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists

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
    fun `test parseS3Url with valid URL`() =
        runBlocking {
            // Arrange
            val url = "https://test-bucket.s3.amazonaws.com/path/to/file.txt"

            // Act
            val (bucket, key) =
                TemplateStorageUtils::class.java
                    .getDeclaredMethod("parseS3Url", String::class.java)
                    .apply { isAccessible = true }
                    .invoke(TemplateStorageUtils, url) as Pair<String, String>

            // Assert
            assertEquals("test-bucket", bucket)
            assertEquals("path/to/file.txt", key)
        }

    @Test
    fun `test parseS3Url with invalid URL format throws exception`() =
        runBlocking {
            // Arrange
            val url = "invalid-url"
            val parseS3Url =
                TemplateStorageUtils::class.java
                    .getDeclaredMethod("parseS3Url", String::class.java)
                    .apply { isAccessible = true }

            // Act & Assert
            val exception =
                assertThrows(java.lang.reflect.InvocationTargetException::class.java) {
                    parseS3Url.invoke(TemplateStorageUtils, url)
                }
            assertTrue(exception.cause is TemplateRetrievalException)
            assertEquals("Invalid S3 URL", exception.cause?.message)
        }

    @Test
    fun `test parseS3Url with URL missing bucket throws exception`() =
        runBlocking {
            // Arrange
            val url = "https://s3.amazonaws.com/path/to/file.txt" // Missing bucket in URL
            val parseS3Url =
                TemplateStorageUtils::class.java
                    .getDeclaredMethod("parseS3Url", String::class.java)
                    .apply { isAccessible = true }

            // Act & Assert
            val exception =
                assertThrows(java.lang.reflect.InvocationTargetException::class.java) {
                    parseS3Url.invoke(TemplateStorageUtils, url)
                }
            assertTrue(exception.cause is TemplateRetrievalException)
            assertEquals("Invalid S3 URL", exception.cause?.message)
        }

    @Test
    fun `test parseS3Url with URL missing key throws exception`() =
        runBlocking {
            // Arrange
            val url = "https://test-bucket.s3.amazonaws.com/" // Missing key in URL
            val parseS3Url =
                TemplateStorageUtils::class.java
                    .getDeclaredMethod("parseS3Url", String::class.java)
                    .apply { isAccessible = true }

            // Act & Assert
            val exception =
                assertThrows(java.lang.reflect.InvocationTargetException::class.java) {
                    parseS3Url.invoke(TemplateStorageUtils, url)
                }
            assertTrue(exception.cause is TemplateRetrievalException)
            assertEquals("Invalid S3 URL", exception.cause?.message)
        }

    @Test
    fun `test parseS3Url with different valid URL format`() =
        runBlocking {
            // Arrange
            val url = "https://test-bucket.s3.amazonaws.com/nested/path/to/file.txt"
            val parseS3Url =
                TemplateStorageUtils::class.java
                    .getDeclaredMethod("parseS3Url", String::class.java)
                    .apply { isAccessible = true }

            // Act
            val (bucket, key) = parseS3Url.invoke(TemplateStorageUtils, url) as Pair<String, String>

            // Assert
            assertEquals("test-bucket", bucket)
            assertEquals("nested/path/to/file.txt", key)
        }

    @Test
    fun `test parseS3Url with malformed URL throws exception`() =
        runBlocking {
            // Arrange
            // This URL doesn't match the expected pattern for bucket extraction
            val url = "https://test-bucket.example.com/path/to/file.txt"

            val parseS3Url =
                TemplateStorageUtils::class.java
                    .getDeclaredMethod("parseS3Url", String::class.java)
                    .apply { isAccessible = true }

            // Act & Assert
            val exception =
                assertThrows(java.lang.reflect.InvocationTargetException::class.java) {
                    parseS3Url.invoke(TemplateStorageUtils, url)
                }
            assertTrue(exception.cause is TemplateRetrievalException)
            assertEquals("Invalid S3 URL", exception.cause?.message)
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
