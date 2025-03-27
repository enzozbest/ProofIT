package prompting.helpers.templates

import kotlinx.coroutines.runBlocking
import prompting.helpers.PrototypeInteractor
import prompting.helpers.promptEngineering.PromptingTools
import prototype.helpers.OllamaOptions
import prototype.helpers.EnhancedResponse
import prototype.helpers.PromptException
import utils.environment.EnvironmentLoader
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import kotlinx.serialization.json.*



/**
 * Service responsible for extracting potential templates from LLM responses,
 * generating JSON-LD annotations for them, and storing them in the template library.
 *
 * This facilitates the self-expanding template library functionality.
 */
object TemplateRetrieval {
    private val annotationModel = EnvironmentLoader.get("OLLAMA_MODEL")

    // Directory paths for template storage
    private val resourcesBasePath = "embeddings/src/main/resources/components"

    // Make these var or property with getter for better testability
    var templatesDir = "$resourcesBasePath/templates"
    var metadataDir = "$resourcesBasePath/metadata"

    /**
     * Cleans a raw template string to ensure it's properly formatted and usable.
     * Removes any markdown formatting, extra whitespace, and non-code elements.
     *
     * @param rawTemplate The raw template string from the LLM
     * @return Clean, usable code
     */
    fun cleanTemplate(rawTemplate: String): String {
        // Use the existing tool from PromptingTools
        return PromptingTools.cleanLlmResponse(rawTemplate)
    }

    /**
     * Processes templates identified in an LLM response.
     * For each template:
     * 1. Extracts the template code
     * 2. Generates a JSON-LD annotation
     * 3. Stores both in the template library
     *
     * @param response The LLM response containing identified templates
     * @return List of template IDs that were successfully processed
     */
    suspend fun processTemplatesFromResponse(response: EnhancedResponse): List<String> {
        if (response.extractedTemplates.isEmpty()) {
            return emptyList()
        }

        // Ensure directories exist
        createDirectoriesIfNeeded()

        return response.extractedTemplates.mapNotNull { rawTemplateCode ->
            val cleanedTemplateCode = cleanTemplate(rawTemplateCode)
            processTemplate(cleanedTemplateCode)
        }
    }

    /**
     * Creates the necessary directories for template storage if they don't exist
     */
    fun createDirectoriesIfNeeded() {
        Files.createDirectories(Paths.get(templatesDir))
        Files.createDirectories(Paths.get(metadataDir))
    }

    /**
     * Processes a single template and stores it in the library.
     *
     * @param templateCode The raw template code
     * @return Template ID if successful, null otherwise
     */
    suspend fun processTemplate(templateCode: String): String? {
        // Extract component name from template code
        val componentName = extractComponentName(templateCode) ?: return null

        // Generate JSON-LD annotation for the template
        val jsonLD = generateTemplateAnnotation(templateCode, componentName) ?: return null

        // Store both the template code and its annotation
        val success = storeTemplateFiles(componentName, templateCode, jsonLD)

        return if (success) componentName else null
    }

    /**
     * Extracts the component name from the template code
     *
     * @param templateCode The raw template code
     * @return The component name or null if it couldn't be extracted
     */
    fun extractComponentName(templateCode: String): String? {
        val exportPatterns = listOf(
            """export\s+const\s+(\w+)""".toRegex(),
            """export\s+default\s+function\s+(\w+)""".toRegex(),
            """const\s+(\w+)\s*=\s*\(\s*\{""".toRegex(),
            """class\s+(\w+)\s+extends\s+React""".toRegex()
        )

        for (pattern in exportPatterns) {
            val match = pattern.find(templateCode)
            if (match != null) {
                return match.groupValues[1]
            }
        }

        return "Component${UUID.randomUUID().toString().take(8)}"
    }

