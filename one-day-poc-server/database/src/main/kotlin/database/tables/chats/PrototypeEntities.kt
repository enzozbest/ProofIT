package database.tables.chats

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import java.util.UUID

class PrototypeEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PrototypeEntity>(PrototypeTable)
    
    var message by ChatMessageEntity referencedOn PrototypeTable.messageId
    var filesJson by PrototypeTable.filesJson
    var version by PrototypeTable.version
    var isSelected by PrototypeTable.isSelected
    var timestamp by PrototypeTable.timestamp
    
    fun toPrototype(): Prototype {
        return Prototype(
            id = id.value.toString(),
            messageId = message.id.value.toString(),
            filesJson = filesJson,
            version = version,
            isSelected = isSelected,
            timestamp = timestamp
        )
    }
}