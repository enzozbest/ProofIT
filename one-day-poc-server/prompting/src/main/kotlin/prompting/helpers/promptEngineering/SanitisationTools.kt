package prompting.helpers.promptEngineering

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

/**
 * A utility object providing tools for sanitizing user prompts to ensure safety and security.
 *
 * This object implements methods for cleaning prompt text, extracting significant keywords,
 * and combining these operations for secure prompt processing.
 */
object SanitisationTools {
    const val MAX_PROMPT_LENGTH = 1000

    /**
     * User prompts via the JSON prompt request are sanitised by removing all HTML tags (Jsoup), removing leading
     * and trailing whitespace replacing special characters and HTML entities such as &lt with the empty string.
     * The user input is capped the user input to [MAX_PROMPT_LENGTH] characters, ignoring all text after a word/phrase
     * if the prompt contains a malicious phrase.
     */
    private fun cleanPrompt(prompt: String): String {
        var sanitised =
            Jsoup
                .clean(prompt, Safelist.none())
                .trim()
                .replace(Regex("&[a-zA-Z0-9#]+;"), "")
                .replace(Regex("[^\\w\\s.,!?()]"), "")
                .take(MAX_PROMPT_LENGTH)
        val maliciousPatterns =
            listOf(
                "ignore",
                "pretend",
                "disregard",
                "act like",
                "follow my instructions",
                "do not follow",
                "override",
                "act as",
                "respond as",
            )
        for (pattern in maliciousPatterns) {
            val escapedPattern = pattern.replace(" ", "\\s+")
            val regex = Regex("(?i)$escapedPattern(?:\\s+\\w+)?")
            sanitised = sanitised.replace(regex, "")
        }
        return sanitised
    }

    /**
     * This method implementation("org.jsoup:jsoup:1.15.3")extracts keywords from the user prompt submitted
     * These words will be added to a list and eventually passed to the llm
     * with the sanitised prompt
     */
    private fun extractKeywords(prompt: String): List<String> {
        val keywordSet = KeywordLoader.getKeywordsList().toSet()
        val lowercasePrompt = prompt.lowercase()
        return keywordSet.filter { it in lowercasePrompt }
    }

    /**
     * Main entry point for prompt sanitization, combining cleaning and keyword extraction.
     *
     * This method processes a raw user prompt by:
     * 1. Cleaning the prompt text to remove potentially harmful content
     * 2. Extracting relevant keywords for further processing
     * 3. Returning both in a structured result object
     *
     * @param prompt The raw user input to be sanitized
     * @return A SanitisedPromptResult containing the cleaned prompt and extracted keywords
     */
    internal fun sanitisePrompt(prompt: String): SanitisedPromptResult {
        val sanitisedPrompt = cleanPrompt(prompt)
        val keywords = extractKeywords(sanitisedPrompt)
        println("Keywords are: $keywords")
        return SanitisedPromptResult(sanitisedPrompt, keywords)
    }
}
