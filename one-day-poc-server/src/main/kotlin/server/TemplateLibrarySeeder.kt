package server

import database.core.DatabaseManager
import kotlinx.coroutines.runBlocking
import templates.TemplateService
import utils.environment.EnvironmentLoader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.Socket
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
 */
object TemplateLibrarySeeder {
    private const val ENV_FILE = ".env"
    private const val TEMPLATE_EXTENSION = ".templ"
    private const val ANNOTATION_EXTENSION = ".jsonld"
    private const val PYTHON_MODULE = "information_retrieval.__main__"
    private const val MICROSERVICE_HOST = "localhost"
    private const val MICROSERVICE_PORT = 7000
    private const val SUCCESS_STATUS = "success"

    // Initialize environment
    init {
        EnvironmentLoader.loadEnvironmentFile(ENV_FILE)
    }

    private val seeded: Boolean = EnvironmentLoader.get("TDB_SEEDED").toBoolean()
    private val libraryPath: String = EnvironmentLoader.get("TDB_LIBRARY_PATH_ABSOLUTE")
    private val templateDir = File("$libraryPath/templates")
    private val metadataDir = File("$libraryPath/metadata")
    private val isDevEnvironment = EnvironmentLoader.get("DB_URL").contains("localhost")

    /**
     * Updates the TDB_SEEDED environment variable in the .env file.
     *
     * @param seeded The new value for the TDB_SEEDED environment variable.
     */
    private fun updateSeededStatus(seeded: Boolean) {
        val envFile = File(ENV_FILE)

        if (!envFile.exists()) {
            println("Warning: $ENV_FILE file not found. Cannot update TDB_SEEDED status.")
            return
        }

        runCatching {
            val properties =
                Properties().apply {
                    FileInputStream(envFile).use { load(it) }
                    setProperty("TDB_SEEDED", seeded.toString())
                }

            FileOutputStream(envFile).use {
                properties.store(it, "Updated by TemplateLibrarySeeder")
            }

            println("Updated TDB_SEEDED to $seeded in $ENV_FILE file.")
        }.onFailure {
            println("Error updating TDB_SEEDED status: ${it.message}")
        }
    }

    /**
     * Starts the local Docker database if in development environment.
     */
    private fun startLocalDockerDB() {
        if (!isDevEnvironment) return

        runDockerCommand("up", "-d")
    }

    /**
     * Stops the local Docker database if in development environment.
     */
    private fun stopLocalDockerDB() {
        if (!isDevEnvironment) return

        runDockerCommand("down")
    }

    /**
     * Runs a Docker command with the specified arguments.
     * Tries both "docker-compose" and "docker compose" syntax.
     */
    private fun runDockerCommand(vararg args: String) {
        runCatching {
            // Try docker-compose syntax first
            val process = ProcessBuilder("docker-compose", *args).start()
            val exitCode = process.waitFor()

            // If docker-compose failed, try docker compose syntax
            if (exitCode != 0) {
                ProcessBuilder("docker", "compose", *args)
                    .start()
                    .waitFor()
            }
        }.onFailure {
            // If an exception occurred, try docker compose syntax
            ProcessBuilder("docker", "compose", *args)
                .inheritIO()
                .start()
                .waitFor()
        }
    }

    /**
     * Starts the Python microservice.
     *
     * @return The process running the Python microservice.
     */
    private fun startPythonMicroservice(): Process {
        val workingDir =
            Paths
                .get("embeddings/src/main/python")
                .toAbsolutePath()
                .normalize()
                .toFile()

        val venvDir = File(workingDir, "venv")

        // Determine the Python interpreter path based on the OS
        val interpreter =
            if (System.getProperty("os.name").lowercase().contains("win")) {
                File(venvDir, "Scripts\\python.exe").absolutePath
            } else {
                File(venvDir, "bin/python3.10").absolutePath
            }

        return ProcessBuilder(interpreter, "-m", PYTHON_MODULE)
            .inheritIO()
            .directory(workingDir)
            .start()
    }

    /**
     * Stops the Python microservice.
     *
     * @param process The process running the Python microservice.
     */
    private fun stopPythonMicroservice(process: Process) {
        process.destroy()
    }

    /**
     * Waits for the microservice to be ready by attempting to connect to it.
     *
     * @param host The host where the microservice is running.
     * @param port The port where the microservice is listening.
     * @param timeoutMillis The maximum time to wait for the microservice to be ready.
     * @param pollIntervalMillis The interval between connection attempts.
     * @throws RuntimeException If the microservice is not ready after the timeout.
     */
    fun waitForMicroservice(
        host: String = MICROSERVICE_HOST,
        port: Int = MICROSERVICE_PORT,
        timeoutMillis: Long = 120000,
        pollIntervalMillis: Long = 5000,
    ) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeoutMillis

