package seeding

import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL

internal object SeedingService {
    var logger: Logger = LoggerFactory.getLogger(Seeder::class.java)

    /**
     * Seeds the component library by processing all JSON-LD files in the given directory.
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

    internal fun getResourceUrl(path: String): URL? = SeedingService::class.java.classLoader.getResource(path)
}
