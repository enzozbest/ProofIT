package prompting

import prototype.PredefinedPrototypeService
import prototype.PrototypeTemplate
import java.time.Instant

object PredefinedPrototypes {
    fun run(prompt: String): ServerResponse {
        val prototypeTemplate = PredefinedPrototypeService.getPrototypeForPrompt(prompt)

        return ServerResponse(
            chat = ChatResponse(
                message = prototypeTemplate.chatMessage,
                role = "LLM",
                timestamp = Instant.now().toString(),
                messageId = "0"
            ),
            prototype = PrototypeResponse(
                files = prototypeTemplate.files
            )
        )
    }
}