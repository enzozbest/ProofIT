package server

import kotlinx.coroutines.runBlocking
import templates.TemplateService
import utils.environment.EnvironmentLoader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Paths
import java.util.Properties

/**
 * Object responsible for seeding the Template Library.
 * To run the seeding process, the environment variable TDB_SEEDED must be set to false. This must be done manually.
 * The environment variable TDB_LIBRARY_PATH_ABSOLUTE must be set to the absolute path of root the Template Library.
 * The Template Library must contain the following structure:
 * - A folder named "templates" containing the template files. The extension must be .templ.
 * - A folder named "metadata" containing JSON-LD annotation files for each template. The extension must be .jsonld.
 * The names of the template files and the annotation files must match!.
 *
 * The seeding process will read the template files and the annotation files,
 * and store them in the appropriate database.
 *
 * The annotation files will be sent to the Python microservice for processing,
 * whereas the template files will be stored in the database. Thus, a connection to the Python service and the database
 * is required for this service.
 *
 * */
object TemplateLibrarySeeder {
    private val seeded: Boolean = EnvironmentLoader.get("TDB_SEEDED").toBoolean()
    private val libraryPath: String = EnvironmentLoader.get("TDB_LIBRARY_PATH_ABSOLUTE")
    private val templateDir = File("$libraryPath/templates")
    private val metadataDir = File("$libraryPath/metadata")

    /**
     * Updates the TDB_SEEDED environment variable in the .env file.
     *
     * @param seeded The new value for the TDB_SEEDED environment variable.
     */
    private fun updateSeededStatus(seeded: Boolean) {
        try {
            val envFile = File(".env")
            if (!envFile.exists()) {
                println("Warning: .env file not found. Cannot update TDB_SEEDED status.")
                return
            }

            val properties = Properties()
            FileInputStream(envFile).use { properties.load(it) }

            properties.setProperty("TDB_SEEDED", seeded.toString())

            FileOutputStream(envFile).use {
                properties.store(it, "Updated by TemplateLibrarySeeder")
            }

            println("Updated TDB_SEEDED to $seeded in .env file.")
        } catch (e: Exception) {
            println("Error updating TDB_SEEDED status: ${e.message}")
        }
    }

    suspend fun seed() {
        try {
            if (seeded) {
                println("Template Library already seeded.")
                return
            }

            // Ensure the provided library path is absolute and exists.
            if (!Paths.get(libraryPath).isAbsolute) {
                println("The path to the Template Library must be absolute.")
                return
            }
            if (!File(libraryPath).exists()) {
                println("The path to the Template Library does not exist.")
                return
            }

            println("Attempting to read template files...")
            val templates = templateDir.listFiles { _, name -> name.endsWith(".templ") }?.toList().orEmpty()
            println("Found ${templates.size} template files.")

            println("Attempting to read annotation files...")
            val annotations = metadataDir.listFiles { _, name -> name.endsWith(".jsonld") }?.toList().orEmpty()
            println("Found ${annotations.size} annotation files.")

            if (templates.isEmpty()) {
                println("No template files found.")
                return
            }

            if (annotations.isEmpty()) {
                println("No annotation files found.")
                return
            }

            // Build a lookup map for annotations by base name.
            val annotationMap = annotations.associateBy { it.name.substringBeforeLast('.') }

            println("Seeding Template Library...")
            val successCount =
                templates
                    .mapNotNull { template ->
                        val templateBaseName = template.name.substringBeforeLast('.')
                        val annotationFile =
                            annotationMap[templateBaseName]
                                ?: return@mapNotNull run {
                                    println("No annotation found for template '${template.name}'")
                                    null
                                }

                        try {
                            val annotationText = annotationFile.readText()
                            val response = TemplateService.storeTemplate(template.toURI().toString(), annotationText)
                            if (response.status == "success") {
                                println("Template '${template.name}' processed with annotation ID '${response.id}'.")
                                template.name to (
                                    response.id
                                        ?: error("Annotation ID not found for template '${template.name}'")
                                )
                            } else {
                                println("Failed to store template '${template.name}': ${response.message ?: "Unknown error"}")
                                null
                            }
                        } catch (e: Exception) {
                            println("Error processing template '${template.name}': ${e.message}")
                            null
                        }
                    }.toMap()

            if (successCount.isNotEmpty()) {
                println("Successfully seeded ${successCount.size} templates.")
                // Update the TDB_SEEDED environment variable to true
                updateSeededStatus(true)
            } else {
                println("No templates were successfully seeded.")
            }

            println("Template Library seeding process completed.")
        } catch (e: Exception) {
            println("Error during seeding process: ${e.message}")
        }
    }
}

// Label this function as the main function because this script is executable on its own!
fun main() {
    runBlocking {
        TemplateLibrarySeeder.seed()
    }
}
