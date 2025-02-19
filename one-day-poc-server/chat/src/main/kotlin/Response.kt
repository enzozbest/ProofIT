package kcl.seg.rtt.chat

import kotlinx.serialization.Serializable

@Serializable
data class Response (
    val time: String,
    val message: String
)