package aws.s3

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.sts.StsClient
import aws.sdk.kotlin.services.sts.model.AssumeRoleRequest
import aws.sdk.kotlin.services.sts.model.AssumeRoleResponse
import aws.sdk.kotlin.services.sts.model.Credentials
import aws.smithy.kotlin.runtime.time.Instant
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kcl.seg.rtt.utils.aws.S3Manager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import kotlin.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3RoleTest {
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
    fun `Test assumeS3SafeRole returns valid credentials`() =
        runBlocking {
            val credentials = s3Manager.assumeS3SafeRole()
            assertNotNull(credentials)
            assertEquals("accessKeyId", credentials.accessKeyId)
            assertEquals("secretAccessKey", credentials.secretAccessKey)
            assertEquals("sessionToken", credentials.sessionToken)
            coVerify { mockStsClient.assumeRole(any<AssumeRoleRequest>()) }
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
    fun `Test assumeS3SafeRole returns empty credentials when config is null`() =
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
    fun `Test assumeS3SafeRole returns empty credentials when role is not a JsonPrimitive`() =
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
        val s3Config = buildJsonObject { }
        val client = s3Manager.buildClient(s3Config, dummyCredentialsProvider)
        assertEquals("eu-west-2", client.region)
        assertEquals(dummyCredentialsProvider, client.credentialsProvider)
    }

    @Test
    fun `buildClient returns S3Client with default region when region is malformed in s3Config`() {
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
        val client = s3Manager.buildClient(null, dummyCredentialsProvider)
        assertNull(client.region)
        assertEquals(dummyCredentialsProvider, client.credentialsProvider)
    }
}
