package kcl.seg.rtt.prototype

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.jsoup.safety.Safelist
import java.nio.file.Path
import kotlin.io.path.createTempFile
import kotlin.io.path.writeText

/**
 * Main function that runs multiple security checks (size check, blocklist check, compile check, etc.)
 * If any fail, returns false.
 *
 * @param code The code snippet to verify
 * @param language The language (e.g. "python", "javascript", "css", "html")
 * @return True if all checks pass, false otherwise
 */
fun secureCodeCheck(code: String, language: String): Boolean {
    // 1) Check size limit
    if (!checkCodeSizeLimit(code, maxBytes = 100_000)) {
        println("Code exceeds size limit.")
        return false
    }

    // 2) Blocklist approach for suspicious patterns
    if (!scanForDangerousPatterns(code, language)) {
        println("Code contains dangerous patterns for $language.")
        return false
    }

    // 3) If it's HTML or CSS, we might sanitize or do further checks
    if (language.equals("html", ignoreCase = true)) {
        // optional: sanitize
        val sanitized = sanitizeHtml(code)
        if (!htmlIsSimilarEnough(code, sanitized)) {
            println("HTML changed significantly after sanitization => suspicious.")
            return false
        }
    }

    // 4) Compile / syntax check
    if (!runCompilerCheck(code, language)) {
        println("Syntax or compile check failed for $language code.")
        return false
    }

    // If you intend to run the code, consider a sandbox here

    // All checks passed
    return true
}

/**
 * Enforces a maximum code size in bytes (UTF-8).
 */
fun checkCodeSizeLimit(code: String, maxBytes: Int): Boolean {
    val size = code.toByteArray(Charsets.UTF_8).size
    return size <= maxBytes
}

/**
 * Example "dangerous patterns" check using a simple blocklist.
 * Real code might add or remove patterns over time.
 */
fun scanForDangerousPatterns(code: String, language: String): Boolean {
    // Patterns that might indicate malicious code or OS-level calls
    val globalBlocklist = listOf(
        Regex("""(?i)\bexec\([^)]*\)"""),     // generic 'exec(...)'
        Regex("""(?i)\bsystem\([^)]*\)"""),   // 'system(...)' calls
        Regex("""(?i)\bimport\s+subprocess"""),
        Regex("""(?i)rm\s+-rf"""),
    )

    // Language-specific patterns
    val pythonBlocklist = listOf(
        Regex("""(?i)\bos\.system\("""),
        Regex("""(?i)\bimport\s+os""")
    )
    val jsBlocklist = listOf(
        Regex("""(?i)\brequire\(.*child_process.*\)"""),
        Regex("""(?i)\bimport\s+child_process"""),
    )
    val htmlBlocklist = listOf(
        Regex("""(?i)<script>"""), // maybe disallow inline <script> tags
    )

    // Merge global + language blocklists
    val patterns = mutableListOf<Regex>().apply {
        addAll(globalBlocklist)
        when (language.lowercase()) {
            "python" -> addAll(pythonBlocklist)
            "javascript" -> addAll(jsBlocklist)
            "html" -> addAll(htmlBlocklist)
        }
    }

    // Return false if any pattern is found
    for (pattern in patterns) {
        if (pattern.containsMatchIn(code)) {
            println("Blocked pattern found: $pattern")
            return false
        }
    }
    return true
}

/**
 * Optionally sanitize the HTML to remove suspicious tags/attributes.
 */
fun sanitizeHtml(originalHtml: String): String {
    // Using Jsoup's "clean" with a relatively permissive safelist
    // You can pick from Whitelist.none(), Whitelist.basic(), etc.
    return Jsoup.clean(originalHtml, Safelist.relaxed())
}

/**
 * Compare sanitized HTML vs original for big differences.
 * If you consider big differences suspicious, you can fail.
 */
fun htmlIsSimilarEnough(original: String, sanitized: String): Boolean {
    // Arbitrary approach: if length differs by more than 50%, we suspect something
    val origLen = original.length
    val sanLen = sanitized.length
    return sanLen >= (origLen * 0.5)
}

/**
 * Runs a naive compile / syntax check for a few languages (python, js, css, html).
 * If you support more languages, extend this.
 */
fun runCompilerCheck(code: String, language: String): Boolean {
    return when (language.lowercase()) {
        "python" -> checkPythonSyntax(code)
        "javascript" -> checkJavaScriptSyntax(code)
        "css" -> checkCssSyntax(code)
        "html" -> checkHtmlSyntaxWithJsoup(code) // Or Tidy-based approach
        else -> false
    }
}

/**
 * Attempts to verify the Python syntax of [pythonCode] by writing it to a
 * temporary `.py` file and running `python -m py_compile`.
 *
 * If the compilation is successful (exit code = 0), returns true; otherwise false.
 *
 * @param pythonCode The raw Python code snippet to be checked.
 * @return True if the code passed Python's syntax check, false otherwise.
 */
private fun checkPythonSyntax(pythonCode: String): Boolean {
    val tempPath: Path = createTempFile(prefix = "pythonSnippet", suffix = ".py")

    tempPath.writeText(pythonCode)

    val process = ProcessBuilder("python", "-m", "py_compile", tempPath.toString())
        .redirectErrorStream(true)
        .start()

    val exitCode = process.waitFor()
    return exitCode == 0
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
private fun checkCssSyntax(cssCode: String): Boolean {
    val tempPath = createTempFile("cssSnippet", ".css")

    tempPath.writeText(cssCode)
    val process = ProcessBuilder("npx", "stylelint", tempPath.toString())
        .redirectErrorStream(true)
        .start()

    val exitCode = process.waitFor()
    return exitCode == 0
}

/**
 * Performs a minimal syntax check on [htmlCode] by attempting to parse it with Jsoup.
 *
 * Jsoup is fairly tolerant, so minor errors won't necessarily fail here.
 * For stricter checks or advanced validation (e.g., W3C compliance),
 * consider using Tidy or a dedicated validator.
 *
 * @param htmlCode The HTML content to parse.
 * @return True if Jsoup could parse it without throwing an exception, false otherwise.
 */
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
private fun checkJavaScriptSyntax(jsCode: String): Boolean {
    val tempPath = createTempFile("jsSnippet", ".js")
    tempPath.writeText(jsCode)

    val process = ProcessBuilder("node", "--check", tempPath.toString())
        .redirectErrorStream(true)
        .start()

    val exitCode = process.waitFor()
    return exitCode == 0
}
