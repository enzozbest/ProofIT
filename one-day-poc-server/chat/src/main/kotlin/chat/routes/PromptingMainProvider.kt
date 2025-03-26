package chat.routes

import prompting.PromptingMain

/**
 * Singleton provider for PromptingMain instance.
 *
 * This class manages a single instance of PromptingMain throughout the application
 * and provides methods to get, set, and reset the instance.
 */
object PromptingMainProvider {
    private lateinit var promptingMainInstance: PromptingMain

    /**
     * Gets the current PromptingMain instance, initializing it if necessary.
     *
     * @return The current PromptingMain instance
     */
    fun getInstance(): PromptingMain {
        if (!::promptingMainInstance.isInitialized) {
            promptingMainInstance = PromptingMain()
        }
        return promptingMainInstance
    }

    /**
     * Sets a custom PromptingMain instance.
     *
     * This function is primarily used for testing purposes to inject a mock or
     * customized PromptingMain implementation.
     *
     * @param promptObject The PromptingMain instance to use for processing requests
     */
    fun setInstance(promptObject: PromptingMain) {
        promptingMainInstance = promptObject
    }

    /**
     * Resets the PromptingMain instance to a new default instance.
     *
     * This function is used to restore the default behavior of the prompting
     * workflow, typically after testing or when a fresh state is required.
     */
    fun resetInstance() {
        promptingMainInstance = PromptingMain()
    }
}
