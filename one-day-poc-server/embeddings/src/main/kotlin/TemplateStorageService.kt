import core.DatabaseManager
import org.slf4j.LoggerFactory
import tables.templates.Template
import java.util.UUID

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

        val result =
            runCatching {
                DatabaseManager.templateRepository().saveTemplateToDB(template)
            }
        return if (result.isSuccess) {
            templateId
        } else {
            logger.info("Failed to store template $templateId")
            null
        }
    }

    /**
     * Retrieves a template by its ID.
     *
     * @param templateId The ID of the template to retrieve
     * @return Result containing the found template or null if not found
     */
    suspend fun getTemplateById(templateId: UUID): Template? {
        val uuidString = templateId.toString()

        val template =
            runCatching {
                DatabaseManager.templateRepository().getTemplateFromDB(uuidString)
            }.getOrElse {
                logger.error("Error retrieving template with ID $uuidString: ${it.message}", it)
                null
            }

        return template ?: run {
            logger.info("Failed to get template with the following id: $uuidString")
            null
        }
    }
}
