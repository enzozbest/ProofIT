package storage

import kcl.seg.rtt.utils.environment.EnvironmentLoader
import java.io.File

object StorageService {
    private var localStorage = true

    private fun updateStorageLocation() {
        EnvironmentLoader.reset()
        EnvironmentLoader.get("LOCAL_STORAGE").let {
            localStorage = it.toBoolean()
        }
    }

    private fun getStorageLocation(): String {
        updateStorageLocation()
        return if (localStorage) "local" else "remote"
    }

    /**
     * Function to store a file in storage.
     * If the call succeeds, the full path to the file is returned. Otherwise, an empty string is returned.
     *
     * @param bucket The S3 bucket in which to store the file. Only required for remote storage.
     * @param path The path in which to store the file. Only required for local storage.
     * @param key The key of the file. For remote storage this may be a subpath in the bucket. For local storage, this
     * is the name of the file.
     * @param file The file to store.
     * @return The full path to the file if the call succeeds, an empty string otherwise.
     */
    suspend fun storeFile(
        bucket: String?,
        path: String?,
        key: String,
        file: File,
    ): String =
        when (getStorageLocation()) {
            "local" ->
                path?.let { LocalStorage.storeFile(path, key, file) }
                    ?: throw IllegalArgumentException("Path is required for local storage!")

            "remote" ->
                bucket?.let { S3Storage.storeFile(bucket, key, file) }
                    ?: throw IllegalArgumentException("Bucket is required for remote storage!")

            else -> ""
        }

    /**
     * Function to retrieve a file from storage.
     * If the file is found, it is returned. Otherwise, null is returned.
     * @param path The full path to the file. Only required for local storage retrieval.
     * @param bucket The S3 bucket in which to store the file. Only required for remote storage retrieval.
     * @param key The key of the file. Only required for remote storage retrieval.
     */
    suspend fun getFile(
        path: String?,
        bucket: String?,
        key: String?,
    ) = when (getStorageLocation()) {
        "local" ->
            path?.let { LocalStorage.getFile(path) }
                ?: throw IllegalArgumentException("Path is required for local storage!")

        "remote" ->
            bucket?.let {
                key?.let { S3Storage.getFile(bucket, key) }
                    ?: throw IllegalArgumentException("Key is required for remote storage!")
            }
                ?: throw IllegalArgumentException("Bucket is required for remote storage!")

        else -> null
    }

    /**
     * Function to delete a file from storage.
     * If the call succeeds, true is returned. Otherwise, false is returned.
     * @param path The full path to the file. Only required for local storage deletion.
     * @param bucket The S3 bucket in which to store the file. Only required for remote storage deletion.
     * @param key The key of the file. Only required for remote storage deletion.
     * @return true if the file was successfully deleted, false otherwise.
     */
    suspend fun deleteFile(
        path: String?,
        bucket: String?,
        key: String?,
    ): Boolean =
        when (getStorageLocation()) {
            "local" ->
                path?.let { LocalStorage.deleteFile(path) }
                    ?: throw IllegalArgumentException("Path is required for local storage deletion!")

            "remote" ->
                bucket?.let {
                    key?.let { S3Storage.deleteFile(bucket, key) }
                        ?: throw IllegalArgumentException("Key is required for remote storage deletion!")
                } ?: throw IllegalArgumentException("Bucket is required for remote storage deletion!")

            else -> false
        }
}
