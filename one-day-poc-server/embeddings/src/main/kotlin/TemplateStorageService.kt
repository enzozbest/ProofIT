import org.slf4j.LoggerFactory
import tables.templates.Template
import java.util.UUID
import core.DatabaseManager

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

        return try {
            val result = DatabaseManager.templateRepository().saveTemplateToDB(template)
            if (result.isSuccess) {
                Result.success(template)
            } else {
                val exception = result.exceptionOrNull() ?: Exception("Unknown error during template creation")
                logger.error("Failed to create template: ${exception.message}", exception)
                Result.failure(exception)
            }
        } catch (e: Exception) {
            logger.error("Exception during template creation: ${e.message}", e)
            Result.failure(e)
        }
    }

}