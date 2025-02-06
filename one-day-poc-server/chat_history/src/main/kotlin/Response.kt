package kcl.seg.rtt.chat_history

import kotlinx.serialization.Serializable

@Serializable
data class Response (
    val time: String,
    val message: String
)