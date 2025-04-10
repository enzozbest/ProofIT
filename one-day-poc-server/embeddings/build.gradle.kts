val ktorVersion: String by rootProject.extra

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.1.0"
}

group = "kcl.seg.rtt"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":database"))
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation(kotlin("test"))
}
