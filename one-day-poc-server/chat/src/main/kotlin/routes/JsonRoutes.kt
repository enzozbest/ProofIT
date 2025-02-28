package kcl.seg.rtt.chat.routes

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kcl.seg.rtt.chat.JSON
import kcl.seg.rtt.chat.Request
import kcl.seg.rtt.prompting.PromptingMain

private var client = HttpClient(CIO)
private var endpoint = "http://localhost:8000/api/prototype/generate"

internal fun setTestClient(testClient: HttpClient) {
    client = testClient
}

internal fun setTestEndpoint(endpoint: String) {
    kcl.seg.rtt.chat.routes.endpoint = endpoint
}

fun Route.jsonRoutes() {
    post(JSON) {
        val request: Request =
            runCatching {
                call.receive<Request>()
            }.getOrElse {
                return@post call.respondText(
                    "Invalid request ${it.message}",
                    status = HttpStatusCode.BadRequest,
                )
            }
        handleJsonRequest(request, call)
    }
}

private suspend fun handleJsonRequest(
    request: Request,
    call: ApplicationCall,
) {
    call.respondText(PromptingMain().run(request.prompt)?.response!!) // Start the prompting workflow
}

// private suspend fun makePromptRequest(): HttpResponse =
//    client.post(ENDPOINT) {
//        contentType(ContentType.Application.Json)
//        setBody(
//            """{
//            |"prompt": "${sanitisedResult.prompt}"
//            |"keywords": "${sanitisedResult.keywords}"
//            |}
//            """.trimMargin(),
//        )
//    }
