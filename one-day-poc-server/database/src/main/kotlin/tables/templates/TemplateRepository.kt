package tables.templates

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class TemplateRepository(
    private val db: Database,
) {
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
    suspend fun getTemplateFromDB(id: String): Result<Template?> =
        runCatching {
            newSuspendedTransaction(Dispatchers.IO, db) {
                TemplateEntity.findById(id)?.toTemplate()
            }
        }
}
