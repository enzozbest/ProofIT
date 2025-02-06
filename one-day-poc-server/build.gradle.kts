val ktorVersion by extra { "3.0.3" }
val kotlinVersion by extra { "2.1.0" }

plugins {
    kotlin("jvm") version "2.1.0"
    id("application")
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

group = "kcl.seg.rtt"
version = "0.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    dependencies {
        implementation("io.ktor:ktor-server-core:$ktorVersion")
        implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
        implementation("ch.qos.logback:logback-classic:1.4.11")
        implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
        testImplementation(kotlin("test"))
        testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
        testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
        implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.1")
        testImplementation("org.mockito:mockito-core:5.12")
        testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
        testImplementation("io.mockk:mockk:1.13.16")
        testImplementation("net.bytebuddy:byte-buddy:1.14")
        testImplementation("net.bytebuddy:byte-buddy-agent:1.14")
        implementation("io.ktor:ktor-server-netty:$ktorVersion")
    }
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation(project(":routes"))
    implementation(project(":chat_history"))
    // JSoup for HTML sanitization
    implementation("org.jsoup:jsoup:1.15.3")
    implementation(project(":auth"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}