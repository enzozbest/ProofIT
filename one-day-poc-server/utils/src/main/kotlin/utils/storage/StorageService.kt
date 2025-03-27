package utils.storage

import java.io.File

object StorageService {
    /**
     * Function to store a file in local storage.
     * If the call succeeds, the full path to the file is returned. Otherwise, an empty string is returned.
     *
     * @param path The path in which to store the file.
     * @param key The name of the file.
     * @param file The file to store.
     * @return The full path to the file if the call succeeds, an empty string otherwise.
     */
    fun storeFileLocal(
        path: String,
        key: String,
        file: File,
    ): String = LocalStorage.storeFile(path, key, file)

    /**
     * Function to store a file in remote storage.
     * If the call succeeds, the full path to the file is returned. Otherwise, an empty string is returned.
     *
     * @param bucket The S3 bucket in which to store the file.
     * @param key The key of the file. This may be a subpath in the bucket.
     * @param file The file to store.
     * @return The full path to the file if the call succeeds, an empty string otherwise.
     */
    suspend fun storeFileRemote(
        bucket: String,
        key: String,
        file: File,
    ): String = S3Storage.storeFile(bucket, key, file)

    /**
     * Function to retrieve a file from local storage.
     * If the file is found, it is returned. Otherwise, null is returned.
     *
     * @param path The full path to the file.
     * @return The contents of the file as a ByteArray. If the file is not found, null is returned.
     */
    fun getFileLocal(path: String): ByteArray? = LocalStorage.getFile(path)

    /**
     * Function to retrieve a file from remote storage.
     * If the file is found, it is returned. Otherwise, null is returned.
     *
     * @param bucket The S3 bucket from which to retrieve the file.
     * @param key The key of the file.
     * @return The contents of the file as a ByteArray. If the file is not found, null is returned.
     */
    suspend fun getFileRemote(
        bucket: String,
        key: String,
    ): ByteArray? = S3Storage.getFile(bucket, key)

    /**
     * Function to delete a file from local storage.
     * If the call succeeds, true is returned. Otherwise, false is returned.
     *
     * @param path The full path to the file.
     * @return true if the file was successfully deleted, false otherwise.
     */
    fun deleteFileLocal(path: String): Boolean = LocalStorage.deleteFile(path)

    /**
     * Function to delete a file from remote storage.
     * If the call succeeds, true is returned. Otherwise, false is returned.
     *
     * @param bucket The S3 bucket from which to delete the file.
     * @param key The key of the file.
     * @return true if the file was successfully deleted, false otherwise.
     */
    suspend fun deleteFileRemote(
        bucket: String,
        key: String,
    ): Boolean = S3Storage.deleteFile(bucket, key)
}
