import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.internal.builtins.StandardNames.FqNames.target
import java.nio.file.Files
import java.nio.file.StandardCopyOption


plugins {
    kotlin("jvm") version "2.2.21"
    id("com.gradleup.shadow") version "9.2.0"
}

group = "calebxzhou.rdi"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.13")
    implementation("io.netty:netty-all:4.2.5.Final")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes("Main-Class" to "calebxzhou.rdi.prox.MainKt")
    }
}
tasks.named<Test>("test") {
    enabled = true
}
tasks.test {
    useJUnitPlatform()
}
base {
    archivesName.set("prox")
}
tasks.named<Jar>("jar") {
    archiveClassifier.set("plain")
}
kotlin {
    jvmToolchain(21)
}
tasks.register("出core") {
    dependsOn(tasks.named("build"))
    val artifact = layout.buildDirectory.file("libs/rdi-5-mc-client-1.21.1-neoforge.jar")

    doLast {
        val jarFile = artifact.get().asFile
        if (!jarFile.exists()) {
            throw GradleException("未找到构建产物: $jarFile")
        }
            val targetDir = layout.projectDirectory.dir("\\\\rdi5\\rdi55\\prox\\").asFile
            val destFile = targetDir.resolve(jarFile.name)
            Files.copy(
                jarFile.toPath(),
                destFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )

    }
}