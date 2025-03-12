package seeding

import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL

/**
 * Internal service responsible for initializing the system with component data.
 *
 * This singleton object provides functionality for seeding the application with
 * component metadata from JSON-LD files stored in the resources directory.
 */
internal object SeedingService {
    var logger: Logger = LoggerFactory.getLogger(Seeder::class.java)

    /**
     * Seeds the component library by processing all JSON-LD files in the resources.
     *
     * This method locates the components metadata directory in the application's
     * resources, then uses the Seeder to process all JSON-LD files found within.
     * Any errors during seeding are logged but don't throw exceptions.
     */
    internal fun seedComponents() {
        val seeder = Seeder
        val resourceUrl = getResourceUrl("components/metadata")

        if (resourceUrl == null) {
            logger.error("ERROR: Components metadata directory not found in resources")
            return
        }

        // Convert URL to file path
        val metadataPath = File(resourceUrl.toURI()).path

        try {
            // Process the component library synchronously
            runBlocking {
                seeder.processComponentLibrary(metadataPath)
            }
        } catch (e: Exception) {
            logger.error("ERROR: Failed to initialize component library: ${e.message}")
        }
    }

    /**
     * Retrieves a URL for a resource path within the application's classpath.
     *
     * @param path The relative path to the resource
     * @return URL to the resource, or null if not found
     */
    internal fun getResourceUrl(path: String): URL? = SeedingService::class.java.classLoader.getResource(path)
}
