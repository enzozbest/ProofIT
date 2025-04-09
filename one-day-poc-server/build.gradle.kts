val ktorVersion by extra { "3.0.3" }
val kotlinVersion by extra { "2.1.0" }

plugins {
    kotlin("jvm") version "2.1.0"
    id("application")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.sonarqube") version "4.0.0.2929"
    jacoco
}

tasks.register<JavaExec>("seed") {
    group = "Seeding"
    description = "Seeds the Template Library."
    mainClass.set("server.TemplateLibrarySeederKt")
    classpath = sourceSets["main"].runtimeClasspath
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

tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = "io.ktor.server.netty.EngineMain"
    }
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
                        """**/*${"$$"}serializer*.*""",
                        """server/*""",
                        """**/TemplateLibrarySeeder.kt""",
                    )
                },
            )
        }
    }
}

dependencies {
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
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.5.18")
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

allprojects {
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(19))
        }
    }
}

tasks.named("run") {
    dependsOn("startDocker")
}

tasks.register<Exec>("startDocker") {
    group = "docker"
    description = "Starts the PostgreSQL docker container"
    commandLine("sh", "-c", "docker compose up -d || docker-compose up -d")
}
