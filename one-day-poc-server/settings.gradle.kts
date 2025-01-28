plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "one-day-poc-server"
include("routes")
include("chat_history")
