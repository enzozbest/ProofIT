val ktorVersion: String by rootProject.extra
val kotlinVersion: String by rootProject.extra

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.1.0"
}

group = "kcl.seg.rtt"
version = "0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation(project(":utils"))
    testImplementation("com.squareup.okhttp3:mockwebserver:4.9.3")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}