import org.jetbrains.compose.reload.core.Environment.Companion.application

val ktorVersion = "3.3.3"
plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
    id("org.jetbrains.compose") version "1.10.0-rc02"
    `java-library`
    idea
    application
}


repositories {
    mavenLocal()
    mavenCentral()
    google()
}

base {
    archivesName.set("rdi-5-ui")
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("plain")
}
dependencies {
    implementation(compose.desktop.currentOs)
    testImplementation(kotlin("test"))
    implementation("io.netty:netty-all:4.2.7.Final")
    implementation(project(":common"))
    implementation("org.hotswapagent:hotswap-agent-core:2.0.1")
    implementation("com.github.oshi:oshi-core:6.9.1")
    implementation("com.electronwill.night-config:toml:3.8.3")
    implementation("ch.qos.logback:logback-classic:1.5.21")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.6")
    implementation("net.peanuuutz.tomlkt:tomlkt:0.5.0")

    implementation("calebxzhou.mykotutils:std:0.1")
    implementation("calebxzhou.mykotutils:log:0.1")
    implementation("calebxzhou.mykotutils:ktor:0.1")
    implementation("calebxzhou.mykotutils:hwspec:0.1")
    implementation("calebxzhou.mykotutils:curseforge:0.1")
    implementation("calebxzhou.mykotutils:modrinth:0.1")
    implementation("calebxzhou.mykotutils:mojang:0.1")

    // Expanded mcLibs dependencies
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-encoding:$ktorVersion")
    implementation("org.jsoup:jsoup:1.19.1")
    implementation("org.mongodb:bson:5.1.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.2")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.9.0")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
application {
    mainClass.set("calebxzhou.rdi.client.MainKt")
}
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}
