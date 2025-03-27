package chat.routes

import chat.BaseAuthenticationServer
import chat.ChatEndpoints
import chat.UPLOAD
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UploadRoutesTest : BaseAuthenticationServer() {
    @BeforeEach
    fun setup() {
        ChatEndpoints.UPLOAD_DIR = "test_uploads"
        File("test_uploads").deleteRecursively()
    }

    @AfterEach
    fun cleanup() {
        File("test_uploads").deleteRecursively()
        ChatEndpoints.UPLOAD_DIR = "uploads"
    }

    @Test
    fun `Test valid file upload and directory is created on first upload`() =
        testApplication {
            setupTestApplication()
            val testDir = File("test_uploads")
            assertFalse(testDir.exists())

            val response =
                client.post(UPLOAD) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append(
                                    "file",
                                    "test".toByteArray(),
                                    Headers.build {
                                        append(HttpHeaders.ContentType, "text/plain")
                                        append(HttpHeaders.ContentDisposition, "filename=\"test.txt\"")
                                    },
                                )
                            },
                        ),
                    )
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(testDir.exists())
            assertTrue(testDir.isDirectory)
        }

    @Test
    fun `Test upload to existing directory succeeds`() =
        testApplication {
            setupTestApplication()
            val testDir = File("test_uploads")
            testDir.mkdirs()

            val response =
                client.post(UPLOAD) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append(
                                    "file",
                                    "test".toByteArray(),
                                    Headers.build {
                                        append(HttpHeaders.ContentType, "text/plain")
                                        append(HttpHeaders.ContentDisposition, "filename=\"test.txt\"")
                                    },
                                )
                            },
                        ),
                    )
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(testDir.exists())
        }

    @Test
    fun `Test multipart upload with file and description`() =
        testApplication {
            setupTestApplication()
            val testDir = File("test_uploads")

            val response =
                client.post(UPLOAD) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("description", "Test Description")
                                append(
                                    "file",
                                    "test content".toByteArray(),
                                    Headers.build {
                                        append(HttpHeaders.ContentType, "text/plain")
                                        append(HttpHeaders.ContentDisposition, "filename=\"test.txt\"")
                                    },
                                )
                            },
                        ),
                    )
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("Test Description"))
            assertTrue(testDir.listFiles()?.any { it.name.startsWith("test") } == true)
        }

    @Test
    fun `Test multipart upload with message JSON`() =
        testApplication {
            setupTestApplication()

            val response =
                client.post(UPLOAD) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append(
                                    "message",
                                    """
                                    {
                                        "userID": "testUser",
                                        "time": "2025-01-01T12:00:00",
                                        "prompt": "Hello",
                                        "conversationId": "test-conversation-id"
                                    }
                                    """.trimIndent(),
                                )
                                append(
                                    "file",
                                    "test".toByteArray(),
                                    Headers.build {
                                        append(HttpHeaders.ContentType, "text/plain")
                                        append(HttpHeaders.ContentDisposition, "filename=\"test.txt\"")
                                    },
                                )
                            },
                        ),
                    )
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `Test multipart upload handles invalid message JSON`() =
        testApplication {
            setupTestApplication()

            val response =
                client.post(UPLOAD) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("message", "invalid json")
                                append(
                                    "file",
                                    "test".toByteArray(),
                                    Headers.build {
                                        append(HttpHeaders.ContentType, "text/plain")
                                        append(HttpHeaders.ContentDisposition, "filename=\"test.txt\"")
                                    },
                                )
                            },
                        ),
                    )
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
    fun `Test message response format is correct`() =
        testApplication {
            setupTestApplication()

            val response =
                client.post(UPLOAD) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append(
                                    "message",
                                    """
                                    {
                                        "userID": "testUser",
                                        "time": "2025-01-01T12:00:00",
                                        "prompt": "Hello",
                                        "conversationId": "test-conversation-id"
                                    }
                                    """.trimIndent(),
                                )
                                append(
                                    "file",
                                    "test".toByteArray(),
                                    Headers.build {
                                        append(HttpHeaders.ContentType, "text/plain")
                                        append(HttpHeaders.ContentDisposition, "filename=\"test.txt\"")
                                    },
                                )
                            },
                        ),
                    )
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseJson = Json.decodeFromString<Response>(response.bodyAsText())
            assertNotNull(responseJson)
            assertEquals("Hello, testUser!", responseJson.message)
        }

    @Test
    fun `Test file upload handling`() =
        testApplication {
            setupTestApplication()
            val testContent = "test content"
            val fileName = "test.txt"

            val response =
                client.post(UPLOAD) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append(
                                    "file",
                                    testContent.toByteArray(),
                                    Headers.build {
                                        append(HttpHeaders.ContentType, "text/plain")
                                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                                    },
                                )
                            },
                        ),
                    )
                }

            assertEquals(HttpStatusCode.OK, response.status)

            val testDir = File("test_uploads")
            assertTrue(testDir.exists())

            val uploadedFile =
                testDir.listFiles()?.firstOrNull { it.name.startsWith("test_") && it.name.endsWith(".txt") }
            assertNotNull(uploadedFile, "Uploaded file should exist")

            if (uploadedFile != null) {
                assertEquals(testContent, uploadedFile.readText())
            }

            val fileNameParts = uploadedFile?.name?.split("_")
            assertEquals("test", fileNameParts?.get(0) ?: "Filename should start with original filename")
            assertTrue(
                fileNameParts?.get(1)?.substringBefore(".txt")?.toLongOrNull() != null,
                "Filename should contain timestamp",
            )
        }

    @Test
    fun `Test Response class is serializable`() {
        val response = Response("time", "message")
        val json = Json.encodeToString(Response.serializer(), response)
        val deserialized = Json.decodeFromString(Response.serializer(), json)
        assertEquals(response, deserialized)
    }

    @Test
    fun `Test generateTimestampedFileName with extension adds timestamp before extension`() {
        val originalFileName = "test.jpg"
        val result = generateTimestampedFileName(originalFileName)

        assertTrue(result.matches(Regex("test_\\d+\\.jpg")))

        val parts = result.substring(0, result.lastIndexOf('.')).split("_")
        assertEquals(2, parts.size)
        assertEquals("test", parts[0])
        assertTrue(parts[1].toLongOrNull() != null)
    }

    @Test
    fun `Test upload without file does not cause errors`() =
        testApplication {
            setupTestApplication()

            val response =
                client.post(UPLOAD) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("description", "No File Attached")
                            },
                        ),
                    )
                }

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(response.bodyAsText().contains("No File Attached"))
        }

    @Test
    fun `Test upload with form items but no file item`() =
        testApplication {
            setupTestApplication()

            val response =
                client.post(UPLOAD) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("description", "Description Only")
                                append(
                                    "message",
                                    """
                                    {
                                        "userID": "testUser",
                                        "time": "2025-01-01T12:00:00",
                                        "prompt": "Hello",
                                        "conversationId": "test-conversation-id"
                                    }
                                    """.trimIndent(),
                                )
                            },
                        ),
                    )
                }

            assertEquals(HttpStatusCode.OK, response.status)
            val responseJson = Json.decodeFromString<Response>(response.bodyAsText())
            assertEquals("Hello, testUser!", responseJson.message)
        }

    @Test
    fun `Test createUploadDirectory reuses existing directory`() =
        testApplication {
            setupTestApplication()
            val testDir = File("test_uploads")
            testDir.mkdirs()

            val markerFile = File(testDir, "marker.txt")
            markerFile.writeText("marker")

            client.post(UPLOAD) {
                header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                "file",
                                "test".toByteArray(),
                                Headers.build {
                                    append(HttpHeaders.ContentType, "text/plain")
                                    append(HttpHeaders.ContentDisposition, "filename=\"test.txt\"")
                                },
                            )
                        },
                    ),
                )
            }

            assertTrue(markerFile.exists(), "Directory should be reused, not recreated")
        }

    @Test
    fun `Test multipart upload without any parts`() =
        testApplication {
            setupTestApplication()

            val response =
                client.post(UPLOAD) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    setBody(
                        MultiPartFormDataContent(
                            formData {},
                        ),
                    )
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `Test upload with multiple files`() =
        testApplication {
            setupTestApplication()

            val response =
                client.post(UPLOAD) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append(
                                    "file",
                                    "first file content".toByteArray(),
                                    Headers.build {
                                        append(HttpHeaders.ContentType, "text/plain")
                                        append(HttpHeaders.ContentDisposition, "filename=\"first.txt\"")
                                    },
                                )
                                append(
                                    "file",
                                    "second file content".toByteArray(),
                                    Headers.build {
                                        append(HttpHeaders.ContentType, "text/plain")
                                        append(HttpHeaders.ContentDisposition, "filename=\"second.txt\"")
                                    },
                                )
                            },
                        ),
                    )
                }

            assertEquals(HttpStatusCode.OK, response.status)

            val testDir = File("test_uploads")
            val files = testDir.listFiles() ?: emptyArray()

            assertTrue(files.any { it.name.startsWith("first_") })
            assertTrue(files.any { it.name.startsWith("second_") })
        }

    @Test
    fun `Test upload with blank filename`() =
        testApplication {
            setupTestApplication()

            val response =
                client.post(UPLOAD) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append(
                                    "file",
                                    "content".toByteArray(),
                                    Headers.build {
                                        append(HttpHeaders.ContentType, "text/plain")
                                        append(HttpHeaders.ContentDisposition, "filename=\"\"")
                                    },
                                )
                            },
                        ),
                    )
                }

            assertEquals(HttpStatusCode.OK, response.status)

            val testDir = File("test_uploads")
            val uploadedFile = testDir.listFiles()?.firstOrNull { it.name.startsWith("unknown_") }
            assertNotNull(uploadedFile, "File with generated name should exist")
        }

    @Test
    fun `Test binary file upload`() =
        testApplication {
            setupTestApplication()

            val binaryContent = ByteArray(256) { it.toByte() }

            val response =
                client.post(UPLOAD) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append(
                                    "file",
                                    binaryContent,
                                    Headers.build {
                                        append(HttpHeaders.ContentType, "application/octet-stream")
                                        append(HttpHeaders.ContentDisposition, "filename=\"binary.dat\"")
                                    },
                                )
                            },
                        ),
                    )
                }

            assertEquals(HttpStatusCode.OK, response.status)

            val testDir = File("test_uploads")
            val uploadedFile = testDir.listFiles()?.firstOrNull { it.name.startsWith("binary_") }
            assertNotNull(uploadedFile, "Binary file should be uploaded")

            if (uploadedFile != null) {
                val uploadedContent = uploadedFile.readBytes()
                assertTrue(binaryContent.contentEquals(uploadedContent), "Binary content should match")
            }
        }

    @Test
    fun `Test form item with unknown name is ignored`() =
        testApplication {
            setupTestApplication()

            val response =
                client.post(UPLOAD) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("unknownField", "This should be ignored")
                                append(
                                    "file",
                                    "test content".toByteArray(),
                                    Headers.build {
                                        append(HttpHeaders.ContentType, "text/plain")
                                        append(HttpHeaders.ContentDisposition, "filename=\"test.txt\"")
                                    },
                                )
                            },
                        ),
                    )
                }

            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `Test handleMessagePart with null message skips let block`() =
        testApplication {
            setupTestApplication()

            val testDescription = "Test with null message"

            val response =
                client.post(UPLOAD) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("description", testDescription)
                                append("message", "null")
                                append(
                                    "file",
                                    "test content".toByteArray(),
                                    Headers.build {
                                        append(HttpHeaders.ContentType, "text/plain")
                                        append(HttpHeaders.ContentDisposition, "filename=\"test.txt\"")
                                    },
                                )
                            },
                        ),
                    )
                }

            assertEquals(HttpStatusCode.OK, response.status)

            val responseText = response.bodyAsText()
            assertTrue(responseText.contains(testDescription))
            assertTrue(responseText.contains("is uploaded to"))

            runCatching {
                Json.decodeFromString<Response>(responseText)
            }.onSuccess {
                assertTrue(false, "Response should not be in JSON format when message is null")
            }
        }

    @Test
    fun `Test upload with missing filename parameter`() =
        testApplication {
            setupTestApplication()

            val testDescription = "Test with missing filename parameter"

            val response =
                client.post(UPLOAD) {
                    header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("description", testDescription)
                                append(
                                    "file",
                                    "test content".toByteArray(),
                                    Headers.build {
                                        append(HttpHeaders.ContentType, "text/plain")
                                        append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"\"")
                                    },
                                )
                            },
                        ),
                    )
                }

            assertEquals(HttpStatusCode.OK, response.status)

            val testDir = File("test_uploads")
            val uploadedFile = testDir.listFiles()?.firstOrNull { it.name.startsWith("unknown_") }
            assertNotNull(uploadedFile, "File with generated name should exist")

            if (uploadedFile != null) {
                assertEquals("test content", uploadedFile.readText())
            }

            val responseText = response.bodyAsText()
            assertTrue(responseText.contains(testDescription))
            assertTrue(responseText.contains("is uploaded to"))
        }
}
