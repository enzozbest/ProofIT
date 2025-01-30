package kcl.seg.rtt.utils.JSON

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
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
        val file: File = File(path)
        val content: String = file.readText()
        return Json.parseToJsonElement(content).jsonObject
    }
}



