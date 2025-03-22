package database.tables.prototypes

import database.tables.chats.ChatMessageTable
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

/**
 * Test-specific table for Prototypes
 */
internal object TestPrototypes : UUIDTable("test_prototypes") {
    val userId = text("userId")
    val userPrompt = text("prompt")
    val fullPrompt = text("fullPrompt")
    val path = varchar("path", 255).nullable()
    val createdAt = timestamp("created_at")
    val projectName = text("name")
}

/**
 * Test-specific entity for Prototypes. This is used for Exposed to interact with the database via DAO.
 */
class TestPrototypeEntity(
    id: EntityID<UUID>,
) : UUIDEntity(id) {
    companion object : UUIDEntityClass<TestPrototypeEntity>(TestPrototypes)

    var userId by TestPrototypes.userId
    var userPrompt by TestPrototypes.userPrompt
    var fullPrompt by TestPrototypes.fullPrompt
    var s3Key by TestPrototypes.path
    var createdAt by TestPrototypes.createdAt
    var projectName by TestPrototypes.projectName

    /**
     * Converts the entity to a Prototype object
     */
    fun toPrototype() =
        Prototype(
            id = this.id.value,
            userId = this.userId,
            userPrompt = this.userPrompt,
            fullPrompt = this.fullPrompt,
            s3key = this.s3Key,
            createdAt = this.createdAt,
            projectName = this.projectName,
        )
}

/**
 * Test-specific data class for Prototypes
 * @property id The UUID of the prototype
 * @property userId The ID of the user who created the prototype
 * @property userPrompt The prompt submitted by the user
 * @property fullPrompt The prompt which was actually sent to the LLM
 * @property s3key The S3 key of the prototype's source
 * @property createdAt The timestamp of when the prototype was created
 * @property projectName The name of the prototype
 */
data class TestPrototype(
    val id: UUID,
    var userId: String,
    var userPrompt: String,
    var fullPrompt: String,
    val s3key: String?,
    val createdAt: Instant,
    val projectName: String,
)