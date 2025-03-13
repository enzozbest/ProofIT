package prompting.helpers.templates

import prompting.exceptions.TemplateRetrievalException
import utils.environment.EnvironmentLoader
import utils.storage.StorageService
import kotlin.io.path.createTempFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.writeText

/**
 * Utility object for template storage operations.
 * Handles file operations for templates, including retrieving and storing template files.
 */
object TemplateStorageUtils {
    /**
     * Retrieves the file content from either local or remote storage.
     *
     * @param templateHandle The URI of the template file
     * @return The content of the file as a ByteArray
     * @throws TemplateRetrievalException if the file cannot be retrieved
     */
    suspend fun retrieveFileContent(templateHandle: String): ByteArray =
        if (EnvironmentLoader.get("LOCAL_STORAGE").toBoolean()) {
            retrieveLocalFileContent(templateHandle)
        } else {
            retrieveRemoteFileContent(templateHandle)
        }

    /**
     * Retrieves a file from local storage.
     *
     * @param path The path to the file
     * @return The content of the file as a ByteArray
     * @throws TemplateRetrievalException if the file cannot be retrieved
     */
    private fun retrieveLocalFileContent(path: String): ByteArray =
        StorageService.getFileLocal(path = path)
            ?: throw TemplateRetrievalException("File not found")

    /**
     * Retrieves a file from remote storage.
     *
     * @param url The S3 URL of the file
     * @return The content of the file as a ByteArray
     * @throws TemplateRetrievalException if the file cannot be retrieved
     */
    private suspend fun retrieveRemoteFileContent(url: String): ByteArray {
        val (bucket, key) = parseS3Url(url)
        return StorageService.getFileRemote(bucket = bucket, key = key)
            ?: throw TemplateRetrievalException("File not found")
    }

    /**
     * Parses an S3 URL to extract the bucket and key.
     *
     * @param url The S3 URL to parse
     * @return A Pair of bucket and key
     * @throws TemplateRetrievalException if the URL is invalid
     */
    private fun parseS3Url(url: String): Pair<String, String> {
        val bucket =
            Regex("""^https://([^\.]+)\.s3\.amazonaws\.com/.*$""")
                .find(url)
                ?.groupValues
                ?.get(1)
                ?: throw TemplateRetrievalException("Invalid S3 URL")

        val key =
            Regex("""^https://[^\.]+\.s3\.amazonaws\.com/(.+)$""")
                .find(url)
                ?.groupValues
                ?.get(1)
                ?: throw TemplateRetrievalException("Invalid S3 URL")

        return Pair(bucket, key)
    }

    /**
     * Stores a file in either local or remote storage.
     *
     * @param content The content to store
     * @param filePrefix The prefix for the temporary file
     * @param fileSuffix The suffix for the temporary file
     * @param storageConfig The configuration for storing the file (path/bucket and key)
     * @return The path to the stored file
     */
    suspend fun storeFile(
        content: String,
        filePrefix: String,
        fileSuffix: String,
        storageConfig: StorageConfig,
    ): String {
        val tempFile = createTempFile(prefix = filePrefix, suffix = fileSuffix)
        tempFile.writeText(content)

        return if (EnvironmentLoader.get("LOCAL_STORAGE").toBoolean()) {
            StorageService.storeFileLocal(storageConfig.path, storageConfig.key, tempFile.toFile())
        } else {
            StorageService.storeFileRemote(storageConfig.bucket, storageConfig.key, tempFile.toFile())
        }.also {
            tempFile.deleteIfExists()
        }
    }

    /**
     * Configuration for storing a file.
     *
     * @param path The path for local storage
     * @param key The key for the file
     * @param bucket The bucket for remote storage
     */
    data class StorageConfig(
        val path: String,
        val key: String,
        val bucket: String,
    )
}
