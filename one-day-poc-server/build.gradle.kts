val ktorVersion by extra { "3.0.3" }
val kotlinVersion by extra { "2.1.0" }

plugins {
    kotlin("jvm") version "2.1.0"
    id("application")
    // id("io.gitlab.arturbosch.detekt") version "1.23.0"
    id("org.sonarqube") version "4.0.0.2929"
    jacoco
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

// detekt {
//    toolVersion = "1.23.0"
//    buildUponDefaultConfig = true
//    allRules = false
//    autoCorrect = true
//    baseline = file("detekt-baseline.xml")
//    reportsDir = file("build/reports/detekt.html")
//    config.setFrom("$rootDir/detekt.yml")
// }

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
//    apply(plugin = "io.gitlab.arturbosch.detekt")
    plugins.withType<org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper> {
        apply(plugin = "jacoco")
    }
    dependencies {
        implementation("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.0")
        implementation("io.ktor:ktor-server-core:$ktorVersion")
        implementation("io.ktor:ktor-server-netty:$ktorVersion")
        implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
        implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
        implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
        implementation("ch.qos.logback:logback-classic:1.4.12")
        implementation("org.slf4j:slf4j-api:1.7.36")
        testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
        testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
        testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
        testImplementation("io.mockk:mockk:1.13.16")
        testImplementation("net.bytebuddy:byte-buddy:1.14")
        testImplementation("net.bytebuddy:byte-buddy-agent:1.14")
        testImplementation(kotlin("test"))
    }
//    detekt {
//        toolVersion = "1.23.0"
//        buildUponDefaultConfig = true
//        autoCorrect = true
//        config.setFrom("$rootDir/detekt.yml")
//    }

    tasks.withType<Test> {
        useJUnitPlatform()
        finalizedBy("jacocoTestReport")
    }

    plugins.withType<JacocoPlugin> {
        tasks.withType<JacocoReport> {
            dependsOn(tasks.named<Test>("test"))
            reports {
                xml.required.set(true)
                html.required.set(true)
            }
            classDirectories.setFrom(
                sourceSets["main"].output.asFileTree.matching {
                    exclude(
                        """**/*${'$'}DefaultImpls*.*""",
                        """**/*${'$'}WhenMappings*.*""",
                        """**/*${'$'}SuspendImpl*.*""",
                        """**/*${'$'}delegate*.*""",
                        """**/*${'$'}Function*.*""",
                        """**/*${'$'}Metadata*.*""",
                        """**/*${'$'}Companion*.*""",
                        """server/*""",
                        """routes/*""",
                        """**/TemplateLibrarySeeder.kt""",
                    )
                },
            )
        }
    }
}

dependencies {
    // detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.0")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation(project(":auth"))
    implementation(project(":prototype"))
    implementation(project("embeddings"))
    api(project(":database"))
    implementation(project(":routes"))
    implementation(project(":utils"))
    implementation(project(":chat"))
    implementation(project(":prompting"))
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("ch.qos.logback:logback-classic:1.4.12")
    implementation("org.jsoup:jsoup:1.15.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
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
    commandLine("sh", "-c", "docker compose up -d || docker-compose up -d")
}

allprojects {
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(19))
        }
    }
}
