plugins {
    kotlin("jvm")
}

group = "kcl.seg.rtt"
version = "0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("aws.sdk.kotlin:s3:1.4.11")
    implementation("aws.sdk.kotlin:sts:1.4.11")
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    testImplementation("io.findify:s3mock_2.13:0.2.6")
    testImplementation("io.mockk:mockk:1.13.16")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    testImplementation(kotlin("test"))
    implementation("aws.smithy.kotlin:http-client-engine-okhttp:1.4.2") {
        // exclude(group = "com.squareup.okhttp3", module = "okhttp")
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(23)
}
