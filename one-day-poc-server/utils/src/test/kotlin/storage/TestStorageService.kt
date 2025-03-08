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
    fun `storeFileLocally delegates to LocalStorage storeFile`() =
        runBlocking {
            mockkObject(LocalStorage)
            val localPath = "localDir"
            val key = "test.txt"
            val dummyFile = File.createTempFile("dummy", ".txt").apply { writeText("dummy content") }
            val expectedLocalPath = "$localPath/$key"
            coEvery { LocalStorage.storeFile(localPath, key, dummyFile) } returns expectedLocalPath

            val result = StorageService.storeFileLocal(path = localPath, key = key, file = dummyFile)
            assertEquals(expectedLocalPath, result)
            coVerify { LocalStorage.storeFile(localPath, key, dummyFile) }
        }

    @Test
    fun `storeFileRemotely delegates to S3Storage storeFile`() =
        runBlocking {
            mockkObject(S3Storage)
            val bucket = "remoteBucket"
            val key = "test.txt"
            val dummyFile = File.createTempFile("dummy", ".txt").apply { writeText("dummy content") }
            val expectedS3Path = "s3://$bucket/$key"
            coEvery { S3Storage.storeFile(bucket, key, dummyFile) } returns expectedS3Path

            val result = StorageService.storeFileRemote(bucket = bucket, key = key, file = dummyFile)
            assertEquals(expectedS3Path, result)
            coVerify { S3Storage.storeFile(bucket, key, dummyFile) }
        }

    @Test
    fun `getFileLocally delegates to LocalStorage getFile`() =
        runBlocking {
            mockkObject(LocalStorage)
            val localFilePath = "localDir/test.txt"
            val expectedBytes = "local file content".toByteArray()
            coEvery { LocalStorage.getFile(localFilePath) } returns expectedBytes

            val result = StorageService.getFileLocal(path = localFilePath)
            assertContentEquals(expectedBytes, result!!)
            coVerify { LocalStorage.getFile(localFilePath) }
        }

    @Test
    fun `getFileRemotely delegates to S3Storage getFile`() =
        runBlocking {
            mockkObject(S3Storage)
            val bucket = "remoteBucket"
            val key = "test.txt"
            val expectedBytes = "remote file content".toByteArray()
            coEvery { S3Storage.getFile(bucket, key) } returns expectedBytes

            val result = StorageService.getFileRemote(bucket = bucket, key = key)
            assertContentEquals(expectedBytes, result!!)
            coVerify { S3Storage.getFile(bucket, key) }
        }

    @Test
    fun `deleteFileLocally delegates to LocalStorage deleteFile`() =
        runBlocking {
            mockkObject(LocalStorage)
            val localFilePath = "localDir/test.txt"
            coEvery { LocalStorage.deleteFile(localFilePath) } returns true

            val result = StorageService.deleteFileLocal(path = localFilePath)
            assertTrue(result)
            coVerify { LocalStorage.deleteFile(localFilePath) }
        }

    @Test
    fun `deleteFileRemotely delegates to S3Storage deleteFile`() =
        runBlocking {
            mockkObject(S3Storage)
            val bucket = "remoteBucket"
            val key = "test.txt"
            coEvery { S3Storage.deleteFile(bucket, key) } returns true

            val result = StorageService.deleteFileRemote(bucket = bucket, key = key)
            assertTrue(result)
            coVerify { S3Storage.deleteFile(bucket, key) }
        }
}