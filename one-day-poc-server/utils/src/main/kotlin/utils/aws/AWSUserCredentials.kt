package utils.aws

import aws.sdk.kotlin.runtime.auth.credentials.DefaultChainCredentialsProvider
import kotlinx.coroutines.runBlocking
import kotlin.io.resolve

object AWSUserCredentials {
    private val credentialsProvider = DefaultChainCredentialsProvider()
    private val credentials = runBlocking { credentialsProvider.resolve() }
    private val accessKeyId = credentials.accessKeyId
    private val secretAccessKey = credentials.secretAccessKey

    fun getAccessKeyId(): String = accessKeyId

    fun getSecretAccessKey(): String = secretAccessKey

    fun getRegion(): String = credentialsProvider.region ?: "eu-west-2"
}
