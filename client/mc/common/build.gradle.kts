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

group = "calebxzhou.rdi.mc.common"
version = "0.1"

val ktorVersion = "3.3.3"
val kotlinLoggingVersion = "7.0.6"
repositories {
    mavenLocal()
    mavenCentral()
}
dependencies {

	testImplementation(kotlin("test"))
}