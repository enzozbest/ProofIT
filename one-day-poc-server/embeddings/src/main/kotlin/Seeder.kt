import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object Seeder {
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
                    val response = embeddingService.storeTemplate(file.nameWithoutExtension, jsonContent)

                    if (response.status != "success") {
                        println("Embedding failed for ${file.name}: ${response.message ?: "Unknown error"}")
                    }
                } catch (e: Exception) {
                    println("Error embedding ${file.name}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("Error processing file ${file.name}: ${e.message}")
        }
    }

    private fun findJsonFiles(directory: File): List<File> =
        directory
            .listFiles()
            ?.filter { it.extension.equals("json", ignoreCase = true) }
            ?: emptyList()

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
