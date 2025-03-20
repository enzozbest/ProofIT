package prompting.helpers

import prototype.PrototypeMain
import prototype.helpers.OllamaOptions
import prototype.helpers.OllamaResponse

object PrototypeInteractor {
    suspend fun prompt(
        prompt: String,
        model: String,
        options: OllamaOptions,
    ): OllamaResponse? = PrototypeMain(model).prompt(prompt, options)
}
