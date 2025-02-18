val ktorVersion by extra { "3.0.3" }
val kotlinVersion by extra { "2.1.0" }

plugins {
    kotlin("jvm") version "2.1.0"
    id("application")
    id("io.gitlab.arturbosch.detekt") version "1.23.0"
    id("org.sonarqube") version "4.0.0.2929"
    id("jacoco")
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.register<JacocoReport>("jacocoMergedReport") {
    dependsOn(subprojects.map { it.tasks.named("test") })
    dependsOn(subprojects.map { it.tasks.named("jacocoTestReport") })

    executionData.setFrom(
        subprojects.mapNotNull {
            it.tasks
                .withType<JacocoReport>()
                .findByName("jacocoTestReport")
                ?.executionData
        },
    )

    subprojects.forEach { subproject ->
        additionalSourceDirs.from(
            subproject.extensions
                .getByType<JavaPluginExtension>()
                .sourceSets
                .getByName("main")
                .allSource.srcDirs,
        )
        sourceDirectories.from(
            subproject.extensions
                .getByType<JavaPluginExtension>()
                .sourceSets
                .getByName("main")
                .allSource.srcDirs,
        )
        classDirectories.from(subproject.buildDir.resolve("classes/kotlin/main"))
    }

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

detekt {
    toolVersion = "1.23.0"
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = true
    baseline = file("detekt-baseline.xml")
    reportsDir = file("build/reports/detekt.html")
    config.setFrom("$rootDir/detekt.yml")
}

sonarqube {
    properties {
        property("sonar.host.url", "http://localhost:9000")
        property("sonar.token", "sqa_9d0266531f00a65342739c57874cebd9a91e7cea")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${project.layout.buildDirectory}/reports/jacoco/jacocoMergedReport/jacocoMergedReport.xml",
        )
    }
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
    apply(plugin = "io.gitlab.arturbosch.detekt")
    apply(plugin = "jacoco")
    dependencies {
        detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.0")
        implementation("io.ktor:ktor-server-core:$ktorVersion")
        implementation("io.ktor:ktor-server-netty:$ktorVersion")
        implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
        implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
        implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
        implementation("ch.qos.logback:logback-classic:1.4.11")
        testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
        testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
        testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
        testImplementation("io.mockk:mockk:1.13.16")
        testImplementation("net.bytebuddy:byte-buddy:1.14")
        testImplementation("net.bytebuddy:byte-buddy-agent:1.14")
        testImplementation(kotlin("test"))
    }
    detekt {
        toolVersion = "1.23.0"
        buildUponDefaultConfig = true
        autoCorrect = true
        config.setFrom("$rootDir/detekt.yml")
    }
    jacoco {
        toolVersion = "0.8.10"
    }

    tasks.jacocoTestReport {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.0")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation(project(":auth"))
    implementation(project(":prototype"))
    implementation(project(":webcontainer"))
    implementation(project(":database"))
    implementation(project(":routes"))
    implementation(project(":utils"))
    implementation(project(":chat_history"))
    implementation("org.jsoup:jsoup:1.15.3")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<Test> {
    maxParallelForks = Runtime.getRuntime().availableProcessors()
    forkEvery = 10
}

tasks.named("run") {
    dependsOn("startDocker")
}

tasks.register<Exec>("startDocker") {
    group = "docker"
    description = "Starts the PostgreSQL docker container"
    commandLine("docker-compose", "up", "-d")
}

allprojects {
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(23))
        }
    }
}
