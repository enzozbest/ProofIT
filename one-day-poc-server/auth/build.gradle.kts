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
    implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.14")
    implementation(project(":utils"))
    testImplementation("com.squareup.okhttp3:mockwebserver:5.0.0-alpha.14")
    testImplementation(kotlin("test"))

    /*constraints {
        implementation("com.squareup.okhttp3:okhttp") {
            version {
                strictly("4.12.0")
            }
        }
    }*/
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}
