package prototype.services

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import prototype.helpers.OpenAIOptions
import prototype.helpers.OpenAIResponse
import prototype.helpers.parseOpenAIResponse

object OpenAIService {
    suspend fun callOpenAI(
        request: HttpRequestBuilder,
        options: OpenAIOptions,
    ): OpenAIResponse? {
        val client = HttpClient(CIO)
        val response = client.request(request)
        return parseOpenAIResponse(response.bodyAsText())
    }
}
