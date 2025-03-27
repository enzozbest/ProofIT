package templates

import database.core.DatabaseManager
import database.tables.templates.Template
import java.util.UUID

/**
 * Service responsible for storing and retrieving templates from the database.
 * Uses DatabaseManager to access the TemplateRepository.
 */
object TemplateStorageService {
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
            null
        }
    }

    /**
     * Retrieves a template by its ID.
     *
     * @param templateId The ID of the template to retrieve
     * @return Result containing the found template or null if not found
     */
    suspend fun getTemplateById(templateId: String): Template? {
        val template =
            runCatching {
                DatabaseManager.templateRepository().getTemplateFromDB(templateId)
            }.getOrElse {
                null
            }

        return template ?: run {
            null
        }
    }
}
