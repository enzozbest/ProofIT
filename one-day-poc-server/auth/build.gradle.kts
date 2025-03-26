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
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("redis.clients:jedis:5.2.0")
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    implementation(project(":utils"))
    testImplementation("com.squareup.okhttp3:mockwebserver:5.0.0-alpha.14")
    testImplementation("org.testcontainers:testcontainers:1.19.5")
    testImplementation("org.testcontainers:junit-jupiter:1.19.5")
    testImplementation(kotlin("test"))
}
