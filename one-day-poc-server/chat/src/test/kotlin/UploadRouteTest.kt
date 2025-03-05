package kcl.seg.rtt.chat

import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kcl.seg.rtt.chat.routes.generateTimestampedFileName
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import java.io.File
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlinx.serialization.Serializable

@Serializable
data class Response(
    val message: String,
    val time: String
)

class UploadRoutesTest : BaseAuthenticationServer() {

    @BeforeEach
    fun setup() {
        ChatEndpoint.setUploadDirectory("test_uploads")
        File("test_uploads").deleteRecursively()
    }

    @AfterEach
    fun cleanup() {
        File("test_uploads").deleteRecursively()
        ChatEndpoint.resetToDefault()
    }

    @Test
    fun `Test valid file upload and directory is created on first upload`() = testApplication {
        setupTestApplication()
        val testDir = File("test_uploads")
        assertFalse(testDir.exists())

        val response = client.post(UPLOAD) {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
            setBody(MultiPartFormDataContent(
                formData {
                    append("file", "test".toByteArray(), Headers.build {
                        append(HttpHeaders.ContentType, "text/plain")
                        append(HttpHeaders.ContentDisposition, "filename=\"test.txt\"")
                    })
                }
            ))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(testDir.exists())
        assertTrue(testDir.isDirectory)
    }

    @Test
    fun `Test upload to existing directory succeeds`() = testApplication {
        setupTestApplication()
        val testDir = File("test_uploads")
        testDir.mkdirs()

        val response = client.post(UPLOAD) {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
            setBody(MultiPartFormDataContent(
                formData {
                    append("file", "test".toByteArray(), Headers.build {
                        append(HttpHeaders.ContentType, "text/plain")
                        append(HttpHeaders.ContentDisposition, "filename=\"test.txt\"")
                    })
                }
            ))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(testDir.exists())
    }

    @Test
    fun `Test multipart upload with file and description`() = testApplication {
        setupTestApplication()
        val testDir = File("test_uploads")

        val response = client.post(UPLOAD) {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
            setBody(MultiPartFormDataContent(
                formData {
                    append("description", "Test Description")
                    append("file", "test content".toByteArray(), Headers.build {
                        append(HttpHeaders.ContentType, "text/plain")
                        append(HttpHeaders.ContentDisposition, "filename=\"test.txt\"")
                    })
                }
            ))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Test Description"))
        assertTrue(testDir.listFiles()?.any { it.name.startsWith("test") } ?: false)
    }

    @Test
    fun `Test multipart upload with message JSON`() = testApplication {
        setupTestApplication()

        val response = client.post(UPLOAD) {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
            setBody(MultiPartFormDataContent(
                formData {
                    append("message", """
                        {
                            "userID": "testUser",
                            "time": "2025-01-01T12:00:00",
                            "prompt": "Hello"
                        }
                    """.trimIndent())
                    append("file", "test".toByteArray(), Headers.build {
                        append(HttpHeaders.ContentType, "text/plain")
                        append(HttpHeaders.ContentDisposition, "filename=\"test.txt\"")
                    })
                }
            ))
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `Test multipart upload handles invalid message JSON`() = testApplication {
        setupTestApplication()

        val response = client.post(UPLOAD) {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
            setBody(MultiPartFormDataContent(
                formData {
                    append("message", "invalid json")
                    append("file", "test".toByteArray(), Headers.build {
                        append(HttpHeaders.ContentType, "text/plain")
                        append(HttpHeaders.ContentDisposition, "filename=\"test.txt\"")
                    })
                }
            ))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `Test filename with no extension adds timestamp`() {
        val originalFileName = "testfile"
        val result = generateTimestampedFileName(originalFileName)

        val parts = result.split("_")
        assertEquals(2, parts.size)
        assertEquals("testfile", parts[0])
        assertTrue(parts[1].toLongOrNull() != null)
    }

    @Test
    fun `Test null filename returns unknown with timestamp`() {
        val result = generateTimestampedFileName(null)

        val parts = result.split("_")
        assertEquals(2, parts.size)
        assertEquals("unknown", parts[0])
        assertTrue(parts[1].toLongOrNull() != null)
    }

    @Test
    fun `Test empty filename returns unknown with timestamp`() {
        val result = generateTimestampedFileName("")

        val parts = result.split("_")
        assertEquals(2, parts.size)
        assertEquals("unknown", parts[0])
        assertTrue(parts[1].toLongOrNull() != null)
    }

    @Test
    fun `Test timestamp is current time`() {
        val beforeTest = System.currentTimeMillis()
        val result = generateTimestampedFileName("test")
        val afterTest = System.currentTimeMillis()

        val timestamp = result.split("_")[1].toLong()
        assertTrue(timestamp >= beforeTest)
        assertTrue(timestamp <= afterTest)
    }

    @Test
    fun `Test message response format is correct`() = testApplication {
        setupTestApplication()

        val response = client.post(UPLOAD) {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
            setBody(MultiPartFormDataContent(
                formData {
                    append("message", """
                    {
                        "userID": "testUser",
                        "time": "2025-01-01T12:00:00",
                        "prompt": "Hello"
                    }
                """.trimIndent())
                    append("file", "test".toByteArray(), Headers.build {
                        append(HttpHeaders.ContentType, "text/plain")
                        append(HttpHeaders.ContentDisposition, "filename=\"test.txt\"")
                    })
                }
            ))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseJson = Json.decodeFromString<Response>(response.bodyAsText())
        assertNotNull(responseJson)
        assertEquals("Hello, testUser!", responseJson.message)
    }

    @Test
    fun `Test file upload handling`() = testApplication {
        setupTestApplication()
        val testContent = "test content"
        val fileName = "test.txt"

        val response = client.post(UPLOAD) {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
            setBody(MultiPartFormDataContent(
                formData {
                    append("file", testContent.toByteArray(), Headers.build {
                        append(HttpHeaders.ContentType, "text/plain")
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    })
                }
            ))
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val testDir = File("test_uploads")
        assertTrue(testDir.exists())

        val uploadedFile = testDir.listFiles()?.firstOrNull { it.name.startsWith("test_") && it.name.endsWith(".txt") }
        assertNotNull(uploadedFile, "Uploaded file should exist")

        if (uploadedFile != null) {
            assertEquals(testContent, uploadedFile.readText())
        }

        val fileNameParts = uploadedFile?.name?.split("_")
        assertEquals("test", fileNameParts?.get(0) ?: "Filename should start with original filename")
        assertTrue(
            fileNameParts?.get(1)?.substringBefore(".txt")?.toLongOrNull() != null,
            "Filename should contain timestamp")
    }
}