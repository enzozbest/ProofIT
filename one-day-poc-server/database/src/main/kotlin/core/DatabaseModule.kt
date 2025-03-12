package kcl.seg.rtt.database.core

import core.DatabaseManager
import org.jetbrains.exposed.sql.Database

/**
 * Object to encapsulate database utilities
 */
internal object PoCDatabase {
    val database: Database by lazy { DatabaseManager.init() }
}
