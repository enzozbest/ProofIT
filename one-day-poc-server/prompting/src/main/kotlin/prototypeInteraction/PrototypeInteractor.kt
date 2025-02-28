package kcl.seg.rtt.prompting.prototypeInteraction

import kcl.seg.rtt.prototype.LlmResponse

object PrototypeInteractor {
    suspend fun prompt(
        prompt: String,
        model: String,
    ): LlmResponse? = null
}
