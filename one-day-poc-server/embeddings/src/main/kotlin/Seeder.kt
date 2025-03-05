import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

object Seeder {
    var logger: Logger = LoggerFactory.getLogger(Seeder::class.java)
    private val embeddingService = TemplateService

    suspend fun processComponentLibrary(directoryPath: String) {
        val directory = validateDirectory(directoryPath)
        val jsonFiles = findJsonFiles(directory)

        withContext(Dispatchers.IO) {
            jsonFiles.forEach { file ->
                processFile(file)
            }
        }
    }

    private fun validateDirectory(directoryPath: String): File {
        val directory = File(directoryPath)
        if (!directory.exists() || !directory.isDirectory) {
            throw IllegalArgumentException("Invalid directory path: $directoryPath")
        }
        return directory
    }

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

    private fun findJsonFiles(directory: File): List<File> {
        return directory.listFiles()
            ?.filter { it.extension.equals("json", ignoreCase = true) }
            ?: emptyList()
    }

    /**
     * Simple regex check to see if the content is JSON-LD
     * This avoids full JSON parsing
     */
    private fun isJsonLd(content: String): Boolean {
        val contextPattern = "\"@context\"\\s*:".toRegex()
        val typePattern = "\"@type\"\\s*:".toRegex()

        return contextPattern.containsMatchIn(content) && typePattern.containsMatchIn(content)
    }

}