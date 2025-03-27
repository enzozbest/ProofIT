package utils.json

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.lang.IllegalArgumentException

/**
 * Object to encapsulate JSON utilities
 */
object PoCJSON {
    /**
     * Reads a JSON file and returns a JsonObject
     * @param name The path to the JSON file
     * @return The JSONObject
     */

    fun readJsonFile(name: String): JsonObject {
        val file = javaClass.classLoader.getResourceAsStream(name)
        val content: String = file?.readBytes()?.toString(Charsets.UTF_8) ?: ""
        return Json.parseToJsonElement(content).jsonObject
    }

    /**
     * Finds a specific attribute in a Cognito user attribute array
     * @param array The array of Cognito user attributes
     * @param attribute The attribute to find
     * @return The value of the attribute as a String, or null if the attribute is not found
     */
    fun findCognitoUserAttribute(
        array: JsonArray,
        attribute: String,
    ): String? =
        try {
            array
                .find { it.jsonObject["Name"]?.toString() == "\"${attribute.lowercase()}\"" }
                ?.let {
                    kotlin.runCatching { it.jsonObject["Value"]!!.jsonPrimitive.content }.getOrNull()
                }
        } catch (_: IllegalArgumentException) {
            null
        }
}