    /**
     * Stores the template and its JSON-LD annotation files in the appropriate directories
     *
     * @param componentName The name of the component
     * @param templateCode The template code to store
     * @param jsonLD The JSON-LD annotation to store
     * @return True if successful, false otherwise
     */
    suspend fun storeTemplateFiles(componentName: String, templateCode: String, jsonLD: String): Boolean {
        try {
            File(templatesDir).mkdirs()
            File(metadataDir).mkdirs()

            val templateFile = File(templatesDir, "$componentName.templ")
            val jsonLdFile = File(metadataDir, "$componentName.jsonld")

            templateFile.writeText(templateCode)
            jsonLdFile.writeText(jsonLD)

            return TemplateInteractor.storeNewTemplate(
                componentName, templateCode, jsonLD
            )
        } catch (e: Exception) {
            println("Failed to store template files: ${e.message}")
            return false
        }
    }

    /**
     * Generates a JSON-LD annotation for a template using an LLM.
     *
     * @param templateCode The template code to annotate
     * @param componentName The name of the component
     * @return JSON-LD string or null if generation failed
     */
    fun generateTemplateAnnotation(templateCode: String, componentName: String): String? = runBlocking {
        try {
            val prompt = buildAnnotationPrompt(templateCode, componentName)
            val options = OllamaOptions(temperature = 0.3, top_k = 100, top_p = 0.9)

            val response = PrototypeInteractor.prompt(prompt, annotationModel, options)
                ?: throw PromptException("LLM did not respond for annotation generation")

            val jsonText = response.response ?: throw PromptException("Empty response from LLM")
            val jsonElement = Json.parseToJsonElement(jsonText)

            when (jsonElement) {
                is JsonObject -> {
                    val annotation = jsonElement["annotation"]?.jsonPrimitive?.content
                        ?: throw PromptException("No annotation field in LLM response")
                    return@runBlocking annotation
                }
                is JsonArray -> {
                    if (jsonElement.isEmpty()) throw PromptException("Empty JSON array in response")
                    val firstObject = jsonElement[0].jsonObject
                    val annotation = firstObject["annotation"]?.jsonPrimitive?.content
                        ?: throw PromptException("No annotation field in LLM response")
                    return@runBlocking annotation
                }
                else -> throw PromptException("Unexpected JSON format in LLM response")
            }
        } catch (e: Exception) {
            println("Failed to generate annotation: ${e.message}")
            return@runBlocking null
        }
    }

    /**
     * Builds a prompt for generating a JSON-LD annotation for a template.
     *
     * @param templateCode The template code to analyze
     * @param componentName The name of the component
     * @return A formatted prompt for the LLM
     */
    fun buildAnnotationPrompt(templateCode: String, componentName: String): String {
        return """
        You are an expert in generating JSON-LD metadata for software components.
        Analyze this React component named "$componentName" and create JSON-LD annotation following Schema.org's SoftwareSourceCode format.
        
        Template to analyze:
        ```typescript
        $templateCode
        ```
        
        Generate a JSON-LD annotation that includes:
        1. @context: https://schema.org/
        2. @type: SoftwareSourceCode
        3. name: "$componentName"
        4. description: A detailed description of what the component does and how it can be used
        5. programmingLanguage: {"@type": "ComputerLanguage", "name": "TypeScript"}
        6. framework: {"@type": "SoftwareApplication", "name": "React"}
        7. applicationCategory: "User Interface Component"
        8. keywords: At least 5 relevant keywords that describe this component's functionality and use cases
        
        Format your response as a JSON object with a single key "annotation" containing the JSON-LD as a string.
        Example:
        {
          "annotation": "{\\"@context\\": \\"https://schema.org/\\", \\"@type\\": \\"SoftwareSourceCode\\", \\"name\\": \\"$componentName\\", ... }"
        }
        
        Only return the JSON object, no explanations or additional text.
        """
    }

    /**
     * Integrates the template processing capability with the main workflow.
     * This method can be called after receiving an LLM response in PromptingMain.
     *
     * @param response The LLM response to process for templates
     */
    suspend fun processTemplatesFromWorkflow(response: EnhancedResponse) {
        val processedTemplates = processTemplatesFromResponse(response)
        if (processedTemplates.isNotEmpty()) {
            println("Successfully added ${processedTemplates.size} new templates to the library:")
            processedTemplates.forEach { println("- $it") }
        }
    }
}