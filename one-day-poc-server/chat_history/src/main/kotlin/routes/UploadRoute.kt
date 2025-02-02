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
                    fileName = generateTimestampedFileName(part.originalFileName as String)
                    val fileBytes = part.provider().readRemaining().readByteArray()
                    File("$uploadDir/$fileName").writeBytes(fileBytes)
                }

                else -> {}
            }
            part.dispose()
        }
        call.respondText("$fileDescription is uploaded to 'uploads/$fileName'")
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