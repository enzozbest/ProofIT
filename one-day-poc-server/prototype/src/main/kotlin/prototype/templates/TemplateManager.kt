@file:Suppress("ktlint:standard:no-empty-file")

package prototype.templates // package kcl.seg.rtt.prototype
//
//
// /**
// * Result object indicating whether the new template was validated successfully or not.
// * If [success] == false, [errorMessage] should contain a reason.
// */
// data class TemplateValidationResult(
//    val success: Boolean,
//    val errorMessage: String? = null
// )
//
//
// /**
// * Handles new template detection and validation logic.
// */
// object TemplateManager {
//
//    /**
//     * Checks if the [llmRawText] includes a "new template" declaration, e.g., a "@metadata new_template" tag.
//     *
//     * @param llmRawText The raw text/string from the LLM.
//     * @return true if we believe it's defining a new template; false otherwise.
//     */
//    fun detectNewTemplate(llmRawText: String): Boolean {
//        // For now, a simple check:
//        // In practice, can parse JSON or look for more advanced markers
//        return llmRawText.contains("@metadata new_template", ignoreCase = true)
//    }
//
//    /**
//     * Validates the newly generated template to ensure it "compiles" or is well-formed.
//     * E.g., we might check placeholders are correct, code can run, etc.
//     *
//     * @param llmRawText The raw text containing the new template.
//     * @return A [TemplateValidationResult] indicating success or failure.
//     */
//    fun validateNewTemplate(llmRawText: String): TemplateValidationResult {
//        // TODO: parse the new template code or prompt.
//
//        val compileOrRunOkay = runCompilerCheck(llmRawText, "python")
//        // Need to do this for every language involved in the response
//        // Therefore need a way to first parse through the ultimate string
//        if (!compileOrRunOkay) {
//            return TemplateValidationResult(
//                success = false,
//                errorMessage = "Template code failed to compile or run"
//            )
//        }
//
//        // If more checks are needed (like placeholders?), do them here:
//        // check that it has at least one placeholder
//        if (!llmRawText.contains("{topic}")) {
//            return TemplateValidationResult(
//                success = false,
//                errorMessage = "Missing required placeholder {topic}"
//            )
//        }
//
//        return TemplateValidationResult(success = true)
//    }
//
//    /**
//     * If the new template is validated, call the embedding function
//     * to store it in a DB, or S3, etc.
//     * This is a placeholder.
//     *
//     * @param llmRawText The raw text from which we can extract the template content.
//     */
//    fun storeNewTemplateEmbeddings(llmRawText: String) {
//        // TODO: parse the template, extract the relevant text for embedding
//        // val vector = embeddingService.generateVector(llmRawText)
//        // database.saveTemplateVector(vector)
//    }
//
// }
