package kcl.seg.rtt.prototype

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText
import java.nio.file.Path


/**
 * Result object indicating whether the new template was validated successfully or not.
 * If [success] == false, [errorMessage] should contain a reason.
 */
data class TemplateValidationResult(
    val success: Boolean,
    val errorMessage: String? = null
)

/**
 * Handles new template detection and validation logic.
 */
object TemplateManager {

    /**
     * Checks if the [llmRawText] includes a "new template" declaration, e.g., a "@metadata new_template" tag.
     *
     * @param llmRawText The raw text/string from the LLM.
     * @return true if we believe it's defining a new template; false otherwise.
     */
    fun detectNewTemplate(llmRawText: String): Boolean {
        // For now, a simple check:
        // In practice, can parse JSON or look for more advanced markers
        return llmRawText.contains("@metadata new_template", ignoreCase = true)
    }

    /**
     * Validates the newly generated template to ensure it "compiles" or is well-formed.
     * E.g., we might check placeholders are correct, code can run, etc.
     *
     * @param llmRawText The raw text containing the new template.
     * @return A [TemplateValidationResult] indicating success or failure.
     */
    fun validateNewTemplate(llmRawText: String): TemplateValidationResult {
        // TODO: parse the new template code or prompt.

        val compileOrRunOkay = runCompilerCheck(llmRawText, "python")
        // Need to do this for every language involved in the response
        // Therefore need a way to first parse through the ultimate string
        if (!compileOrRunOkay) {
            return TemplateValidationResult(
                success = false,
                errorMessage = "Template code failed to compile or run"
            )
        }

        // If more checks are needed (like placeholders?), do them here:
        // check that it has at least one placeholder
        if (!llmRawText.contains("{topic}")) {
            return TemplateValidationResult(
                success = false,
                errorMessage = "Missing required placeholder {topic}"
            )
        }

        return TemplateValidationResult(success = true)
    }

    private fun checkPythonSyntax(pythonCode: String): Boolean {
        val tempPath: Path = createTempFile(prefix = "pythonSnippet", suffix = ".py")

        tempPath.writeText(pythonCode)

        val process = ProcessBuilder("python", "-m", "py_compile", tempPath.toString())
            .redirectErrorStream(true)
            .start()

        val exitCode = process.waitFor()
        return exitCode == 0
    }

    private fun checkCssSyntax(cssCode: String): Boolean {
        val tempPath = createTempFile("cssSnippet", ".css")

        tempPath.writeText(cssCode)
        val process = ProcessBuilder("npx", "stylelint", tempPath.toString())
            .redirectErrorStream(true)
            .start()

        val exitCode = process.waitFor()
        return exitCode == 0
    }

    private fun checkHtmlSyntaxWithJsoup(htmlCode: String): Boolean {
        return try {

            // Usually Jsoup tries to "tolerate" bad HTML.
            Jsoup.parse(htmlCode, "", Parser.htmlParser())
            true
        } catch (e: Exception) {
            // if you want to check for certain warnings, handle it here.
            false
        }
    }

    private fun checkJavaScriptSyntax(jsCode: String): Boolean {
        val tempPath = createTempFile("jsSnippet", ".js")
        tempPath.writeText(jsCode)

        val process = ProcessBuilder("node", "--check", tempPath.toString())
            .redirectErrorStream(true)
            .start()

        val exitCode = process.waitFor()
        return exitCode == 0
    }


    /**
     * Dummy function to simulate a "compile" or "run" check on the template code.
     * Returns true if it "works," false if there's an error.
     */
    private fun runCompilerCheck(code: String, language: String): Boolean {
        return when (language.lowercase()) {
            "python" -> checkPythonSyntax(code)
            "javascript" -> checkJavaScriptSyntax(code)
            "css" -> checkCssSyntax(code)
            "html" -> checkHtmlSyntaxWithJsoup(code)
            else -> false // or throw an exception
        }
    }

    /**
     * If the new template is validated, call the embedding function
     * to store it in a DB, or S3, etc.
     * This is a placeholder.
     *
     * @param llmRawText The raw text from which we can extract the template content.
     */
    fun storeNewTemplateEmbeddings(llmRawText: String) {
        // TODO: parse the template, extract the relevant text for embedding
        // val vector = embeddingService.generateVector(llmRawText)
        // database.saveTemplateVector(vector)
    }
}
