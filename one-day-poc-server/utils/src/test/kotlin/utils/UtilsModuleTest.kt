package utils

import aws.sdk.kotlin.services.sts.StsClient
import io.ktor.server.application.Application
import io.mockk.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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
    private val testS3ConfigJson = """
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

        // Create mock objects
        mockApplication = mockk(relaxed = true)
        mockStsClient = mockk(relaxed = true)
        capturedS3Manager = slot()

        // Mock StsClient constructor
        mockkConstructor(StsClient::class)

        // Mock EnvironmentLoader.get to return the test AWS region
        every { EnvironmentLoader.get("AWS_REGION") } returns testAwsRegion

        // Mock PoCJSON.readJsonFile to return the test S3 configuration
        every { PoCJSON.readJsonFile("s3_config.json") } returns testS3Config

        // Mock S3Service.init to capture the S3Manager instance
        every { S3Service.init(capture(capturedS3Manager)) } just Runs
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `Test configureUtils initializes S3Service with correct configuration`() {
        // Call the function under test directly
        mockApplication.configureUtils()

        // Verify that EnvironmentLoader.get was called with the correct key
        verify { EnvironmentLoader.get("AWS_REGION") }

        // Verify that PoCJSON.readJsonFile was called with the correct file name
        verify { PoCJSON.readJsonFile("s3_config.json") }

        // Verify that S3Service.init was called with an S3Manager instance
        verify { S3Service.init(any()) }

        // Verify that an S3Manager was captured
        assert(capturedS3Manager.isCaptured)
    }
}