        while (System.currentTimeMillis() < endTime) {
            try {
                Socket(host, port).use {
                    println("Microservice is ready!")
                    return
                }
            } catch (e: Exception) {
                // Connection failed, sleep and try again
                Thread.sleep(pollIntervalMillis)
            }
        }

        throw RuntimeException("Timeout waiting for microservice to be ready.")
    }

    /**
     * Seeds the Template Library with templates and annotations.
     */
    fun seed() {
        if (seeded) {
            println("Template Library already seeded.")
            return
        }

        try {
            validateLibraryPath()

            println("Checking for local database...")
            runBlocking { startLocalDockerDB() }

            println("Initialising DB...")
            runBlocking { DatabaseManager.externalInit() }

            println("Starting Python microservice...")
            val pythonProcess = startPythonMicroservice()
            waitForMicroservice()

            val templates = loadTemplateFiles()
            val annotations = loadAnnotationFiles()

            if (templates.isEmpty() || annotations.isEmpty()) {
                return
            }

            // Build a lookup map for annotations by base name
            val annotationMap = annotations.associateBy { it.name.substringBeforeLast('.') }

            println("Seeding Template Library...")
            val successCount = processTemplates(templates, annotationMap)

            if (successCount.isNotEmpty()) {
                println("Successfully seeded ${successCount.size} templates.")
                // updateSeededStatus(true)
            } else {
                println("No templates were successfully seeded.")
            }

            println("Stopping Python microservice...")
            stopPythonMicroservice(pythonProcess)
            stopLocalDockerDB()
            println("Template Library seeding process completed.")
        } catch (e: Exception) {
            println("Error during seeding process: ${e.message}")
        }
    }

    /**
     * Validates that the library path is absolute and exists.
     *
     * @return true if the path is valid, false otherwise.
     */
    private fun validateLibraryPath(): Boolean {
        if (!Paths.get(libraryPath).isAbsolute) {
            println(libraryPath)
            println("The path to the Template Library must be absolute.")
            return false
        }

        if (!File(libraryPath).exists()) {
            println("The path to the Template Library does not exist.")
            return false
        }

        return true
    }

    /**
     * Loads template files from the template directory.
     *
     * @return A list of template files.
     */
    private fun loadTemplateFiles(): List<File> {
        println("Attempting to read template files...")
        val templates = templateDir.listFiles { _, name -> name.endsWith(TEMPLATE_EXTENSION) }?.toList().orEmpty()
        println("Found ${templates.size} template files.")

        if (templates.isEmpty()) {
            println("No template files found.")
        }

        return templates
    }

    /**
     * Loads annotation files from the metadata directory.
     *
     * @return A list of annotation files.
     */
    private fun loadAnnotationFiles(): List<File> {
        println("Attempting to read annotation files...")
        val annotations = metadataDir.listFiles { _, name -> name.endsWith(ANNOTATION_EXTENSION) }?.toList().orEmpty()
        println("Found ${annotations.size} annotation files.")

        if (annotations.isEmpty()) {
            println("No annotation files found.")
        }

        return annotations
    }

    /**
     * Processes templates and their annotations.
     *
     * @param templates The list of template files.
     * @param annotationMap A map of annotation files by base name.
     * @return A map of template names to annotation IDs for successfully processed templates.
     */
    private fun processTemplates(
        templates: List<File>,
        annotationMap: Map<String, File>,
    ): Map<String, String> =
        runBlocking {
            templates
                .mapNotNull { template ->
                    processTemplate(template, annotationMap)
                }.toMap()
        }

    /**
     * Processes a single template and its annotation.
     *
     * @param template The template file.
     * @param annotationMap A map of annotation files by base name.
     * @return A pair of template name to annotation ID if successful, null otherwise.
     */
    private suspend fun processTemplate(
        template: File,
        annotationMap: Map<String, File>,
    ): Pair<String, String>? {
        val templateBaseName = template.name.substringBeforeLast('.')
        val annotationFile =
            annotationMap[templateBaseName] ?: run {
                println("No annotation found for template '${template.name}'")
                return null
            }

        return runCatching {
            val annotationText = annotationFile.readText()
            val response = TemplateService.storeTemplate(template.toURI().toString(), annotationText)

            if (response.status == SUCCESS_STATUS) {
                val id = response.id ?: error("Annotation ID not found for template '${template.name}'")
                println("Template '${template.name}' processed with annotation ID '$id'.")
                template.name to id
            } else {
                println("Failed to store template '${template.name}': ${response.message ?: "Unknown error"}")
                null
            }
        }.getOrElse {
            println("Error processing template '${template.name}': ${it.message}")
            null
        }
    }
}

fun main() =
    runBlocking {
        TemplateLibrarySeeder.seed()
    }
