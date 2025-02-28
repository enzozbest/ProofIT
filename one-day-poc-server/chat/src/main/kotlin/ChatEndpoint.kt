package kcl.seg.rtt.chat

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import kcl.seg.rtt.chat.routes.*

const val CHAT = "/api/chat"
const val GET = "$CHAT/get"
const val JSON = "$CHAT/json"
const val UPLOAD = "$CHAT/upload"

class ChatEndpoint {
    companion object {
        private const val DEFAULT_UPLOAD_DIR = "uploads"
        private var uploadDirectory: String = DEFAULT_UPLOAD_DIR

        fun setUploadDirectory(dir: String) {
            uploadDirectory = dir
        }

        fun resetToDefault() {
            uploadDirectory = DEFAULT_UPLOAD_DIR
        }

        fun getUploadDirectory(): String = uploadDirectory
    }
}

fun Application.chatModule() {
    routing {
        jsonRoutes()
        authenticate("jwt-verifier") {
            chatRoutes()

            uploadRoutes(ChatEndpoint.getUploadDirectory())
        }
    }
}
