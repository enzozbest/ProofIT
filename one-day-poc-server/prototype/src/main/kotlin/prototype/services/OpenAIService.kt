package prototype.services

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import prototype.helpers.OpenAIOptions
import prototype.helpers.OpenAIResponse

object OpenAIService {
    suspend fun callOpenAI(
        request: HttpRequestBuilder,
        options: OpenAIOptions,
    ): OpenAIResponse? {
        val client = HttpClient(CIO)

        val response = client.request(request)
        val responseBody = response.bodyAsText()
        val json = runCatching { Json.decodeFromString(OpenAIResponse.serializer(), responseBody) }

        return json.getOrNull()
    }
}
