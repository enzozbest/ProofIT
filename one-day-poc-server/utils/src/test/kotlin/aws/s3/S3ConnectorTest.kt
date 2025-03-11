package aws.s3

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.*
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.smithy.kotlin.runtime.time.Instant
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kcl.seg.rtt.utils.aws.S3Manager
import kcl.seg.rtt.utils.aws.S3Service
import kcl.seg.rtt.utils.environment.EnvironmentLoader
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3ConnectorTest {
    private lateinit var mockS3Client: S3Client
    private lateinit var mockS3Manager: S3Manager
    private val bucketName = "testBucket"
    private val testFile =
        File.createTempFile("test", ".txt").apply {
            writeText("Test!")
        }

    private val testKey = "testKey"

    @BeforeAll
    fun setup() {
        EnvironmentLoader.reset()
        File("test.env").writeText("AWS_REGION=eu-west-2")
        EnvironmentLoader.loadEnvironmentFile("test.env")
        mockS3Client = mockk<S3Client>(relaxed = true)
        mockS3Manager = mockk<S3Manager>(relaxed = true)
        coEvery { mockS3Manager.getClient() } returns mockS3Client
        coEvery { mockS3Manager.assumeS3SafeRole() } returns
            Credentials(
                "accessKey",
                "secretKey",
                "sessionToken",
                Instant.now().plus(Duration.parse("10h")),
            )
        S3Service.init(mockS3Manager)
    }

    @AfterAll
    fun teardown() {
        testFile.delete()
    }

    @Test
    fun `Test uploads file`() =
        runBlocking {
            coEvery { mockS3Client.putObject(any<PutObjectRequest>()) } returns PutObjectResponse {}
            val url = S3Service.uploadFile(bucketName, testFile, testKey)
            assertEquals("https://$bucketName.s3.amazonaws.com/$testKey", url)
            coVerify { mockS3Client.putObject(any<PutObjectRequest>()) }
        }

    @Test
    fun `Test empty string returned if upload file fails`() =
        runBlocking {
            coEvery { mockS3Client.putObject(any<PutObjectRequest>()) } throws RuntimeException("Failed to upload file")
            val response = S3Service.uploadFile(bucketName, testFile, testKey)
            assertEquals("", response)
            coVerify { mockS3Client.putObject(any<PutObjectRequest>()) }
        }

    @Test
    fun `Test retrieves file contents successfully`() =
        runBlocking {
            val response = GetObjectResponse { body = ByteStream.fromBytes("Test content".encodeToByteArray()) }
            coEvery {
                mockS3Client.getObject(any<GetObjectRequest>(), any<suspend (GetObjectResponse) -> String>())
            } coAnswers {
                secondArg<suspend (GetObjectResponse) -> String>().invoke(response)
            }
            val content = S3Service.getFile(bucketName, testKey)
            assertContentEquals("Test content".encodeToByteArray(), content)
            coVerify { mockS3Client.getObject(any<GetObjectRequest>(), any()) }
        }

    @Test
    fun `Test returns null if get file fails`(): Unit =
        runBlocking {
            coEvery {
                mockS3Client.getObject(any<GetObjectRequest>(), any<suspend (GetObjectResponse) -> String>())
            } throws RuntimeException("Failed to get file")

            val response = S3Service.getFile(bucketName, testKey)
            assertNull(response)
            coVerify { mockS3Client.getObject(any<GetObjectRequest>(), any()) }
        }

    @Test
    fun `Test retrieves file fails if response body is null`() =
        runBlocking {
            val response = GetObjectResponse { body = null }
            coEvery {
                mockS3Client.getObject(any<GetObjectRequest>(), any<suspend (GetObjectResponse) -> String>())
            } coAnswers {
                secondArg<suspend (GetObjectResponse) -> String>().invoke(response)
            }
            val content = S3Service.getFile(bucketName, testKey)
            assertNull(content)
            coVerify { mockS3Client.getObject(any<GetObjectRequest>(), any()) }
        }

    @Test
    fun `Test deletes object successfully`() =
        runBlocking {
            coEvery { mockS3Client.deleteObject(any<DeleteObjectRequest>()) } returns DeleteObjectResponse {}

            val result = S3Service.deleteObject(bucketName, testKey)

            assertTrue(result)
            coVerify { mockS3Client.deleteObject(any<DeleteObjectRequest>()) }
        }

    @Test
    fun `Test false if delete file fails`(): Unit =
        runBlocking {
            coEvery { mockS3Client.deleteObject(any<DeleteObjectRequest>()) } throws RuntimeException("Failed to delete file")

            val response = S3Service.deleteObject(bucketName, testKey)
            assertFalse(response)
            coVerify { mockS3Client.deleteObject(any<DeleteObjectRequest>()) }
        }

    @Test
    fun `Test listBucket returns list of objects`() =
        runBlocking {
            val mockObjects =
                listOf(
                    Object { key = "file1.txt" },
                    Object { key = "file2.txt" },
                )
            val response = ListObjectsV2Response { contents = mockObjects }
            coEvery { mockS3Client.listObjectsV2(any<ListObjectsV2Request>()) } returns response

            val objects = S3Service.listBucket(bucketName)

            assertEquals(listOf("file1.txt", "file2.txt"), objects)
            coVerify { mockS3Client.listObjectsV2(any<ListObjectsV2Request>()) }
        }

    @Test
    fun `Test empty list returned if list bucket fails`(): Unit =
        runBlocking {
            coEvery { mockS3Client.listObjectsV2(any<ListObjectsV2Request>()) } throws RuntimeException("Failed to list bucket")

            val response = S3Service.listBucket(bucketName)
            assertEquals(emptyList(), response)
            coVerify { mockS3Client.listObjectsV2(any<ListObjectsV2Request>()) }
        }

    @Test
    fun `Test listBucket returns empty list if response is null`() =
        runBlocking {
            val response = ListObjectsV2Response { contents = null }
            coEvery { mockS3Client.listObjectsV2(any<ListObjectsV2Request>()) } returns response

            val objects = S3Service.listBucket(bucketName)
            assertEquals(emptyList(), objects)
            coVerify { mockS3Client.listObjectsV2(any<ListObjectsV2Request>()) }
        }
}
