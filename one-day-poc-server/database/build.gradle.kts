val ktorVersion: String by rootProject.extra
val kotlinVersion: String by rootProject.extra

plugins {
    kotlin("jvm")
}

group = "kcl.seg.rtt"
version = "0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://aws-sdk-kotlin.bintray.com/aws-sdk-kotlin")
}

dependencies {
    implementation("org.jetbrains.exposed:exposed-core:0.43.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.43.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.43.0")
    implementation("org.jetbrains.exposed:exposed-json:0.43.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.43.0")
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.flywaydb:flyway-core:9.16.0")
    testImplementation("org.jetbrains.exposed:exposed-core:0.43.0")
    testImplementation("org.jetbrains.exposed:exposed-dao:0.43.0")
    testImplementation("org.jetbrains.exposed:exposed-jdbc:0.43.0")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    testImplementation(kotlin("test"))
    implementation(project(":utils"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}
