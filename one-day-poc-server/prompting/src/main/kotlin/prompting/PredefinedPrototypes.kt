package prompting

import prototype.services.PredefinedPrototypeService

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
