package aws

import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
import io.mockk.*
import kotlinx.coroutines.runBlocking
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
        // Create a mock provider
        mockProvider = mockk<AWSCredentialsProvider>()

        // Set up default behavior
        every { mockProvider.getAccessKeyId() } returns "mock-access-key"
        every { mockProvider.getSecretAccessKey() } returns "mock-secret-key"
        every { mockProvider.getRegion() } returns "mock-region"
    }

    @AfterEach
    fun tearDown() {
        // Reset the provider to default after each test
        AWSUserCredentials.resetProvider()
        clearAllMocks()
    }

    @Test
    fun `Test default provider returns expected values`() {
        // The default provider should return default values or values from the environment
        // We're not testing the actual values, just that the methods don't throw exceptions
        AWSUserCredentials.getAccessKeyId()
        AWSUserCredentials.getSecretAccessKey()
        AWSUserCredentials.getRegion()
    }

    @Test
    fun `Test setProvider changes the provider`() {
        // Set our mock provider
        AWSUserCredentials.setProvider(mockProvider)

        // Verify that the methods delegate to our mock provider
        assertEquals("mock-access-key", AWSUserCredentials.getAccessKeyId())
        assertEquals("mock-secret-key", AWSUserCredentials.getSecretAccessKey())
        assertEquals("mock-region", AWSUserCredentials.getRegion())

        // Verify that our mock provider's methods were called
        verify(exactly = 1) { mockProvider.getAccessKeyId() }
        verify(exactly = 1) { mockProvider.getSecretAccessKey() }
        verify(exactly = 1) { mockProvider.getRegion() }
    }

    @Test
    fun `Test resetProvider restores the default provider`() {
        // First set our mock provider
        AWSUserCredentials.setProvider(mockProvider)

        // Verify it's being used
        assertEquals("mock-access-key", AWSUserCredentials.getAccessKeyId())

        // Now reset to the default provider
        AWSUserCredentials.resetProvider()

        // The default provider should be used now, not our mock
        // We can verify this by checking that our mock is not called again
        AWSUserCredentials.getAccessKeyId()
        verify(exactly = 1) { mockProvider.getAccessKeyId() }
    }

    @Test
    fun `Test default provider handles exception when resolving access key`() {
        // Mock the DefaultChainCredentialsProvider to throw an exception when resolve() is called
        mockkConstructor(DefaultChainCredentialsProvider::class)
        coEvery { anyConstructed<DefaultChainCredentialsProvider>().resolve() } throws RuntimeException("Test exception")

        // Reset to ensure we're using the default provider
        AWSUserCredentials.resetProvider()

        // The default provider should handle the exception and return a default value
        val accessKeyId = AWSUserCredentials.getAccessKeyId()
        assertEquals("default-access-key-id", accessKeyId)

        unmockkConstructor(DefaultChainCredentialsProvider::class)
    }

    @Test
    fun `Test default provider handles exception when resolving secret key`() {
        // Mock the DefaultChainCredentialsProvider to throw an exception when resolve() is called
        mockkConstructor(DefaultChainCredentialsProvider::class)
        coEvery { anyConstructed<DefaultChainCredentialsProvider>().resolve() } throws RuntimeException("Test exception")

        // Reset to ensure we're using the default provider
        AWSUserCredentials.resetProvider()

        // The default provider should handle the exception and return a default value
        val secretAccessKey = AWSUserCredentials.getSecretAccessKey()
        assertEquals("default-secret-access-key", secretAccessKey)

        unmockkConstructor(DefaultChainCredentialsProvider::class)
    }

    @Test
    fun `Test default provider returns default region when region is null`() {
        // Mock the DefaultChainCredentialsProvider to return null for region
        mockkConstructor(DefaultChainCredentialsProvider::class)
        every { anyConstructed<DefaultChainCredentialsProvider>().region } returns null

        // Reset to ensure we're using the default provider
        AWSUserCredentials.resetProvider()

        // The default provider should return the default region
        val region = AWSUserCredentials.getRegion()
        assertEquals("eu-west-2", region)

        unmockkConstructor(DefaultChainCredentialsProvider::class)
    }

    @Test
    fun `Test default provider returns actual region when region is not null`() {
        // Mock the DefaultChainCredentialsProvider to return a non-null region
        mockkConstructor(DefaultChainCredentialsProvider::class)
        every { anyConstructed<DefaultChainCredentialsProvider>().region } returns "us-east-1"

        // Reset to ensure we're using the default provider
        AWSUserCredentials.resetProvider()

        // The default provider should return the actual region
        val region = AWSUserCredentials.getRegion()
        assertEquals("us-east-1", region)

        unmockkConstructor(DefaultChainCredentialsProvider::class)
    }
}
