import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class ComponentMetadata(
    val components: List<Component>
)

@Serializable
data class Component(
    val descriptiveName: String,
    val fileName: String,
)

class Seeder(private val embeddingService: EmbeddingService) {
    private fun readComponentMetadata(): ComponentMetadata {
        val metadataFile = File("src/main/components/metadata/components.json")
        return Json.decodeFromString(metadataFile.readText())
    }

    private fun readComponentFile(fileName: String): String {
        val componentFile = File("src/main/components/$fileName")
        return componentFile.readText()
    }


    suspend fun seedComponents() {
        val metadata = readComponentMetadata()

        metadata.components.forEach { component ->
            // Read component code
            val code = readComponentFile(component.fileName)

            embeddingService.embedAndStore(
                name = component.descriptiveName,
                code = code
            )
        }
    }
}