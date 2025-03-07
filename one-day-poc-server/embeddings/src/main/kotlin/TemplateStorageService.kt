import org.slf4j.LoggerFactory
import tables.templates.Template
import java.util.UUID
import core.DatabaseManager
import io.ktor.server.plugins.*

/**
 * Service responsible for storing and retrieving templates from the database.
 * Uses DatabaseManager to access the TemplateRepository.
 */
object TemplateStorageService {
    var logger = LoggerFactory.getLogger(TemplateService::class.java)

    /**
     * Creates a new template and stores it in the database.
     *
     * @param fileURI The URI of the template file
     * @return template id or null
     */
    suspend fun createTemplate(fileURI: String): String? {
        val templateId = UUID.randomUUID().toString()
        val template = Template(id = templateId, fileURI = fileURI)

        val result = DatabaseManager.templateRepository().saveTemplateToDB(template)

        if (result.isSuccess) {
            return templateId
        } else {
            logger.info("Failed to store template $templateId")
            return null
        }
    }

    /**
     * Retrieves a template by its ID.
     *
     * @param id The ID of the template to retrieve
     * @return Result containing the found template or null if not found
     */
    suspend fun getTemplateById(templateId: UUID): Template? {
        val uuidString = templateId.toString()

        return try {
            val template = DatabaseManager.templateRepository().getTemplateFromDB(uuidString)

            if (template == null) {
                logger.info("Failed to get template with the following id: $uuidString")
                null
            } else {
                template
            }

        } catch (e: Exception) {
            logger.error("Error retrieving template with ID $uuidString: ${e.message}", e)
            null
        }
    }

}