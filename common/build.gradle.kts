import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
	kotlin("jvm") version "2.2.21"
	kotlin("plugin.serialization") version "2.2.21"
}

group = "calebxzhou.rdi.common"
version = "0.1"

val ktorVersion = "3.3.3"
val kotlinLoggingVersion = "7.0.6"
repositories {
    mavenLocal()
    mavenCentral()
}
dependencies {
    implementation("calebxzhou.mykotutils:std:0.1")
    implementation("calebxzhou.mykotutils:log:0.1")
    implementation("ch.qos.logback:logback-classic:1.5.21")
    implementation("org.jsoup:jsoup:1.19.1")
    implementation("org.mongodb:bson:5.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("com.electronwill.night-config:toml:3.8.3")
    implementation("net.peanuuutz.tomlkt:tomlkt:0.5.0")

    implementation("io.ktor:ktor-client-core:${ktorVersion}")
    implementation("io.ktor:ktor-client-okhttp:${ktorVersion}")
    implementation("io.ktor:ktor-client-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-client-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-client-encoding:${ktorVersion}")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")

    implementation("io.github.oshai:kotlin-logging-jvm:${kotlinLoggingVersion}")
    testImplementation("io.github.oshai:kotlin-logging-jvm:${kotlinLoggingVersion}")

    implementation("net.benwoodworth.knbt:knbt:0.11.8")

	testImplementation(kotlin("test"))
}