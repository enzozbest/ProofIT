package kcl.seg.rtt.chat_history.routes

import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.io.readByteArray
import java.io.File

/*
    * This route is used to upload files to the server, can be of any type#
    * It creates an upload dir for now as it is not linked to an s3 bucket yet
 */
fun Route.uploadRoutes() {
    var fileDescription = ""
    var fileName = ""

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
                    fileDescription = part.value
                }

                is PartData.FileItem -> {
                    fileName = part.originalFileName as String
                    val fileBytes = part.provider().readRemaining().readByteArray()
                    File("$uploadDir/$fileName${System.currentTimeMillis()}").writeBytes(fileBytes)
                }

                else -> {}
            }
            part.dispose()
        }

        call.respondText("$fileDescription is uploaded to 'uploads/$fileName'")
    }
}