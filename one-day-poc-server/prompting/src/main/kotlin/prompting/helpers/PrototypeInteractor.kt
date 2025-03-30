package prompting.helpers

import prototype.PrototypeMain
import prototype.helpers.LLMOptions
import prototype.helpers.LLMResponse

object PrototypeInteractor {
    suspend fun prompt(
        prompt: String,
        model: String,
        route: String,
        options: LLMOptions,
    ): LLMResponse? {
        val prototypeMain = PrototypeMain(route = route, model = model)
        return prototypeMain.prompt(prompt, options)
    }
}
