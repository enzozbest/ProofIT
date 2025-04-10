val ktorVersion: String by project
val kotlinVersion: String by project

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
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.ktor:ktor-client-plugins:$ktorVersion")
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1")
    implementation("org.jsoup:jsoup:1.16.1")
    implementation("net.sf.jtidy:jtidy:r938")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
    testImplementation("io.ktor:ktor-client-mock:3.0.3")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    testImplementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation(project(":utils"))
}
