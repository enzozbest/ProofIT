package database.core

import org.jetbrains.exposed.sql.Database

/**
 * Object to encapsulate database utilities.
 * This object exists to ensure the database is only initialised once, and is initialised at the first access attempt.
 * It also guarantees that the database is only initialised lazily, only when it is needed.
 */
internal object PoCDatabase {
    internal val database: Database by lazy { DatabaseManager.init() }

    fun init() {
        database
    }
}
