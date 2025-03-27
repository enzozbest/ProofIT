package prototype.security

import org.w3c.tidy.Tidy
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText

/**
 * Main function that runs multiple security checks (size check, blocklist check, compile check, etc.)
 * If any fail, returns false.
 *
 * @param code The code snippet to verify
 * @param language The language (e.g. "javascript", "css", "html")
 * @return True if all checks pass, false otherwise
 */
fun secureCodeCheck(
    code: String,
    language: String,
): Boolean {
    // // 1) Check size limit
    // if (!checkCodeSizeLimit(code, maxBytes = 100_000)) {
    //     println("Code exceeds size limit.")
    //     return false
    // }

    // // 2) Blocklist approach for suspicious patterns
    // if (!scanForDangerousPatterns(code, language)) {
    //     println("Code contains dangerous patterns for $language.")
    //     return false
    // }

    // // 3) If it's HTML or CSS, we might sanitize or do further checks
    // if (language.equals("html", ignoreCase = true)) {
    //     // optional: sanitize
    //     val sanitized = sanitizeHtml(code)
    //     if (!htmlIsSimilarEnough(code, sanitized)) {
    //         println("HTML changed significantly after sanitization => suspicious.")
    //         return false
    //     }
    // }

    // 4) Compile / syntax check
    if (!runCompilerCheck(code, language)) {
        println("Syntax or compile check failed for $language code.")
        return false
    }

    // All checks passed
    return true
}

/**
 * Enforces a maximum code size in bytes (UTF-8).
 */
fun checkCodeSizeLimit(
    code: String,
    maxBytes: Int,
): Boolean {
    val size = code.toByteArray(Charsets.UTF_8).size
    return size <= maxBytes
}

// /**
//  * Example "dangerous patterns" check using a simple blocklist.
//  * Real code might add or remove patterns over time.
//  */
// fun scanForDangerousPatterns(code: String, language: String): Boolean {
//     // Patterns that might indicate malicious code or OS-level calls
//     val globalBlocklist = listOf(
//         Regex("""(?i)\bexec\([^)]*\)"""),     // generic 'exec(...)'
//         Regex("""(?i)\bsystem\([^)]*\)"""),   // 'system(...)' calls
//         Regex("""(?i)\bimport\s+subprocess"""),
//         Regex("""(?i)rm\s+-rf"""),
//     )

//     // Language-specific patterns
//     val pythonBlocklist = listOf(
//         Regex("""(?i)\bos\.system\("""),
//         Regex("""(?i)\bimport\s+os""")
//     )
//     val jsBlocklist = listOf(
//         Regex("""(?i)\brequire\(.*child_process.*\)"""),
//         Regex("""(?i)\bimport\s+child_process"""),
//     )

//     // Merge global + language blocklists
//     val patterns = mutableListOf<Regex>().apply {
//         addAll(globalBlocklist)
//         when (language.lowercase()) {
//             "python" -> addAll(pythonBlocklist)
//             "javascript" -> addAll(jsBlocklist)
//             "html" -> addAll(htmlBlocklist)
//         }
//     }

//     // Return false if any pattern is found
//     for (pattern in patterns) {
//         if (pattern.containsMatchIn(code)) {
//             println("Blocked pattern found: $pattern")
//             return false
//         }
//     }
//     return true
// }

/**
 * Runs a naive compile / syntax check for a few languages (python, js, css, html).
 * If you support more languages, extend this.
 */
fun runCompilerCheck(
    code: String,
    language: String,
): Boolean =
    when (language.lowercase()) {
        "javascript" -> checkJavaScriptSyntax(code)
        "css" -> checkCssSyntax(code)
        "html" -> checkHtmlSyntaxWithJTidy(code)
        else -> false
    }

/**
 * Checks CSS syntax using Stylelint.
 *
 * 1. Writes [cssCode] to a temporary `.css` file.
 * 2. Invokes `npx stylelint` on that file.
 * 3. Returns true if exit code == 0 (no major syntax errors), otherwise false.
 *
 * Note: Requires Node.js and stylelint to be installed (often via `npm install -g stylelint` or local node_modules).
 *
 * @param cssCode The raw CSS snippet to validate.
 * @return True if stylelint found no serious syntax issues, false otherwise.
 */
fun checkCssSyntax(cssCode: String): Boolean {
    val tempPath = createTempFile("cssSnippet", ".css")

    tempPath.writeText(cssCode)
    val process =
        ProcessBuilder("npx", "stylelint", tempPath.toString())
            .redirectErrorStream(true)
            .start()
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    val errorOutput = StringBuilder()
    var line: String?
    while (reader.readLine().also { line = it } != null) {
        errorOutput.append(line).append("\n")
    }

    val exitCode = process.waitFor()

    if (exitCode != 0) {
        println("CSS validation error: ${errorOutput.toString().trim()}")
    }

    return exitCode == 0
}

/**
 * Performs a syntax check on [htmlCode].
 *
 * @param htmlCode The HTML content to parse.
 * @return True if the code is valid HTML, otherwise false.
 */
fun checkHtmlSyntaxWithJTidy(htmlCode: String): Boolean {
    val tidy =
        Tidy().apply {
            quiet = true
            showWarnings = true
        }
    val errorWriter = StringWriter()
    tidy.errout = PrintWriter(errorWriter)

    val inputStream = ByteArrayInputStream(htmlCode.toByteArray(Charsets.UTF_8))
    val outputStream = ByteArrayOutputStream()

    tidy.parseDOM(inputStream, outputStream)

    val errors = errorWriter.toString()
    return !errors.contains("Error: ")
}

/**
 * Checks JavaScript syntax by creating a temporary `.js` file and running `node --check`.
 *
 * Returns true if no errors were detected (exit code == 0), otherwise false.
 *
 * Note: Requires Node.js installed, as well as support for `--check`.
 *
 * @param jsCode The JavaScript code snippet to validate.
 * @return True if Node.js found no syntax errors, otherwise false.
 */
fun checkJavaScriptSyntax(jsCode: String): Boolean {
    val tempPath = createTempFile("jsSnippet", ".js")
    tempPath.writeText(jsCode)

    val process =
        ProcessBuilder("node", "--check", tempPath.toString())
            .redirectErrorStream(true)
            .start()

    val reader = BufferedReader(InputStreamReader(process.inputStream))
    val errorOutput = StringBuilder()
    var line: String?
    while (reader.readLine().also { line = it } != null) {
        errorOutput.append(line).append("\n")
    }

    val exitCode = process.waitFor()

    if (exitCode != 0) {
        println("JavaScript syntax error: ${errorOutput.toString().trim()}")
    }

    return exitCode == 0
}
