package kcl.seg.rtt.chat_history

import io.ktor.server.application.*
import io.ktor.server.routing.*
import kcl.seg.rtt.chat_history.routes.*
import io.ktor.server.auth.*

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

        fun getUploadDirectory(): String {
            return uploadDirectory
        }
    }
}

fun Application.chatModule() {
    routing {
        authenticate("jwt-verifier") {
            chatRoutes()
            jsonRoutes()
            uploadRoutes(ChatEndpoint.getUploadDirectory())
        }
    }
}