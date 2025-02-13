package kcl.seg.rtt.database.repositories

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant
import java.util.*

/**
 * Table for Prototypes
 */
object Prototypes : UUIDTable("prototypes") {
    val userId = text("userId")
    val userPrompt = text("prompt")
    val fullPrompt = text("fullPrompt")
    val s3key = varchar("s3_key", 255).nullable()
    val createdAt = timestamp("created_at")
}

/**
 * Entity for Prototypes. This is used for Exposed to interact with the database via DAO.
 */
class PrototypeEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PrototypeEntity>(Prototypes)

    var userId by Prototypes.userId
    var userPrompt by Prototypes.userPrompt
    var fullPrompt by Prototypes.fullPrompt
    var s3Key by Prototypes.s3key
    var createdAt by Prototypes.createdAt

    /**
     * Converts the entity to a Prototype object
     */
    fun toPrototype() = Prototype(
        id = this.id.value,
        userId = this.userId,
        userPrompt = this.userPrompt,
        fullPrompt = this.fullPrompt,
        s3key = this.s3Key,
        createdAt = this.createdAt
    )
}

/**
 * Data class for Prototypes
 * @property id The UUID of the prototype
 * @property userId The ID of the user who created the prototype
 * @property userPrompt The prompt submitted by the user
 * @property fullPrompt The prompt which was actually sent to the LLM
 * @property s3key The S3 key of the prototype's source
 * @property createdAt The timestamp of when the prototype was created
 */
data class Prototype(
    val id: UUID,
    var userId: String,
    var userPrompt: String,
    var fullPrompt: String,
    val s3key: String?,
    val createdAt: Instant
)