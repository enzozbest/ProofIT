package prompting.helpers.templates

import templates.TemplateService
import templates.TemplateStorageService
import utils.environment.EnvironmentLoader

/**
 * Interacts with templates by fetching and storing them.
 * Uses TemplateService for embedding and searching, and TemplateStorageUtils for file operations.
 */
object TemplateInteractor {
    /**
     * Fetches templates that match the given prompt.
     *
     * @param prompt The prompt to match templates against
     * @return A list of template contents as strings
     */
    suspend fun fetchTemplates(prompt: String): List<String> {
        val embedding = runCatching { TemplateService.embed(prompt, "prompt").embedding }.getOrElse { emptyList() }
        val templateIds = runCatching { TemplateService.search(embedding, prompt).matches }.getOrElse { emptyList() }

        println(templateIds)

        return templateIds.mapNotNull { id ->
            getTemplateContent(id)
        }
    }

    /**
     * Retrieves the content of a template by its ID.
     *
     * @param id The ID of the template
     * @return The content of the template as a string, or null if not found
     */
    private suspend fun getTemplateContent(id: String): String? =
        TemplateStorageService.getTemplateById(id)?.fileURI?.let { templateHandle ->
            val fileContent = TemplateStorageUtils.retrieveFileContent(templateHandle)
            fileContent.decodeToString()
        }

    /**
     * Stores a new template with its associated metadata.
     *
     * @param templateID The ID of the template
     * @param templateCode The template code to store
     * @param jsonLD The JSON-LD metadata for the template
     * @return true if the template was successfully stored, false otherwise
     */
    suspend fun storeNewTemplate(
        templateID: String,
        templateCode: String,
        jsonLD: String,
    ): Boolean =
        runCatching {
            // Store template file
            val templateConfig =
                TemplateStorageUtils.StorageConfig(
                    path = "templates",
                    key = "$templateID.templ",
                    bucket = EnvironmentLoader.get("S3_BUCKET_TEMPLATES"),
                )
            val templateFilePath =
                TemplateStorageUtils.storeFile(
                    content = templateCode,
                    filePrefix = "template_${templateID}_",
                    fileSuffix = ".txt",
                    storageConfig = templateConfig,
                )

            if (templateFilePath.isEmpty()) return@runCatching false

            // Store JSON-LD file
            val jsonLDConfig =
                TemplateStorageUtils.StorageConfig(
                    path = "templates/metadata",
                    key = "jsonld_$templateID.json",
                    bucket = EnvironmentLoader.get("S3_BUCKET_TEMPLATES"),
                )
            val jsonLDFilePath =
                TemplateStorageUtils.storeFile(
                    content = jsonLD,
                    filePrefix = "jsonld_${templateID}_",
                    fileSuffix = ".json",
                    storageConfig = jsonLDConfig,
                )

            if (jsonLDFilePath.isEmpty()) return@runCatching false

            // Create template in database and store in template service
            val createdTemplateId = TemplateStorageService.createTemplate(templateFilePath)
            if (createdTemplateId == null) return@runCatching false

            TemplateService.storeTemplate(createdTemplateId, templateFilePath).status == "success"
        }.getOrElse { false }
}
