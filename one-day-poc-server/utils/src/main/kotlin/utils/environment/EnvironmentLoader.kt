package utils.environment

import io.github.cdimascio.dotenv.Dotenv
import java.io.File

/**
 * This object is used to load environment variables to the program.
 * Variables can be set directly in the system or in an environment file. By default, a file named .env will be loaded.
 * However, a file can be used by calling [loadEnvironmentFile] explicitly where needed.
 */
object EnvironmentLoader {
    private var env: Dotenv? = null

    /**
     * This function loads an environment file.
     */
    fun loadEnvironmentFile(fileName: String) {
        if (File(fileName).exists()) {
            env =
                Dotenv
                    .configure()
                    .directory("./")
                    .filename(fileName)
                    .load()
        }
    }

    /**
     * This function gets the value of an environment variable.
     */
    fun get(key: String): String = env?.get(key) ?: SystemEnvironment.readSystemVariable(key) ?: ""

    /**
     * This function resets the environment to null, i.e. it unloads the previously loaded environment.
     */
    fun reset() {
        env = null
    }
}

/**
 * This object is used to read system environment variables.
 * The main purpose of wrapping the call to [readSystemVariable] in this object is to increase
 * testability and scalability, following the Open/Closed Principle.
 */
internal object SystemEnvironment {
    /**
     * This function reads a system environment variable.
     * @param variableName The name of the variable to read.
     * @return The value of the variable, or null if the variable does not exist.
     */
    fun readSystemVariable(variableName: String): String? = System.getenv(variableName)
}
