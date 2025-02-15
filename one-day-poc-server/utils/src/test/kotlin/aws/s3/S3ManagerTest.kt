package aws.s3

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.sts.StsClient
import aws.sdk.kotlin.services.sts.model.AssumeRoleRequest
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
class S3ManagerTest {
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
    fun `Test assumeS3SafeRole returns credentials`() =
        runBlocking {
            val credentials = s3Manager.assumeS3SafeRole()
            assertNotNull(credentials)
            assertEquals("accessKeyId", credentials.accessKeyId)
            assertEquals("secretAccessKey", credentials.secretAccessKey)
            assertEquals("sessionToken", credentials.sessionToken)
            coVerify { mockStsClient.assumeRole(any<AssumeRoleRequest>()) }
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
    fun `Test assumeS3SafeRole handles assumeRole failure`() =
        runBlocking {
            coEvery { mockStsClient.assumeRole(any()) } throws RuntimeException("AWS STS failed")
            val exception =
                assertThrows<RuntimeException> {
                    runBlocking { s3Manager.assumeS3SafeRole() }
                }
            assertEquals("AWS STS failed", exception.message)
            coVerify { mockStsClient.assumeRole(any<AssumeRoleRequest>()) }
        }

    @Test
    fun `Test assumeS3SafeRole fails when config is null`() =
        runBlocking {
            coEvery { mockStsClient.assumeRole(any()) } throws RuntimeException("roleArn must not be null")
            val s3ManagerWithInvalidConfig = S3Manager(null, mockStsClient)

            val result = runBlocking { s3ManagerWithInvalidConfig.assumeS3SafeRole() }
            assertEquals(
                aws.smithy.kotlin.runtime.auth.awscredentials
                    .Credentials("", "", ""),
                result,
            )
        }

    @Test
    fun `Test assumeS3SafeRole returns empty credentials when config is incomplete`() =
        runBlocking {
            val s3ManagerWithInvalidConfig = S3Manager(buildJsonObject { }, mockStsClient)

            val result = runBlocking { s3ManagerWithInvalidConfig.assumeS3SafeRole() }
            assertEquals(
                aws.smithy.kotlin.runtime.auth.awscredentials
                    .Credentials("", "", ""),
                result,
            )
        }

    @Test
    fun `Test assumeS3SafeRole returns empty credentials when "role" is not a JsonPrimitive`() =
        runBlocking {
            val s3ManagerWithInvalidConfig =
                S3Manager(
                    buildJsonObject {
                        put("role", buildJsonObject { put("key", "value") })
                    },
                    mockStsClient,
                )

            val result = runBlocking { s3ManagerWithInvalidConfig.assumeS3SafeRole() }
            assertEquals(
                aws.smithy.kotlin.runtime.auth.awscredentials
                    .Credentials("", "", ""),
                result,
            )
        }

    @Test
    fun `buildClient returns S3Client with region from valid s3Config`() {
        val s3Config =
            buildJsonObject {
                put("region", JsonPrimitive("us-east-1"))
            }

        val client = s3Manager.buildClient(s3Config, dummyCredentialsProvider)

        assertEquals("us-east-1", client.region)
        assertEquals(dummyCredentialsProvider, client.credentialsProvider)
    }

    @Test
    fun `buildClient returns S3Client with default region when s3Config is missing region`() {
        // JSON with no "region" key.
        val s3Config = buildJsonObject { }

        val client = s3Manager.buildClient(s3Config, dummyCredentialsProvider)

        // Since the region key is missing the runCatching block fails and we fall back to "eu-west-2".
        assertEquals("eu-west-2", client.region)
        assertEquals(dummyCredentialsProvider, client.credentialsProvider)
    }

    @Test
    fun `buildClient returns S3Client with default region when region is malformed in s3Config`() {
        // Here the "region" key exists but its value is not a JsonPrimitive (so jsonPrimitive will fail).
        val s3Config =
            buildJsonObject {
                put("region", buildJsonObject { put("unexpected", JsonPrimitive("data")) })
            }

        val client = s3Manager.buildClient(s3Config, dummyCredentialsProvider)

        assertEquals("eu-west-2", client.region)
        assertEquals(dummyCredentialsProvider, client.credentialsProvider)
    }

    @Test
    fun `buildClient returns S3Client with default region when s3Config is null`() {
        val s3Config = null
        val client = s3Manager.buildClient(s3Config, dummyCredentialsProvider)
        assertEquals("eu-west-2", client.region)
        assertEquals(dummyCredentialsProvider, client.credentialsProvider)
    }

    @Test
    fun `buildClient returns S3Client with null region when s3Config is null`() {
        val client = s3Manager.buildClient(null, dummyCredentialsProvider)

        assertEquals("eu-west-2", client.region)
        assertEquals(dummyCredentialsProvider, client.credentialsProvider)
    }
}
