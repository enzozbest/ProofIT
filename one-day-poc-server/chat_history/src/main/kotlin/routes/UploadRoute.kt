package kcl.seg.rtt.chat_history.routes

import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kcl.seg.rtt.chat_history.Request
import kcl.seg.rtt.chat_history.Response
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime

/*
    * This route is used to upload files to the server, can be of any type#
    * It creates an upload dir for now as it is not linked to an s3 bucket yet
 */
fun Route.uploadRoutes() {
    var fileDescription = ""
    var fileName = ""
    var message: Request? = null
    var response: Response? = null

    post("/upload") {
//        val uploadDir = File(application.environment.config.property("upload.dir").getString())
        val uploadDir = File("uploads")
        if (!uploadDir.exists()) {
            uploadDir.mkdirs()
        }

        val multipartData = call.receiveMultipart()

        multipartData.forEachPart { part ->
            when (part) {
                is PartData.FormItem -> {
                    if(part.name == "description"){
                        fileDescription = part.value
                    } else if (part.name == "message") {
                        message = Json.decodeFromString(part.value)
                        response = Response(
                            time = LocalDateTime.now().toString(),
                            message = "${message?.prompt}, ${message?.userID}!"
                        )
                    }
                }

                is PartData.FileItem -> {
                    fileName = generateTimestampedFileName(part.originalFileName as String)
                    val fileBytes = part.provider().readRemaining().readByteArray()
                    File("$uploadDir/$fileName").writeBytes(fileBytes)
                }

                else -> {}
            }
            part.dispose()
        }
        call.respondText("$fileDescription is uploaded to 'uploads/$fileName'")
        if(response != null) {
            call.respond(response!!)
        }
    }
}

/*
    * This function generates a timestamped file name for the uploaded file to avoid conflicts
 */
fun generateTimestampedFileName(originalFileName: String?): String {
    val timestamp = System.currentTimeMillis()
    if (originalFileName.isNullOrBlank()) return "unknown_$timestamp"

    val lastDotIndex = originalFileName.lastIndexOf('.')
    return if (lastDotIndex != -1) {
        val name = originalFileName.substring(0, lastDotIndex)
        val extension = originalFileName.substring(lastDotIndex)
        "${name}_$timestamp$extension"
    } else {
        "${originalFileName}_$timestamp"
    }
}