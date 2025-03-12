package seeding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import templates.TemplateService
import java.io.File

/**
 * A utility object for processing component libraries and storing their JSON-LD templates
 * in the embedding service.
 *
 * Seeder walks through directories containing JSON-LD files, validates them, and
 * sends them to the template embedding service for processing and storage.
 */
internal object Seeder {
    var logger: Logger = LoggerFactory.getLogger(Seeder::class.java)
    private val embeddingService = TemplateService

    /**
     * Processes all JSON-LD files in a specified directory and stores them as templates.
     *
     * This method walks through the directory, identifies valid JSON files, and processes
     * each one that contains JSON-LD content by sending it to the embedding service.
     *
     * @param directoryPath Path to the directory containing JSON-LD template files
     */
    internal suspend fun processComponentLibrary(directoryPath: String) {
        val directory = validateDirectory(directoryPath)
        val jsonFiles = findJsonFiles(directory)

        withContext(Dispatchers.IO) {
            jsonFiles.forEach { file ->
                processFile(file)
            }
        }
    }

    /**
     * Validates that the specified directory path exists and is a directory.
     *
     * @param directoryPath Path to validate
     * @return A File object representing the valid directory
     * @throws IllegalArgumentException If the path doesn't exist or isn't a directory
     */
    private fun validateDirectory(directoryPath: String): File {
        val directory = File(directoryPath)
        if (!directory.exists() || !directory.isDirectory) {
            throw IllegalArgumentException("Invalid directory path: $directoryPath")
        }
        return directory
    }

    /**
     * Processes an individual JSON file, embedding it if it contains valid JSON-LD.
     *
     * This method reads the file content, validates it as JSON-LD, and sends it to the
     * embedding service for storage. Errors during processing are logged but don't
     * stop execution.
     *
     * @param file The JSON file to process
     */
    private suspend fun processFile(file: File) {
        try {
            val jsonContent = file.readText()

            if (isJsonLd(jsonContent)) {
                try {
                    val fileURI = file.toURI().toString()

                    val response = embeddingService.storeTemplate(file.nameWithoutExtension, fileURI, jsonContent)

                    if (response.status != "success") {
                        logger.error("Embedding failed for ${file.name}: ${response.message}")
                    }
                } catch (e: Exception) {
                    logger.error("Error embedding ${file.name}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            logger.error("Error processing file ${file.name}: ${e.message}")
        }
    }

    /**
     * Finds all JSON files in the specified directory.
     *
     * @param directory The directory to search for JSON files
     * @return A list of File objects representing JSON files, or empty list if none found
     */
    private fun findJsonFiles(directory: File): List<File> =
        directory
            .listFiles()
            ?.filter { it.extension.equals("utils/json", ignoreCase = true) }
            ?: emptyList()

    /**
     * Checks if a string contains JSON-LD content using simple regex pattern matching.
     *
     * This method performs a lightweight check for JSON-LD markers (@context and @type)
     * without fully parsing the JSON, which is more efficient for large files.
     *
     * @param content The string content to check
     * @return true if the content appears to be JSON-LD, false otherwise
     */
    private fun isJsonLd(content: String): Boolean {
        val contextPattern = "\"@context\"\\s*:".toRegex()
        val typePattern = "\"@type\"\\s*:".toRegex()

        return contextPattern.containsMatchIn(content) && typePattern.containsMatchIn(content)
    }
}
