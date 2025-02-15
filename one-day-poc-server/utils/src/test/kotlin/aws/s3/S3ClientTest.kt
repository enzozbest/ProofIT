package aws.s3

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.sts.StsClient
import aws.sdk.kotlin.services.sts.model.AssumeRoleResponse
import aws.sdk.kotlin.services.sts.model.Credentials
import aws.smithy.kotlin.runtime.time.Instant
import io.mockk.*
import kcl.seg.rtt.utils.aws.S3Manager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import kotlin.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3ClientTest {
    private lateinit var mockStsClient: StsClient
    private lateinit var mockS3Client: S3Client
    private lateinit var s3Manager: S3Manager
    private val dummyCredentialsProvider =
        StaticCredentialsProvider {
            accessKeyId = "dummyAccessKey"
            secretAccessKey = "dummySecretKey"
        }

    @BeforeAll
    fun setup() {
        mockStsClient = mockk<StsClient>(relaxed = true)
        mockS3Client = mockk<S3Client>(relaxed = true)

        val mockConfig: JsonObject =
            buildJsonObject {
                put("role", "arn:aws:iam::123456789012:role/TestRole")
            }
        coEvery {
            mockStsClient.assumeRole(any())
        } returns
            AssumeRoleResponse {
                credentials =
                    Credentials {
                        accessKeyId = "accessKeyId"
                        secretAccessKey = "secretAccessKey"
                        sessionToken = "sessionToken"
                        expiration = Instant.now().plus(Duration.parse("10h"))
                    }
            }
        s3Manager = S3Manager(mockConfig, mockStsClient)
    }

    @AfterAll
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `Test returns an S3Client instance when it is not initialised`() =
        runBlocking {
            val mockCredentials =
                aws.smithy.kotlin.runtime.auth.awscredentials.Credentials(
                    "mockAccessKey",
                    "mockSecretKey",
                    "mockSessionToken",
                )
            coEvery { s3Manager.assumeS3SafeRole() } returns mockCredentials
            mockkObject(S3Client.Companion)
            every { S3Client.invoke(any()) } returns mockS3Client

            val s3Client = s3Manager.getClient()
            assertNotNull(s3Client)
            assertSame(mockS3Client, s3Client)
            coVerify { s3Manager.assumeS3SafeRole() }
        }

    @Test
    fun `Test returns an S3Client instance when it is initialised`() =
        runBlocking {
            val mockCredentials =
                aws.smithy.kotlin.runtime.auth.awscredentials.Credentials(
                    "mockAccessKey",
                    "mockSecretKey",
                    "mockSessionToken",
                )
            coEvery { s3Manager.assumeS3SafeRole() } returns mockCredentials
            mockkObject(S3Client.Companion)
            every { S3Client.invoke(any()) } returns mockS3Client

            val init = s3Manager.getClient() // Force initialisation
            assertNotNull(init)
            assertSame(mockS3Client, init)
            val s3clinent = s3Manager.getClient() // Call the method again with the initialised instance
            assertNotNull(s3clinent)
            assertSame(mockS3Client, s3clinent)
            assertSame(init, s3clinent)
            coVerify { s3Manager.assumeS3SafeRole() }
        }

    @Test
    fun `Test returns empty credentials when config is incomplete`() =
        runBlocking {
            val s3ManagerWithInvalidConfig = S3Manager(buildJsonObject { }, mockStsClient)
            val s3client = s3ManagerWithInvalidConfig.getClient()
            val result = runBlocking { s3ManagerWithInvalidConfig.assumeS3SafeRole() }
            assertEquals(
                aws.smithy.kotlin.runtime.auth.awscredentials
                    .Credentials("", "", ""),
                result,
            )
        }
}
