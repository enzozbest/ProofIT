package kcl.seg.rtt.chat_history

import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.AfterEach
import java.io.File
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UploadRoutesTest : BaseAuthenticationServer() {

    private val testFileContent = "Test content"
    private val testFileName = "test.txt"

    @Test
    fun `Test Valid File Upload`() = testApplication {
        setupTestApplication()

        File(testFileName).writeText(testFileContent)

        val boundary = "WebAppBoundary"
        val response = client.post("/upload") {
            header(HttpHeaders.Authorization, "Bearer ${createValidToken()}")
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

    @AfterEach
    fun cleanup() {
        File(testFileName).delete()
    }
}