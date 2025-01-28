package kcl.seg.rtt.utils.JSON

import java.io.File
import org.json.JSONObject

/**
 * Reads a JSON file and returns a JSONObject
 * @param path The path to the JSON file
 * @return The JSONObject
 */
fun readJsonFile(path: String): JSONObject {
    val file : File = File(path)
    val content : String = file.readText()
    return JSONObject(content)
}