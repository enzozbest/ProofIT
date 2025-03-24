package aws.sts

import aws.sdk.kotlin.services.sts.StsClient
import aws.sdk.kotlin.services.sts.model.AssumeRoleRequest
import aws.sdk.kotlin.services.sts.model.AssumeRoleResponse
import aws.smithy.kotlin.runtime.time.Instant
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import utils.aws.STSInteractor
import kotlin.time.Duration

class STSInteractorTest {
    private lateinit var mockStsClient: StsClient

    @BeforeEach
    fun setup() {
        mockStsClient = mockk<StsClient>(relaxed = true)
        val mockResponse =
            AssumeRoleResponse {
                credentials =
                    aws.sdk.kotlin.services.sts.model.Credentials {
                        accessKeyId = "mockAccessKeyId"
                        secretAccessKey = "mockSecretAccessKey"
                        sessionToken = "mockSessionToken"
                        expiration = Instant.now().plus(Duration.parse("1h"))
                    }
            }
        coEvery { mockStsClient.assumeRole(any<AssumeRoleRequest>()) } returns mockResponse
    }

    @Test
    fun `Test assumeRole returns credentials`() =
        runBlocking {
            val request =
                AssumeRoleRequest {
                    roleArn = "arn:aws:iam::123456789012:role/TestRole"
                    roleSessionName = "testSession"
                }
            val result = STSInteractor.assumeRole(mockStsClient, request)

            assertEquals("mockAccessKeyId", result.accessKeyId)
            assertEquals("mockSecretAccessKey", result.secretAccessKey)
            assertEquals("mockSessionToken", result.sessionToken)
            coVerify(exactly = 1) { mockStsClient.assumeRole(any<AssumeRoleRequest>()) }
        }
}
