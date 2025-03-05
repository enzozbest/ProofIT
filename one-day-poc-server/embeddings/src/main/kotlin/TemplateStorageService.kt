import org.slf4j.LoggerFactory
import core.DatabaseManager
import tables.templates.Template
import java.util.UUID

/**
 * Service responsible for storing templates to the database.
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

        return DatabaseManager.templateRepository().saveTemplateToDB(template)
            .map { template }
            .onSuccess { logger.info("Template created successfully with ID: ${template.id}") }
            .onFailure { logger.error("Failed to create template: ${it.message}", it) }
    }

}