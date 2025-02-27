package storage

import java.io.File
import kotlin.io.path.Path

object LocalStorage {
    /**
     * Function to store a file in local storage.
     * If the call succeeds, the full system path to the file is returned. Otherwise, an empty string is returned.
     *
     * @param path The path to store the file.
     * @param name The name of the file.
     * @param file The file to store.
     * @return The full system path to the file if the call succeeds, an empty string otherwise.
     */
    fun storeFile(
        path: String,
        name: String,
        file: File,
    ): String =
        runCatching {
            file.copyTo(Path(path, name).toFile(), overwrite = true)
            file.delete()
        }.getOrNull()?.let { Path(path, name).toString() } ?: ""

    /**
     * Function to retrieve a file from local storage.
     * If the file is found, it is returned. Otherwise, null is returned.
     * @param path The full path to the file.
     * @return The contents of the file as a ByteArray. If the file is not found, null is returned.
     */
    fun getFile(path: String): ByteArray? =
        runCatching {
            Path(path).toFile().readBytes()
        }.getOrNull()

    /**
     * Function to delete a file from local storage.
     *@param path The full path to the file.
     * @return true if the file was deleted, false otherwise.
     */
    fun deleteFile(path: String) = runCatching { Path(path).toFile().delete() }.isSuccess
}
