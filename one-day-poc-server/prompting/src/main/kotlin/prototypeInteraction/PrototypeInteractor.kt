package kcl.seg.rtt.prompting.prototypeInteraction

import kcl.seg.rtt.prototype.OllamaResponse
import kcl.seg.rtt.prototype.PrototypeMain

object PrototypeInteractor {
    suspend fun prompt(
        prompt: String,
        model: String,
    ): OllamaResponse? = PrototypeMain(model).prompt(prompt)
}
