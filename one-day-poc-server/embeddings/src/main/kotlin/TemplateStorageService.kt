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
    private val logger = LoggerFactory.getLogger(TemplateService::class.java)

    /**
     * Creates a new template and stores it in the database.
     *
     * @param fileURI The URI of the template file
     * @return Result containing the created template or an error
     */
    suspend fun createTemplate(fileURI: String): Result<Template> {
        val templateId = UUID.randomUUID().toString()
        val template = Template(id = templateId, fileURI = fileURI)

        val result = DatabaseManager.templateRepository().saveTemplateToDB(template)

        return result.map { template }
    }

    /**
     * Retrieves a template by its ID.
     *
     * @param id The ID of the template to retrieve
     * @return Result containing the found template or null if not found
     */
    suspend fun getTemplateById(templateId: UUID): Result<Template?> {
        val uuidString = templateId.toString()

        return DatabaseManager.templateRepository().getTemplateFromDB(uuidString)
            .mapCatching { template ->
                template ?: throw NotFoundException("No template found with ID: $uuidString")
            }
            .onFailure {
                logger.error("Error retrieving template with ID $uuidString: ${it.message}", it)
            }
    }

}