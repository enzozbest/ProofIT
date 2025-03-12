@file:Suppress("ktlint:standard:no-empty-file")

package prototype.templates
// /**
// * Represents a prompt template.
// *
// * @property id A unique identifier for the template (e.g. "assistantV1").
// * @property matchingKeywords A list of keywords we expect in the user's prompt
// *           that suggests this template is relevant. (Dummy logic for now.)
// * @property content A refined prompt that includes placeholders, e.g.:
// *          "You are an AI assistant. The user wants help with {topic}.
// *           Make sure to greet them as {username}."
// */
// data class Template(
//    val id: String,
//    val matchingKeywords: List<String>,
//    val content: String
// )
//
// object TemplateRepository {
//    // For now, a static list of example templates.
//    // In reality, you might load them from DB, S3, or a real config file.
//    private val templates = listOf(
//        Template(
//            id = "assistantV1",
//            matchingKeywords = listOf("assistant", "help"),
//            content = """
//                You are an AI Assistant.
//                The user wants help with {topic}.
//                Greet them as {username}.
//                Provide your answer succinctly.
//            """.trimIndent()
//        ),
//        Template(
//            id = "chatbotV2",
//            matchingKeywords = listOf("chat", "talk", "communicate"),
//            content = """
//                Act as a friendly chatbot.
//                The user is concerned about {topic}.
//                They are named {username}.
//                Offer a short but empathetic response.
//            """.trimIndent()
//        )
//        // Add more templates as needed
//    )
//
//    /**
//     * Finds the first template that contains at least one matching keyword from [userPrompt].
//     * In a real system, you'd have more sophisticated matching logic.
//     */
//    fun findRelevantTemplate(userPrompt: String): Template? {
//        val promptLower = userPrompt.lowercase()
//        return templates.firstOrNull { template ->
//            template.matchingKeywords.any { keyword ->
//                promptLower.contains(keyword.lowercase())
//            }
//        }
//    }
// }
//
// /**
// * Replaces placeholders in [templateContent] with values from [placeholders].
// *
// * Placeholders are expected to look like {key}, e.g. {topic}, {username}.
// *
// * method in future will be replaced by a more sophisticated system that
// * computes embeddings for the user prompt, compares them against embeddings
// * for each template, and picks the best semantic match.
// */
// fun fillTemplateContent(templateContent: String, placeholders: Map<String, String>): String {
//    var result = templateContent
//    placeholders.forEach { (key, value) ->
//        // naive approach: replace all occurrences of {key} with the actual [value]
//        result = result.replace("{$key}", value, ignoreCase = false)
//    }
//    return result
// }
//
// /**
// * Given the raw [userPrompt], find a relevant template
// * and fill it with the placeholders from [placeholderMap].
// *
// * If no template is found, returns null or a fallback string.
// */
// fun buildRefinedPrompt(userPrompt: String, placeholderMap: Map<String, String>): String? {
//    // 1) Find a matching template
//    val template = TemplateRepository.findRelevantTemplate(userPrompt)
//        ?: return null // or "No matching template"
//
//    // 2) Fill placeholders
//    val refined = fillTemplateContent(template.content, placeholderMap)
//
//    // 3) Return the final text that can be fed to the LLM
//    return refined
// }
//
//
//
//
