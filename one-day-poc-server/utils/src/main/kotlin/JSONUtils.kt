package kcl.seg.rtt.utils.JSON

import kotlinx.serialization.json.*
import java.io.File

/**
 * Object to encapsulate JSON utilities
 */
object PoCJSON {

    /**
     * Reads a JSON file and returns a JsonObject
     * @param path The path to the JSON file
     * @return The JSONObject
     */

    fun readJsonFile(path: String): JsonObject {
        val file = File(path)
        val content: String = file.readText()
        return Json.parseToJsonElement(content).jsonObject
    }

    /**
     * Finds a specific attribute in a Cognito user attribute array
     * @param array The array of Cognito user attributes
     * @param attribute The attribute to find
     * @return The value of the attribute as a String, or null if the attribute is not found
     */
    fun findCognitoUserAttribute(array: JsonArray, attribute: String): String? {
        return array.find { it.jsonObject["Name"]?.toString() == "\"${attribute.lowercase()}\"" }?.jsonObject?.get("Value")
            ?.jsonPrimitive?.content
    }
}



