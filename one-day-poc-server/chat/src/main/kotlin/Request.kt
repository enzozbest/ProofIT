package kcl.seg.rtt.chat

import kotlinx.serialization.Serializable

@Serializable
data class Request(
    val userID: String,
    val time: String,
    val prompt: String
)