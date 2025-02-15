package kcl.seg.rtt.utils.aws

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.DeleteObjectRequest
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.ListObjectsV2Request
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.sts.StsClient
import aws.sdk.kotlin.services.sts.model.AssumeRoleRequest
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.content.asByteStream
import aws.smithy.kotlin.runtime.content.decodeToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.Files

data class ConfiguredS3Client(
    val s3Client: S3Client,
    val credentialsProvider: StaticCredentialsProvider?,
    val region: String?,
)

/**
 * Object to interact with AWS S3 service.
 */
class S3Manager(
    private val s3Config: JsonObject?,
    private val stsClient: StsClient,
) {
    private var s3client: S3Client? = null

    /**
     * Function to assume the role which allows interactions with S3.
     */
    suspend fun assumeS3SafeRole(): Credentials {
        val request =
            AssumeRoleRequest {
                roleArn =
                    s3Config?.let {
                        runCatching { it["role"]!!.jsonPrimitive.content }.getOrNull()
                    }
                roleSessionName = "oneDayPocSession"
            }
        return STSInteractor.assumeRole(stsClient, request)
    }

    /**
     * Function to get an S3 client using temporary credentials.
     */
    suspend fun getClient(): S3Client {
        if (s3client != null) return s3client!!
        val tempCredentials = assumeS3SafeRole()
        val credentialsProvider = StaticCredentialsProvider(tempCredentials)
        return buildClient(s3Config, credentialsProvider).s3Client.also {
            s3client = it
        }
    }

    fun buildClient(
        s3Config: JsonObject?,
        credentialsProvider: StaticCredentialsProvider,
    ): ConfiguredS3Client {
        val region =
            s3Config?.let {
                kotlin.runCatching { it["region"]!!.jsonPrimitive.content }.getOrElse { "eu-west-2" }
            }

        val s3client =
            S3Client {
                this.region = region
                this.credentialsProvider = credentialsProvider
            }
        return ConfiguredS3Client(s3client, credentialsProvider, region)
    }
}

object S3Service {
    private val dispatcher = Dispatchers.IO
    private lateinit var s3Manager: S3Manager
    private lateinit var s3client: S3Client

    fun init(s3Manager: S3Manager) {
        this.s3Manager = s3Manager
    }

    private suspend fun ensureClient() {
        if (!::s3client.isInitialized) {
            s3client = s3Manager.getClient()
        }
    }

    /**
     * Function to upload a file to an S3 bucket.
     * @param bucketName The name of the bucket to upload the file to.
     * @param file The file to upload.
     * @param key The key to use for the file in the bucket.
     * @return The URL of the uploaded file.
     */
    suspend fun uploadFile(
        bucketName: String,
        file: File,
        key: String,
    ): String {
        val contentType =
            withContext(dispatcher) {
                Files.probeContentType(file.toPath()) // Automatically detects MIME type.
            }
        val request =
            PutObjectRequest {
                this.bucket = bucketName
                this.key = key
                this.body = file.asByteStream()
                this.contentType = contentType
            }
        ensureClient()
        s3client.putObject(request)
        return "https://$bucketName.s3.amazonaws.com/$key"
    }

    /**
     * Function to retrieve a file from S3 for use in-memory. Files are retrieved as Strings.
     * @param bucketName The name of the bucket to retrieve the file from.
     * @param key The key of the file to retrieve.
     * @return The contents of the file as a String.
     */
    suspend fun getFile(
        bucketName: String,
        key: String,
    ): String {
        val request =
            GetObjectRequest {
                this.bucket = bucketName
                this.key = key
            }
        ensureClient()
        return s3client.getObject(request) { response ->
            response.body?.decodeToString().toString()
        }
    }

    /**
     * Function to delete an object from an S3 bucket.
     * @param bucketName The name of the bucket to delete the object from.
     * @param key The key of the object to delete.
     * @return True if the object was successfully deleted, false otherwise.
     */
    suspend fun deleteObject(
        bucketName: String,
        key: String,
    ): Boolean {
        val request =
            DeleteObjectRequest {
                this.bucket = bucketName
                this.key = key
            }
        ensureClient()
        s3client.deleteObject(request).also {
            return true
        }
    }

    /**
     * Function to list all objects in an S3 bucket. The results are returned as a list of keys, as Strings.
     * @param bucketName The name of the bucket to list objects from.
     * @return A list of keys of objects in the bucket.
     */
    suspend fun listBucket(bucketName: String): List<String> {
        ensureClient()
        val request =
            ListObjectsV2Request {
                this.bucket = bucketName
            }
        return s3client.listObjectsV2(request).contents?.mapNotNull { it.key } ?: emptyList()
    }
}
