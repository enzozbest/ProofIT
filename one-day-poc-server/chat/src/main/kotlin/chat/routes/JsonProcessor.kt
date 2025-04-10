package chat.routes

/**
 * Utility class for processing JSON responses from the prompting pipeline.
 *
 * This class provides methods to extract specific parts from JSON strings
 * without requiring full deserialization.
 */
object JsonProcessor {
    /**
     * Extract parts from the raw JSON response using string operations to avoid full deserialization.
     *
     * @param jsonString The raw JSON response from the prompting pipeline
     * @return Pair of (chatContent, prototypeFilesJson) - any might be empty if not found
     */
    fun processRawJsonResponse(jsonString: String): Pair<String, String> {
        val chatContent = extractChatContent(jsonString)
        val prototypeFilesJson = extractPrototypeContent(jsonString)

        return Pair(chatContent, prototypeFilesJson)
    }

    /**
     * Extracts the chat content from a JSON string.
     *
     * @param jsonString The JSON string to extract from
     * @return The extracted chat content or an empty string if not found
     */
    private fun extractChatContent(jsonString: String): String {
        val chatRegex = """"chat"\s*:\s*"([^"]*)"|\{\s*"message"\s*:\s*"([^"]*)"""".toRegex()
        val chatMatch = chatRegex.find(jsonString)
        return chatMatch?.groupValues?.firstOrNull { it.isNotEmpty() && it != chatMatch.value } ?: ""
    }

    /**
     * Extracts the prototype content from a JSON string.
     *
     * @param jsonString The JSON string to extract from
     * @return The extracted prototype content or null if not found
     */
    private fun extractPrototypeContent(jsonString: String): String {
        // Default value if no prototype content is found
        var prototypeContent: String? = null

        // Find the "files" section within prototype
        val filesMarker = "\"files\":"
        val filesStartIndex = jsonString.indexOf(filesMarker)

        if (filesStartIndex >= 0) {
            var pos = filesStartIndex + filesMarker.length

            while (pos < jsonString.length && jsonString[pos].isWhitespace()) {
                pos++
            }
            var depth = 0
            var foundStart = false
            val filesJson = StringBuilder()

            while (pos < jsonString.length && !foundStart) {
                if (jsonString[pos] == '{') {
                    foundStart = true
                    depth = 1
                    filesJson.append('{')
                }
                pos++
            }
            if (foundStart) {
                while (pos < jsonString.length && depth > 0) {
                    val char = jsonString[pos]
                    filesJson.append(char)

                    if (char == '{') {
                        depth++
                    } else if (char == '}') {
                        depth--
                    }
                    pos++
                }
                if (depth == 0) {
                    prototypeContent = filesJson.toString()
                }
            }
        }
        return prototypeContent ?: "{}"
    }
}
