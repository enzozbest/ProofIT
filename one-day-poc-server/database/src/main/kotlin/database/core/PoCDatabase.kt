package database.core

import org.jetbrains.exposed.sql.Database

/**
 * Object to encapsulate database utilities
 */
internal object PoCDatabase {
    internal val database: Database by lazy { DatabaseManager.init() }
}
