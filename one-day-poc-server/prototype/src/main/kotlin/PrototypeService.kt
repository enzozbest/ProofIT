package kcl.seg.rtt.prototype

import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import kcl.seg.rtt.prototype.PrototypesTable

class PrototypeService {
    fun generatePrototype(prompt: String, context: List<String>?): String {
        val fullPrompt = buildFullPrompt(prompt, context)
        val llmResponse = callLLM(fullPrompt)
        return llmResponse
    }

    private fun buildFullPrompt(prompt: String, context: List<String>?): String {
        return if (context != null) {
            context.joinToString("\n") + "\n" + prompt
        } else {
            prompt
        }
    }

    private fun validatePrompt(prompt: String) {
        if (prompt.isBlank()) {
            throw IllegalArgumentException("Prompt must not be empty.")
        }
        // Add more validation rules here...
    }

    /**
    Ensure all data is valid and appropriate
    before making LLM call, don't want invalid
    calls wasting resources
    */
    private fun safelyCallLLM(prompt: String): String {
        return try {
            callLLM(prompt)
        } catch (e: Exception) {
            // Log or handle the error as necessary
            throw RuntimeException("Error calling LLM: ${e.message}", e)
        }
    }


    private fun callLLM(prompt: String): String {
        // Interact with LLM
        return "Generated prototype based on prompt: $prompt"
    }

    /**
     * Location of this function can be changed later on
     * To somewhere more prototype handling focused
     * Just need a place for it temporarily
     */
    private fun storePrototype(prototypeOutput: String, context: List<String>?) {
        // Stub for storing the generated prototype
        transaction {
            PrototypesTable.insert {row ->
                row[prototype] = prototypeOutput
            }
        }

        // This is a placeholder for future functionality.
    }

    // Placeholder for future functionality
    // Will be needed to pass to web container
    fun formatToJSON(prototypeOutput: String): String {
        return prototypeOutput
    }

}