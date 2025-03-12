package database.tables.templates

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory

class TemplateRepository(
    private val db: Database,
) {
    companion object {
        private val IO_DISPATCHER = Dispatchers.IO
    }

    val logger = LoggerFactory.getLogger(TemplateRepository::class.java)

    /**
     * Function to save a Template to the database
     * @param template The [tables.templates.Template] to save
     * @return A Result object containing the success or failure of the operation
     */
    suspend fun saveTemplateToDB(template: Template): Result<Unit> =
        runCatching {
            if (template.id.isBlank()) {
                throw IllegalArgumentException("Template ID cannot be empty")
            }

            newSuspendedTransaction(IO_DISPATCHER, db) {
                val existingTemplate = TemplateEntity.Companion.findById(template.id)
                if (existingTemplate != null) {
                    existingTemplate.fileURI = template.fileURI
                } else {
                    TemplateEntity.Companion.new(template.id) {
                        fileURI = template.fileURI
                    }
                }
            }
        }

    /**
     * Function to retrieve a Prototype from the database by its ID
     * @param id The UUID of the Prototype to retrieve
     * @return A Result object containing the Prototype if it exists, or null if it does not
     */
    suspend fun getTemplateFromDB(id: String): Template? =
        runCatching {
            newSuspendedTransaction(IO_DISPATCHER, db) {
                TemplateEntity.Companion.findById(id)?.toTemplate()
            }
        }.onFailure { e ->
            logger.error("Error retrieving template with ID $id: ${e.message}", e)
        }.getOrNull()
}
