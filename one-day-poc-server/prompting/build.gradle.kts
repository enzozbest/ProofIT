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
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation(project(":prototype"))
    implementation(project(":embeddings"))
    implementation(project(":database"))
    implementation(project(":utils"))
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation(project(":utils"))
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xmulti-dollar-interpolation")
    }
}
