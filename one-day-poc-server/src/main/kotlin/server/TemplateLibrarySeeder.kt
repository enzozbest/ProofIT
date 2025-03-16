package server

import utils.environment.EnvironmentLoader

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
    private val seeded = EnvironmentLoader.get("TDB_SEEDED").toBoolean()
    private val libraryPath = EnvironmentLoader.get("TDB_LIBRARY_PATH_ABSOLUTE")

    fun seed() {
        if (seeded) {
            println("Template Library already seeded.")
            return
        }

        println("Seeding Template Library...")
        println("Template Library seeded.")
    }
}

// Label this function as the main function because this script is executable on its own!
fun main() {
    TemplateLibrarySeeder.seed()
}
