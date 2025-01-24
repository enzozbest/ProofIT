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
        testImplementation(kotlin("test"))
    }
}

dependencies {
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation(project(":routes"))
}
tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}