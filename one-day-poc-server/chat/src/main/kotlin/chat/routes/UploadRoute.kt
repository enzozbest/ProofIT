package chat.routes

import chat.Request
import chat.UPLOAD
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDateTime

/**
 * A serializable data class representing the response to be sent after file upload.
 *
 * @property time Timestamp string indicating when the response was created
 * @property message Response message containing feedback about the upload
 */
@Serializable
data class Response(
    val time: String,
    val message: String,
)

/**
 * Configures a POST route that handles multipart file upload requests.
 *
 * This route processes uploaded files along with optional description and message data.
 * It stores files in the specified upload directory and generates a response with
 * information about the uploaded content.
 *
 * @receiver The Route on which this endpoint will be registered
 * @param uploadDir Base directory path where uploaded files will be stored
 */
fun Route.uploadRoutes(uploadDir: String) {
    post(UPLOAD) {
        val uploadData = UploadData()
        val uploadDir = createUploadDirectory(uploadDir)

        val multipartData = call.receiveMultipart()
        multipartData.forEachPart { part ->
            handlePart(part, uploadDir, uploadData, call)
        }

        respondToUpload(call, uploadData)
    }
}

/**
 * Container class for tracking data during the file upload process.
 *
 * @property fileDescription Optional description text provided with the upload
 * @property fileName Name of the uploaded file (with timestamp modification)
 * @property message Optional structured message data submitted with the upload
 * @property response The response object to be sent back to the client
 */
private data class UploadData(
    var fileDescription: String = "",
    var fileName: String = "",
    var message: Request? = null,
    var response: Response? = null,
)

/**
 * Creates the upload directory if it doesn't exist.
 *
 * @param dir Path to the directory where uploaded files should be stored
 * @return A File object representing the upload directory
 */
private fun createUploadDirectory(dir: String): File {
    val uploadDir = File(dir)
    // val uploadDir = File(application.environment.config.property("upload.dir").getString())
    if (!uploadDir.exists()) {
        uploadDir.mkdirs()
    }
    return uploadDir
}

/**
 * Processes each part of the multipart data according to its type.
 *
 * This function dispatches different part types to appropriate handlers and ensures
 * that resources are properly disposed after processing.
 *
 * @param part The multipart data part to be processed
 * @param uploadDir The directory where files will be saved
 * @param uploadData Container for tracking upload processing state
 * @param call The ApplicationCall for responding if needed
 */
private suspend fun handlePart(
    part: PartData,
    uploadDir: File,
    uploadData: UploadData,
    call: ApplicationCall,
) {
    when (part) {
        is PartData.FormItem -> handleFormItem(part, uploadData, call)
        is PartData.FileItem -> handleFileItem(part, uploadDir, uploadData)
        else -> {}
    }
    part.dispose()
}

/**
 * Processes form items from the multipart request.
 *
 * Handles text data like descriptions and JSON messages.
 *
 * @param part The form item part to process
 * @param uploadData Container for tracking upload processing state
 * @param call The ApplicationCall for responding in case of errors
 */
private suspend fun handleFormItem(
    part: PartData.FormItem,
    uploadData: UploadData,
    call: ApplicationCall,
) {
    when (part.name) {
        "description" -> uploadData.fileDescription = part.value
        "message" -> handleMessagePart(part.value, uploadData, call)
    }
}

/**
 * Processes a JSON message submitted with the upload.
 *
 * Attempts to parse the message string into a Request object and generate a
 * response. If parsing fails, responds with an error message.
 *
 * @param value The JSON string to parse
 * @param uploadData Container for tracking upload processing state
 * @param call The ApplicationCall for responding in case of errors
 */
private suspend fun handleMessagePart(
    value: String,
    uploadData: UploadData,
    call: ApplicationCall,
) {
    runCatching {
        uploadData.message = Json.decodeFromString(value)
        uploadData.response =
            uploadData.message?.let {
                Response(
                    time = LocalDateTime.now().toString(),
                    message = "${it.prompt}, ${it.userID}!",
                )
            }
    }.onFailure {
        call.respondText(
            text = "Invalid request: ${it.message}",
            status = HttpStatusCode.BadRequest,
        )
    }
}

/**
 * Processes an uploaded file item from the multipart request.
 *
 * Reads the file data, generates a timestamped filename to prevent conflicts,
 * and saves the file to the upload directory.
 *
 * @param part The file item part to process
 * @param uploadDir The directory where the file will be saved
 * @param uploadData Container for tracking upload processing state
 */
private suspend fun handleFileItem(
    part: PartData.FileItem,
    uploadDir: File,
    uploadData: UploadData,
) {
    uploadData.fileName = generateTimestampedFileName(part.originalFileName)
    val fileBytes = part.provider().readRemaining().readByteArray()
    File("$uploadDir/${uploadData.fileName}").writeBytes(fileBytes)
}

/**
 * Sends an appropriate response after processing the upload.
 *
 * If a structured response object is available, it will be sent as JSON.
 * Otherwise, a simple text response is sent containing the file description
 * and storage location.
 *
 * @param call The ApplicationCall used to send the response
 * @param uploadData Container with the upload state and response information
 */
private suspend fun respondToUpload(
    call: ApplicationCall,
    uploadData: UploadData,
) {
    uploadData.response?.let { response ->
        call.respond(response)
    } ?: call.respondText("${uploadData.fileDescription} is uploaded to 'uploads/${uploadData.fileName}'")
}

/**
 * Generates a unique filename by appending a timestamp to the original file name.
 *
 * This prevents filename conflicts in the upload directory and preserves the original
 * file extension if present. If the original filename is null or blank, a generic
 * name with timestamp will be used.
 *
 * @param originalFileName The original name of the uploaded file, potentially null
 * @return A new timestamped filename that preserves the original extension
 */
internal fun generateTimestampedFileName(originalFileName: String?): String {
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
