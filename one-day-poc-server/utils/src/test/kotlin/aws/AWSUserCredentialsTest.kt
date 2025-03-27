package aws

import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import utils.aws.AWSCredentialsProvider
import utils.aws.AWSUserCredentials
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AWSUserCredentialsTest {
    private lateinit var mockProvider: AWSCredentialsProvider

    @BeforeEach
    fun setUp() {
        mockProvider = mockk<AWSCredentialsProvider>()

        every { mockProvider.getAccessKeyId() } returns "mock-access-key"
        every { mockProvider.getSecretAccessKey() } returns "mock-secret-key"
        every { mockProvider.getRegion() } returns "mock-region"
    }

    @AfterEach
    fun tearDown() {
        AWSUserCredentials.resetProvider()
        clearAllMocks()
    }

    @Test
    fun `Test default provider returns expected values`() {
        AWSUserCredentials.getAccessKeyId()
        AWSUserCredentials.getSecretAccessKey()
        AWSUserCredentials.getRegion()
    }

    @Test
    fun `Test setProvider changes the provider`() {
        AWSUserCredentials.setProvider(mockProvider)

        assertEquals("mock-access-key", AWSUserCredentials.getAccessKeyId())
        assertEquals("mock-secret-key", AWSUserCredentials.getSecretAccessKey())
        assertEquals("mock-region", AWSUserCredentials.getRegion())

        verify(exactly = 1) { mockProvider.getAccessKeyId() }
        verify(exactly = 1) { mockProvider.getSecretAccessKey() }
        verify(exactly = 1) { mockProvider.getRegion() }
    }

    @Test
    fun `Test resetProvider restores the default provider`() {
        AWSUserCredentials.setProvider(mockProvider)

        assertEquals("mock-access-key", AWSUserCredentials.getAccessKeyId())

        AWSUserCredentials.resetProvider()

        AWSUserCredentials.getAccessKeyId()
        verify(exactly = 1) { mockProvider.getAccessKeyId() }
    }

    @Test
    fun `Test default provider handles exception when resolving access key`() {
        mockkConstructor(DefaultChainCredentialsProvider::class)
        coEvery { anyConstructed<DefaultChainCredentialsProvider>().resolve() } throws RuntimeException("Test exception")

        AWSUserCredentials.resetProvider()

        val accessKeyId = AWSUserCredentials.getAccessKeyId()
        assertEquals("default-access-key-id", accessKeyId)

        unmockkConstructor(DefaultChainCredentialsProvider::class)
    }

    @Test
    fun `Test default provider handles exception when resolving secret key`() {
        mockkConstructor(DefaultChainCredentialsProvider::class)
        coEvery { anyConstructed<DefaultChainCredentialsProvider>().resolve() } throws RuntimeException("Test exception")

        AWSUserCredentials.resetProvider()

        val secretAccessKey = AWSUserCredentials.getSecretAccessKey()
        assertEquals("default-secret-access-key", secretAccessKey)

        unmockkConstructor(DefaultChainCredentialsProvider::class)
    }

    @Test
    fun `Test default provider returns default region when region is null`() {
        mockkConstructor(DefaultChainCredentialsProvider::class)
        every { anyConstructed<DefaultChainCredentialsProvider>().region } returns null

        AWSUserCredentials.resetProvider()

        val region = AWSUserCredentials.getRegion()
        assertEquals("eu-west-2", region)

        unmockkConstructor(DefaultChainCredentialsProvider::class)
    }

    @Test
    fun `Test default provider returns actual region when region is not null`() {
        mockkConstructor(DefaultChainCredentialsProvider::class)
        every { anyConstructed<DefaultChainCredentialsProvider>().region } returns "us-east-1"

        AWSUserCredentials.resetProvider()

        val region = AWSUserCredentials.getRegion()
        assertEquals("us-east-1", region)

        unmockkConstructor(DefaultChainCredentialsProvider::class)
    }
}
