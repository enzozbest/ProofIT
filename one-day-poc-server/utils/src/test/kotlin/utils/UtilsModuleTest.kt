package utils

import aws.sdk.kotlin.services.sts.StsClient
import io.ktor.server.application.Application
import io.mockk.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import utils.aws.S3Manager
import utils.aws.S3Service
import utils.environment.EnvironmentLoader
import utils.json.PoCJSON

class UtilsModuleTest {
    private val testAwsRegion = "eu-west-2"
    private val testS3ConfigJson =
        """
        {
          "s3": {
            "role": "arn:aws:iam::123456789123:role/some_role",
            "buckets": [
              "some-bucket",
              "some-other-bucket"
            ]
          }
        }
        """.trimIndent()
    private val testS3Config = Json.parseToJsonElement(testS3ConfigJson).jsonObject
    private lateinit var capturedS3Manager: CapturingSlot<S3Manager>
    private lateinit var mockApplication: Application
    private lateinit var mockStsClient: StsClient

    @BeforeEach
    fun setup() {
        mockkObject(EnvironmentLoader)
        mockkObject(PoCJSON)
        mockkObject(S3Service)

        mockApplication = mockk(relaxed = true)
        mockStsClient = mockk(relaxed = true)
        capturedS3Manager = slot()

        mockkConstructor(StsClient::class)

        every { EnvironmentLoader.get("AWS_REGION") } returns testAwsRegion

        every { PoCJSON.readJsonFile("s3_config.json") } returns testS3Config

        every { S3Service.init(capture(capturedS3Manager)) } just Runs
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `Test configureUtils initializes S3Service with correct configuration`() {
        mockApplication.configureUtils()

        verify { EnvironmentLoader.get("AWS_REGION") }

        verify { PoCJSON.readJsonFile("s3_config.json") }

        verify { S3Service.init(any()) }

        assert(capturedS3Manager.isCaptured)
    }
}
