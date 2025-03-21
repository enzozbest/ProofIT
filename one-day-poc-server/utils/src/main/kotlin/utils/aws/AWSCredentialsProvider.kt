package utils.aws

/**
 * Interface for providing AWS credentials.
 * This interface is used to abstract the AWS credentials retrieval,
 * increasing testability.
 */
interface AWSCredentialsProvider {
    /**
     * Get the AWS access key ID.
     * @return The AWS access key ID.
     */
    fun getAccessKeyId(): String

    /**
     * Get the AWS secret access key.
     * @return The AWS secret access key.
     */
    fun getSecretAccessKey(): String

    /**
     * Get the AWS region.
     * @return The AWS region.
     */
    fun getRegion(): String
}