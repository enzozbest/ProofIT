package prompting.helpers

import prototype.PrototypeMain
import prototype.helpers.OllamaResponse

object PrototypeInteractor {
    suspend fun prompt(
        prompt: String,
        model: String,
    ): OllamaResponse? = PrototypeMain(model).prompt(prompt)
}
