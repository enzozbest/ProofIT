package utils.storage

import utils.aws.S3Service
import java.io.File

object S3Storage {
    /**
     * Function to store a file in S3 storage.
     * If the call succeeds, the S3 file key is returned. Otherwise, an empty string is returned.
     *
     * @param bucket The bucket to store the file.
     * @param key The key of the file.
     * @param file The file to store.
     * @return The full S3 path to the file if the call succeeds, an empty string otherwise.
     */
    suspend fun storeFile(
        bucket: String,
        key: String,
        file: File,
    ): String = S3Service.uploadFile(bucket, file, key)

    /**
     * Function to retrieve a file from S3 storage.
     * If the file is found, it is returned. Otherwise, null is returned.
     * @param bucket The bucket to retrieve the file from.
     * @param key The key of the file.
     * @return The contents of the file as a ByteArray. If the file is not found, null is returned.
     */
    suspend fun getFile(
        bucket: String,
        key: String,
    ): ByteArray? = S3Service.getFile(bucket, key)

    /**
     * Function to delete a file from S3 storage.
     * If the call succeeds, true is returned. Otherwise, false is returned.
     * @param bucket The bucket to delete the file from.
     * @param key The key of the file.
     * @return true if the file was deleted, false otherwise.
     */
    suspend fun deleteFile(
        bucket: String,
        key: String,
    ): Boolean = S3Service.deleteObject(bucket, key)
}
