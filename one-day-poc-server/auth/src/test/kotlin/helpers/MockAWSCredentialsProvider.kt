package helpers

import utils.aws.AWSCredentialsProvider

/**
 * A mock implementation of AWSCredentialsProvider for testing.
 * It returns fixed values for the AWS credentials.
 */
class MockAWSCredentialsProvider : AWSCredentialsProvider {
    override fun getAccessKeyId(): String = "mock-access-key-id"

    override fun getSecretAccessKey(): String = "mock-secret-access-key"

    override fun getRegion(): String = "eu-west-2"
}