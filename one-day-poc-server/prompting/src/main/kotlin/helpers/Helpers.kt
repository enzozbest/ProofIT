@file:Suppress("ktlint:standard:filename")

package helpers

import kotlinx.serialization.Serializable

@Serializable
data class SanitisedPromptResult(
    val prompt: String,
    val keywords: List<String>,
)

@Serializable
data class Response(
    val time: String,
    val message: String,
)
