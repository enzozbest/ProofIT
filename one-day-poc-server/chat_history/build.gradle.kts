val ktorVersion: String by rootProject.extra
val kotlinVersion: String by rootProject.extra

plugins {
    id("java")
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "1.9.20"
}

group = "kcl.seg.rtt"
version = "0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("org.jsoup:jsoup:1.15.3") // Used in JsonRoutes.kt
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation(kotlin("test"))

    implementation(project(":auth"))
    implementation(project(":utils"))
}

tasks.test {
    useJUnitPlatform()
}