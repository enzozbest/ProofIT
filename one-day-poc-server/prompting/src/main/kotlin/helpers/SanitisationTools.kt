package helpers

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

object SanitisationTools {
    /**
     * User prompts via the JSON prompt request are sanitised by
     * removing all HTML tags (Jsoup)
     * removing leading and trailing whitespace
     * replacing special characters and HTML entities such as &lt;
     * with the empty string
     * capping the user input to 1000 characters
     * ignoring all text after a word/phrase if the prompt contains a malicious phrase
     *
     */
    private fun cleanPrompt(prompt: String): String {
        var sanitised =
            Jsoup
                .clean(prompt, Safelist.none())
                .trim()
                .replace(Regex("&[a-zA-Z0-9#]+;"), "")
                .replace(Regex("[^\\w\\s.,!?()]"), "")
                .take(1000)
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
            val regex = Regex("((?i)$pattern)")
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

    internal fun sanitisePrompt(prompt: String): SanitisedPromptResult {
        val sanitisedPrompt = cleanPrompt(prompt)
        val keywords = extractKeywords(sanitisedPrompt)
        println("Keywords are: $keywords")
        return SanitisedPromptResult(sanitisedPrompt, keywords)
    }
}
