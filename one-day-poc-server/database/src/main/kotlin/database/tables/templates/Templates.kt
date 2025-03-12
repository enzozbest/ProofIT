package database.tables.templates

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable

/**
 * Table for Prototypes
 */
internal object Templates : IdTable<String>("templates") {
    override val id = text("id").entityId()
    override val primaryKey = PrimaryKey(id)
    val fileURI = text("fileURI")
}

/**
 * Entity for Templates. This is used for Exposed to interact with the database via DAO.
 */
class TemplateEntity(
    id: EntityID<String>,
) : Entity<String>(id) {
    companion object : EntityClass<String, TemplateEntity>(Templates)

    var fileURI by Templates.fileURI

    /**
     * Converts the entity to a Template object
     */
    fun toTemplate() =
        Template(
            id = id.value,
            fileURI = fileURI,
        )
}

/**
 * Data class for Templates
 * @property id The identifier of the template
 * @property fileURI The URI of the file referenced here.
 */
data class Template(
    val id: String,
    val fileURI: String,
)
