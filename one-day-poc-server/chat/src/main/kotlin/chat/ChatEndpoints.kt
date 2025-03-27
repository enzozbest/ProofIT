package chat

import chat.routes.setGetConversationRoute
import chat.routes.setGetHistoryRoute
import chat.routes.setGetPrototypeRoute
import chat.routes.setJsonRoute
import chat.routes.setJsonRouteRetrieval
import chat.routes.uploadRoutes
import io.ktor.server.routing.Route

const val CHAT = "/api/chat"
const val GET = "$CHAT/history"
const val JSON = "$CHAT/json"
const val UPLOAD = "$CHAT/upload"

object ChatEndpoints {
    var UPLOAD_DIR: String = "uploads"

    fun Route.setChatRoutes() {
        setGetHistoryRoute()
        setGetConversationRoute()
        setGetPrototypeRoute()
    }

    fun Route.setJsonRoutes() {
        setJsonRoute()
        setJsonRouteRetrieval()
    }

    fun Route.setUploadRoutes() {
        uploadRoutes(UPLOAD_DIR)
    }
}
