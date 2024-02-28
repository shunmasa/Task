plugins {
    kotlin("jvm") version "1.9.22"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.8.0")// HTML DSL
    implementation("org.webjars.npm:htmx.org:1.8.0")// Requires manual update in contants.kt when upgrading
    implementation("io.javalin:javalin:4.6.4")
    implementation("org.slf4j:slf4j-simple:2.0.0")// required by javalin
    implementation("io.ktor:ktor-server-netty:1.6.3")
    implementation("io.ktor:ktor-html-builder:1.6.3")
    implementation("io.ktor:ktor-server-core:1.6.3")
    implementation("io.ktor:ktor-server-host-common:1.6.3")
    implementation("io.ktor:ktor-server-sessions:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.2.1")
//    implementation("io.ktor:ktor-server-sessions-redis:1.6.3")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {

    jvmToolchain(21)
}