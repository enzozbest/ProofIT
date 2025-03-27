package prompting

import kotlinx.serialization.json.JsonObject
import prototype.PredefinedPrototypeService
import prototype.PrototypeTemplate

data class PredefinedPrototypeTemplate(
    val chatMessage: String,
    val files: String,
)

object PredefinedPrototypes {
    fun run(prompt: String): PredefinedPrototypeTemplate {
        val prototypeTemplate = PredefinedPrototypeService.getPrototypeForPrompt(prompt)

        return PredefinedPrototypeTemplate(
            chatMessage = prototypeTemplate.chatMessage,
            files = prototypeTemplate.files.toString(),
        )
    }
}