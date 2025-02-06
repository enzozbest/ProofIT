package kcl.seg.rtt.chat_history

import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.io.File
import kotlin.test.*

class UploadRoutesTest {

    private val testFileContent = "Test content"
    private val testFileName = "test.txt"

    @Test
    fun testFileUpload() = testApplication {
        application {
            chatModule()
        }

        File(testFileName).writeText(testFileContent)

        val boundary = "WebAppBoundary"
        val response = client.post("/upload") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("description", "Test file")
                        append("file", File(testFileName).readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, "text/plain")
                            append(HttpHeaders.ContentDisposition, "filename=\"$testFileName\"")
                        })
                    },
                    boundary,
                    ContentType.MultiPart.FormData.withParameter("boundary", boundary)
                )
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Test file is uploaded"))
    }

    @AfterTest
    fun cleanup() {
        File(testFileName).delete()
    }
}