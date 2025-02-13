package tables.prototypes

import kcl.seg.rtt.database.repositories.Prototype
import kcl.seg.rtt.database.repositories.PrototypeEntity
import kcl.seg.rtt.database.repositories.Prototypes
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

/**
 * Class to encapsulate functions related to storing a Prototype in the database
 */
class PrototypeRepository(private val db: Database) {

    /**
     * Function to save a Prototype to the database
     * @param prototype The Prototype to save
     * @return A Result object containing the success or failure of the operation
     */
    suspend fun createPrototype(prototype: Prototype): Result<Unit> =
        kotlin.runCatching {
            newSuspendedTransaction(Dispatchers.IO, db) {
                PrototypeEntity.new(prototype.id) {
                    userId = prototype.userId
                    userPrompt = prototype.userPrompt
                    fullPrompt = prototype.fullPrompt
                    s3Key = prototype.s3key
                    createdAt = prototype.createdAt
                }
            }
        }

    /**
     * Function to retrieve a Prototype from the database by its ID
     * @param id The UUID of the Prototype to retrieve
     * @return A Result object containing the Prototype if it exists, or null if it does not
     */
    suspend fun getPrototype(id: UUID): Result<Prototype?> =
        kotlin.runCatching {
            newSuspendedTransaction(Dispatchers.IO, db) {
                PrototypeEntity.findById(id)?.toPrototype()
            }
        }

    /**
     * Function to retrieve a list of Prototypes from the database by the user ID. In other orders,
     * this function retrieves all Prototypes created by a specific user.
     * @param userId The ID of the user
     * @param page The page number of the results (for pagination in case there are many Prototypes)
     * @param pageSize The number of Prototypes to retrieve per page
     * @return A Result object containing a list of Prototypes if they exist
     */
    suspend fun getPrototypesByUserId(
        userId: String,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<List<Prototype>> =
        kotlin.runCatching {
            newSuspendedTransaction(Dispatchers.IO, db) {
                PrototypeEntity.find { Prototypes.userId eq userId }
                    .orderBy(Prototypes.createdAt to SortOrder.DESC)
                    .limit(pageSize, offset = ((page - 1) * pageSize).toLong())
                    .map { it.toPrototype() }
            }
        }
}