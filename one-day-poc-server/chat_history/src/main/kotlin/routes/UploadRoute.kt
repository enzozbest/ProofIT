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
import io.ktor.server.application.ApplicationCall

/*
    * This route is used to upload files to the server, can be of any type#
    * It creates an upload dir for now as it is not linked to an s3 bucket yet
 */
fun Route.uploadRoutes(uploadDir: String) {
    post("/upload") {
        val uploadData = UploadData()
        val uploadDir = createUploadDirectory(uploadDir)

        val multipartData = call.receiveMultipart()
        multipartData.forEachPart { part ->
            handlePart(part, uploadDir, uploadData)
        }

        // Change the function to not be an extension function
        respondToUpload(call, uploadData)
    }
}

private data class UploadData(
    var fileDescription: String = "",
    var fileName: String = "",
    var message: Request? = null,
    var response: Response? = null
)

private fun createUploadDirectory(dir: String): File {
    val uploadDir = File(dir)
    // val uploadDir = File(application.environment.config.property("upload.dir").getString())
    if (!uploadDir.exists()) {
        uploadDir.mkdirs()
    }
    return uploadDir
}

private suspend fun handlePart(part: PartData, uploadDir: File, uploadData: UploadData) {
    when (part) {
        is PartData.FormItem -> handleFormItem(part, uploadData)
        is PartData.FileItem -> handleFileItem(part, uploadDir, uploadData)
        else -> {}
    }
    part.dispose()
}

private fun handleFormItem(part: PartData.FormItem, uploadData: UploadData) {
    when (part.name) {
        "description" -> uploadData.fileDescription = part.value
        "message" -> handleMessagePart(part.value, uploadData)
    }
}

private fun handleMessagePart(value: String, uploadData: UploadData) {
    uploadData.message = Json.decodeFromString(value)
    uploadData.response = Response(
        time = LocalDateTime.now().toString(),
        message = "${uploadData.message?.prompt}, ${uploadData.message?.userID}!"
    )
}

private suspend fun handleFileItem(
    part: PartData.FileItem,
    uploadDir: File,
    uploadData: UploadData
) {
    uploadData.fileName = generateTimestampedFileName(part.originalFileName as String)
    val fileBytes = part.provider().readRemaining().readByteArray()
    File("$uploadDir/${uploadData.fileName}").writeBytes(fileBytes)
}

private suspend fun respondToUpload(
    call: ApplicationCall,
    uploadData: UploadData
) {
    call.respondText("${uploadData.fileDescription} is uploaded to 'uploads/${uploadData.fileName}'")
    uploadData.response?.let { response ->
        call.respond(response)
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