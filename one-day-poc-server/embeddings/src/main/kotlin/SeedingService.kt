import kotlinx.coroutines.runBlocking
import java.io.File

object SeedingService {
    /**
     * Seeds the component library by processing all JSON-LD files in the given directory.
     */
    suspend fun seedComponents() {
        val seeder = Seeder
        val resourceUrl = SeedingService::class.java.classLoader.getResource("components/metadata")

        if (resourceUrl == null) {
            println("ERROR: Components metadata directory not found in resources")
            return
        }

        // Convert URL to file path
        val metadataPath = File(resourceUrl.toURI()).path

        try {
            // Process the component library synchronously
            runBlocking {
                seeder.processComponentLibrary(metadataPath)
            }
            println("Component library initialization complete")
        } catch (e: Exception) {
            println("ERROR: Failed to initialize component library: ${e.message}")
        }
    }
}