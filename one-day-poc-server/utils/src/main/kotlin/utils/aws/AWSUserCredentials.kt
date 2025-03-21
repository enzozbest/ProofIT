package utils.aws

import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
import kotlinx.coroutines.runBlocking

/**
 * Default implementation of AWSCredentialsProvider that uses the DefaultChainCredentialsProvider
 * from the AWS SDK to resolve AWS credentials.
 */
internal class DefaultAWSCredentialsProvider : AWSCredentialsProvider {
    private val credentialsProvider = DefaultChainCredentialsProvider()
    private val credentials = runBlocking { credentialsProvider.resolve() }
    private val accessKeyId = credentials.accessKeyId
    private val secretAccessKey = credentials.secretAccessKey

    override fun getAccessKeyId(): String = accessKeyId

    override fun getSecretAccessKey(): String = secretAccessKey

    override fun getRegion(): String = credentialsProvider.region ?: "eu-west-2"
}

/**
 * Object that provides AWS credentials.
 * In production, it uses a DefaultAWSCredentialsProvider.
 * In tests, it can be configured to use a mock provider.
 */
object AWSUserCredentials : AWSCredentialsProvider {
    private var provider: AWSCredentialsProvider = DefaultAWSCredentialsProvider()

    /**
     * Set a custom AWS credentials provider.
     * This is useful for testing, where a mock provider can be used.
     * @param credentialsProvider The AWS credentials provider to use.
     */
    fun setProvider(credentialsProvider: AWSCredentialsProvider) {
        provider = credentialsProvider
    }

    /**
     * Reset the AWS credentials provider to the default.
     * This is useful for cleaning up after tests.
     */
    fun resetProvider() {
        provider = DefaultAWSCredentialsProvider()
    }

    override fun getAccessKeyId(): String = provider.getAccessKeyId()

    override fun getSecretAccessKey(): String = provider.getSecretAccessKey()

    override fun getRegion(): String = provider.getRegion()
}
