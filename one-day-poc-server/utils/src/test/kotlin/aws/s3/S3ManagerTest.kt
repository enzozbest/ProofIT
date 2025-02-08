package aws.s3

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.sts.StsClient
import aws.sdk.kotlin.services.sts.model.AssumeRoleRequest
import aws.sdk.kotlin.services.sts.model.AssumeRoleResponse
import aws.sdk.kotlin.services.sts.model.Credentials
import aws.smithy.kotlin.runtime.time.Instant
import io.mockk.*
import kcl.seg.rtt.utils.aws.S3Manager
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import kotlin.time.Duration


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class S3ManagerTest {
    private lateinit var mockStsClient: StsClient
    private lateinit var mockS3Client: S3Client
    private lateinit var s3Manager: S3Manager

    @BeforeAll
    fun setup() {
        mockStsClient = mockk<StsClient>(relaxed = true)
        mockS3Client = mockk<S3Client>(relaxed = true)

        val mockConfig: JsonObject = buildJsonObject {
            put("role", "arn:aws:iam::123456789012:role/TestRole")
        }
        coEvery {
            mockStsClient.assumeRole(any())
        } returns AssumeRoleResponse {
            credentials = Credentials {
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
    fun `Test assumeS3SafeRole returns credentials`() = runBlocking {
        val credentials = s3Manager.assumeS3SafeRole()
        assertNotNull(credentials)
        assertEquals("accessKeyId", credentials.accessKeyId)
        assertEquals("secretAccessKey", credentials.secretAccessKey)
        assertEquals("sessionToken", credentials.sessionToken)
        coVerify { mockStsClient.assumeRole(any<AssumeRoleRequest>()) }
    }

    @Test
    fun `Test returns an S3Client instance`() = runBlocking {
        val mockCredentials = aws.smithy.kotlin.runtime.auth.awscredentials.Credentials(
            "mockAccessKey",
            "mockSecretKey",
            "mockSessionToken"
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
    fun `Test assumeS3SafeRole handles assumeRole failure`() = runBlocking {
        coEvery { mockStsClient.assumeRole(any()) } throws RuntimeException("AWS STS failed")
        val exception = assertThrows<RuntimeException> {
            runBlocking { s3Manager.assumeS3SafeRole() }
        }
        assertEquals("AWS STS failed", exception.message)
        coVerify { mockStsClient.assumeRole(any<AssumeRoleRequest>()) }
    }

    @Test
    fun `Test assumeS3SafeRole fails when config is null`() = runBlocking {
        coEvery { mockStsClient.assumeRole(any()) } throws RuntimeException("roleArn must not be null")
        val s3ManagerWithInvalidConfig = S3Manager(null, mockStsClient)

        val exception = assertThrows<java.lang.RuntimeException> {
            runBlocking { s3ManagerWithInvalidConfig.assumeS3SafeRole() }
        }
        assertEquals("roleArn must not be null", exception.message)
    }

    @Test
    fun `Test assumeS3SafeRole fails when config is incomplete`() = runBlocking {
        val s3ManagerWithInvalidConfig = S3Manager(buildJsonObject { }, mockStsClient)

        val exception = assertThrows<java.lang.RuntimeException> {
            runBlocking { s3ManagerWithInvalidConfig.assumeS3SafeRole() }
        }
        assertEquals("roleArn must not be null", exception.message)
    }

    @Test
    fun `Test assumeS3SafeRole fails when "role" is not a JsonPrimitive`() = runBlocking {
        val s3ManagerWithInvalidConfig = S3Manager(buildJsonObject {
            put("role", buildJsonObject { put("key", "value") })
        }, mockStsClient)

        val exception = assertThrows<java.lang.RuntimeException> {
            runBlocking { s3ManagerWithInvalidConfig.assumeS3SafeRole() }
        }
        assertEquals("Element class kotlinx.serialization.json.JsonObject is not a JsonPrimitive", exception.message)
    }
}