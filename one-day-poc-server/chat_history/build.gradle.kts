val ktorVersion by extra { "3.0.3" }
val kotlinVersion by extra { "2.1.0" }
val serializationVersion by extra { "1.5.1" }

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
    // Your existing dependencies but with corrected versions
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-html-builder:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.5.1")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    // JSoup for HTML sanitization
    implementation("org.jsoup:jsoup:1.15.3")
}

tasks.test {
    useJUnitPlatform()
}