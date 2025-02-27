package storage

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kcl.seg.rtt.utils.environment.EnvironmentLoader
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TestStorageService {
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `storeFile uses local storage when LOCAL_STORAGE is true`() =
        runBlocking {
            mockkObject(EnvironmentLoader)
            coEvery { EnvironmentLoader.get("LOCAL_STORAGE") } returns "true"
            mockkObject(LocalStorage)
            val localPath = "localDir"
            val key = "test.txt"
            val dummyFile = File.createTempFile("dummy", ".txt").apply { writeText("dummy content") }
            val expectedLocalPath = "$localPath/$key"
            coEvery { LocalStorage.storeFile(localPath, key, dummyFile) } returns expectedLocalPath

            val result = StorageService.storeFile(bucket = null, path = localPath, key = key, file = dummyFile)
            assertEquals(expectedLocalPath, result)
            coVerify { LocalStorage.storeFile(localPath, key, dummyFile) }
        }

    @Test
    fun `storeFile uses remote storage when LOCAL_STORAGE is false`() =
        runBlocking {
            mockkObject(EnvironmentLoader)
            coEvery { EnvironmentLoader.get("LOCAL_STORAGE") } returns "false"
            mockkObject(S3Storage)
            val bucket = "remoteBucket"
            val key = "test.txt"
            val dummyFile = File.createTempFile("dummy", ".txt").apply { writeText("dummy content") }
            val expectedS3Path = "s3://$bucket/$key"
            coEvery { S3Storage.storeFile(bucket, key, dummyFile) } returns expectedS3Path

            val result = StorageService.storeFile(bucket = bucket, path = null, key = key, file = dummyFile)
            assertEquals(expectedS3Path, result)
            coVerify { S3Storage.storeFile(bucket, key, dummyFile) }
        }

    @Test
    fun `storeFile throws exception in local mode if path is missing`(): Unit =
        runBlocking {
            mockkObject(EnvironmentLoader)
            coEvery { EnvironmentLoader.get("LOCAL_STORAGE") } returns "true"
            val dummyFile = File.createTempFile("dummy", ".txt")
            assertFailsWith<IllegalArgumentException> {
                runBlocking {
                    StorageService.storeFile(bucket = null, path = null, key = "test.txt", file = dummyFile)
                }
            }
        }

    @Test
    fun `storeFile throws exception in remote mode if bucket is missing`(): Unit =
        runBlocking {
            mockkObject(EnvironmentLoader)
            coEvery { EnvironmentLoader.get("LOCAL_STORAGE") } returns "false"
            val dummyFile = File.createTempFile("dummy", ".txt")
            assertFailsWith<IllegalArgumentException> {
                runBlocking {
                    StorageService.storeFile(bucket = null, path = null, key = "test.txt", file = dummyFile)
                }
            }
        }

    @Test
    fun `getFile uses local storage when LOCAL_STORAGE is true`() =
        runBlocking {
            mockkObject(EnvironmentLoader)
            coEvery { EnvironmentLoader.get("LOCAL_STORAGE") } returns "true"
            mockkObject(LocalStorage)
            val localFilePath = "localDir/test.txt"
            val expectedBytes = "local file content".toByteArray()
            coEvery { LocalStorage.getFile(localFilePath) } returns expectedBytes

            val result = StorageService.getFile(path = localFilePath, bucket = null, key = null)
            assertContentEquals(expectedBytes, result!!)
            coVerify { LocalStorage.getFile(localFilePath) }
        }

    @Test
    fun `getFile uses remote storage when LOCAL_STORAGE is false`() =
        runBlocking {
            mockkObject(EnvironmentLoader)
            coEvery { EnvironmentLoader.get("LOCAL_STORAGE") } returns "false"
            mockkObject(S3Storage)
            val bucket = "remoteBucket"
            val key = "test.txt"
            val expectedBytes = "remote file content".toByteArray()
            coEvery { S3Storage.getFile(bucket, key) } returns expectedBytes

            val result = StorageService.getFile(path = null, bucket = bucket, key = key)
            assertContentEquals(expectedBytes, result!!)
            coVerify { S3Storage.getFile(bucket, key) }
        }

    @Test
    fun `getFile throws exception in local mode if path is missing`(): Unit =
        runBlocking {
            mockkObject(EnvironmentLoader)
            coEvery { EnvironmentLoader.get("LOCAL_STORAGE") } returns "true"
            assertFailsWith<IllegalArgumentException> {
                runBlocking {
                    StorageService.getFile(path = null, bucket = null, key = null)
                }
            }
        }

    @Test
    fun `getFile throws exception in remote mode if bucket is missing`(): Unit =
        runBlocking {
            mockkObject(EnvironmentLoader)
            coEvery { EnvironmentLoader.get("LOCAL_STORAGE") } returns "false"
            assertFailsWith<IllegalArgumentException> {
                runBlocking {
                    StorageService.getFile(path = null, bucket = null, key = "test.txt")
                }
            }
        }

    @Test
    fun `getFile throws exception in remote mode if key is missing`(): Unit =
        runBlocking {
            mockkObject(EnvironmentLoader)
            coEvery { EnvironmentLoader.get("LOCAL_STORAGE") } returns "false"
            assertFailsWith<IllegalArgumentException> {
                runBlocking {
                    StorageService.getFile(path = null, bucket = "remoteBucket", key = null)
                }
            }
        }

    @Test
    fun `deleteFile uses local storage when LOCAL_STORAGE is true`() =
        runBlocking {
            mockkObject(EnvironmentLoader)
            coEvery { EnvironmentLoader.get("LOCAL_STORAGE") } returns "true"
            mockkObject(LocalStorage)
            val localFilePath = "localDir/test.txt"
            coEvery { LocalStorage.deleteFile(localFilePath) } returns true

            val result = StorageService.deleteFile(path = localFilePath, bucket = null, key = null)
            assertTrue(result)
            coVerify { LocalStorage.deleteFile(localFilePath) }
        }

    @Test
    fun `deleteFile uses remote storage when LOCAL_STORAGE is false`() =
        runBlocking {
            mockkObject(EnvironmentLoader)
            coEvery { EnvironmentLoader.get("LOCAL_STORAGE") } returns "false"
            mockkObject(S3Storage)
            val bucket = "remoteBucket"
            val key = "test.txt"
            coEvery { S3Storage.deleteFile(bucket, key) } returns true

            val result = StorageService.deleteFile(path = null, bucket = bucket, key = key)
            assertTrue(result)
            coVerify { S3Storage.deleteFile(bucket, key) }
        }

    @Test
    fun `deleteFile throws exception in local mode if path is missing`(): Unit =
        runBlocking {
            mockkObject(EnvironmentLoader)
            coEvery { EnvironmentLoader.get("LOCAL_STORAGE") } returns "true"
            assertFailsWith<IllegalArgumentException> {
                runBlocking {
                    StorageService.deleteFile(path = null, bucket = null, key = null)
                }
            }
        }

    @Test
    fun `deleteFile throws exception in remote mode if bucket is missing`(): Unit =
        runBlocking {
            mockkObject(EnvironmentLoader)
            coEvery { EnvironmentLoader.get("LOCAL_STORAGE") } returns "false"
            assertFailsWith<IllegalArgumentException> {
                runBlocking {
                    StorageService.deleteFile(path = null, bucket = null, key = "test.txt")
                }
            }
        }

    @Test
    fun `deleteFile throws exception in remote mode if key is missing`(): Unit =
        runBlocking {
            mockkObject(EnvironmentLoader)
            coEvery { EnvironmentLoader.get("LOCAL_STORAGE") } returns "false"
            assertFailsWith<IllegalArgumentException> {
                runBlocking {
                    StorageService.deleteFile(path = null, bucket = "remoteBucket", key = null)
                }
            }
        }

    @Test
    fun `storeFile returns empty string when storage location is unknown (else branch)`() =
        runBlocking {
            mockkObject(StorageService)
            every { StorageService["getStorageLocation"]() } returns "other"

            val dummyFile = File.createTempFile("dummy", ".txt")
            val result =
                StorageService.storeFile(bucket = "dummyBucket", path = "dummyPath", key = "dummyKey", file = dummyFile)
            assertEquals("", result)
        }

    @Test
    fun `getFile returns null when storage location is unknown (else branch)`() =
        runBlocking {
            mockkObject(StorageService)
            every { StorageService["getStorageLocation"]() } returns "other"

            val result = StorageService.getFile(path = "dummyPath", bucket = "dummyBucket", key = "dummyKey")
            assertNull(result)
        }

    @Test
    fun `deleteFile returns false when storage location is unknown (else branch)`() =
        runBlocking {
            mockkObject(StorageService)
            every { StorageService["getStorageLocation"]() } returns "other"

            val result = StorageService.deleteFile(path = "dummyPath", bucket = "dummyBucket", key = "dummyKey")
            assertFalse(result)
        }
}
