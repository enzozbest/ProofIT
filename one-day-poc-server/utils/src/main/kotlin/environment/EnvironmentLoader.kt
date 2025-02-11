package kcl.seg.rtt.utils.environment

import io.github.cdimascio.dotenv.Dotenv
import java.io.File

object EnvironmentLoader {
    private var env: Dotenv? = null

    fun loadEnvironmentFile(fileName: String = ".env") {
        if (File(fileName).exists())
            env = Dotenv.configure().directory("./").filename(fileName).load()
    }

    fun get(key: String): String {
        return env?.get(key) ?: ""
    }

    fun reset() {
        env = null
    }
}