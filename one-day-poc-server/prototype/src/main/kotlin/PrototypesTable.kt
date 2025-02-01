/**
 * Temporary location for database storing prototypes
 * Eventually to be put in root directory and be a larger
 * database with multiple tables, relating users to their
 * prototypes
 */

package kcl.seg.rtt.prototype


import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table


object PrototypesTable : Table("prototypes") {
    val id: Column<Int> = integer("id").autoIncrement()
    val prototype: Column<String> = text("prototype")
    override val primaryKey = PrimaryKey(id)
}
