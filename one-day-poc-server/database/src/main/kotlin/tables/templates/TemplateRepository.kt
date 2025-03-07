package tables.templates

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory

class TemplateRepository(
    private val db: Database,
) {
    var logger = LoggerFactory.getLogger(TemplateRepository::class.java)

    /**
     * Function to save a Template to the database
     * @param template The [Template] to save
     * @return A Result object containing the success or failure of the operation
     */
    suspend fun saveTemplateToDB(template: Template): Result<Unit> =
        runCatching {
            newSuspendedTransaction(Dispatchers.IO, db) {
                TemplateEntity.new(template.id) {
                    fileURI = template.fileURI
                }
            }
        }

    /**
     * Function to retrieve a Prototype from the database by its ID
     * @param id The UUID of the Prototype to retrieve
     * @return A Result object containing the Prototype if it exists, or null if it does not
     */
    suspend fun getTemplateFromDB(id: String): Template? =
        try {
            newSuspendedTransaction(Dispatchers.IO, db) {
                TemplateEntity.findById(id)?.toTemplate()
            }
        } catch (e: Exception) {
            logger.error("Error retrieving template with ID $id: ${e.message}", e)
            null
        }
}
