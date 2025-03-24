package storage

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import utils.aws.S3Service
import utils.storage.S3Storage
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestS3Storage {
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `storeFile returns S3 key when upload succeeds`() =
        runBlocking {
            mockkObject(S3Service)
            val bucket = "test-bucket"
            val key = "test-key"
            val tempFile = File.createTempFile("test", ".txt").apply { writeText("Sample content") }
            val expectedS3Key = "s3://$bucket/$key"
            coEvery { S3Service.uploadFile(bucket, tempFile, key) } returns expectedS3Key

            val result = S3Storage.storeFile(bucket, key, tempFile)
            assertEquals(expectedS3Key, result)
            coVerify { S3Service.uploadFile(bucket, tempFile, key) }
        }

    @Test
    fun `storeFile returns empty string when upload fails`() =
        runBlocking {
            mockkObject(S3Service)
            val bucket = "test-bucket"
            val key = "test-key"
            val tempFile = File.createTempFile("test", ".txt").apply { writeText("Sample content") }
            coEvery { S3Service.uploadFile(bucket, tempFile, key) } returns ""

            val result = S3Storage.storeFile(bucket, key, tempFile)
            assertEquals("", result)
            coVerify { S3Service.uploadFile(bucket, tempFile, key) }
        }

    @Test
    fun `getFile returns ByteArray when file is retrieved successfully`() =
        runBlocking {
            mockkObject(S3Service)
            val bucket = "test-bucket"
            val key = "test-key"
            val expectedContent = "Test content".toByteArray()
            coEvery { S3Service.getFile(bucket, key) } returns expectedContent

            val result = S3Storage.getFile(bucket, key)
            assertNotNull(result, "Expected a non-null ByteArray")
            assertContentEquals(expectedContent, result)
            coVerify { S3Service.getFile(bucket, key) }
        }

    @Test
    fun `getFile returns null when file retrieval fails`() =
        runBlocking {
            mockkObject(S3Service)
            val bucket = "test-bucket"
            val key = "test-key"
            coEvery { S3Service.getFile(bucket, key) } returns null

            val result = S3Storage.getFile(bucket, key)
            assertNull(result, "Expected null when S3Service.getFile returns null")
            coVerify { S3Service.getFile(bucket, key) }
        }

    @Test
    fun `deleteFile returns true when deletion succeeds`() =
        runBlocking {
            mockkObject(S3Service)
            val bucket = "test-bucket"
            val key = "test-key"
            coEvery { S3Service.deleteObject(bucket, key) } returns true

            val result = S3Storage.deleteFile(bucket, key)
            assertTrue(result, "Expected deleteFile to return true on successful deletion")
            coVerify { S3Service.deleteObject(bucket, key) }
        }

    @Test
    fun `deleteFile returns false when deletion fails`() =
        runBlocking {
            mockkObject(S3Service)
            val bucket = "test-bucket"
            val key = "test-key"
            coEvery { S3Service.deleteObject(bucket, key) } returns false

            val result = S3Storage.deleteFile(bucket, key)
            assertFalse(result, "Expected deleteFile to return false when deletion fails")
            coVerify { S3Service.deleteObject(bucket, key) }
        }
}
