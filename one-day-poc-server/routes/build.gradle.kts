val ktorVersion: String by rootProject.extra

plugins {
    kotlin("jvm")
}

group = "kcl.seg.rtt"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":auth"))
    implementation(project(":chat"))
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
