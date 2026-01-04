import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.gradle.api.GradleException
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar

val ktorVersion = "3.3.3"
val version = "5.8.5"
project.version = version
plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("com.gradleup.shadow") version "9.3.0+"
    `java-library`
    idea
    application
}

tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.BIN
}

repositories {
    mavenLocal()
    exclusiveContent {
        forRepository {
            maven {
                name = "IzzelAliz Maven"
                url = uri("https://maven.izzel.io/releases/")
            }
        }
        filter {
            includeGroup("icyllis.modernui")
        }
    }
}

base {
    archivesName.set("rdi-5-ui")
}

tasks.named<Jar>("jar") {
    archiveClassifier.set("plain")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    archiveFileName.set("rdi-5-ui.jar")
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Built-By" to System.getProperty("user.name"),/*
                "Build-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .format(java.util.Date()),*/
                "Created-By" to "Gradle ${gradle.gradleVersion}"
            )
        )
    }
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

allprojects {
    configurations.all {
        resolutionStrategy {
            force("org.slf4j:slf4j-api:2.0.16")
        }
    }
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("io.netty:netty-all:4.2.7.Final")
    implementation(project(":common"))
    implementation(project(":client-common"))
    val lwjglVersion = "3.3.3"
    val components = listOf("", "glfw", "opengl", "openal", "stb", "tinyfd", "jemalloc")
    components.forEach { component ->
        val suffix = if (component.isNotEmpty()) "-$component" else ""
        implementation("org.lwjgl:lwjgl${suffix}:$lwjglVersion")
        implementation("org.lwjgl:lwjgl${suffix}:$lwjglVersion:natives-windows")
    }

    implementation("org.hotswapagent:hotswap-agent-core:2.0.1")
    implementation("icyllis.modernui:ModernUI-Core:3.12.0")
    testImplementation("icyllis.modernui:ModernUI-Core:3.12.0")
    implementation("icyllis.modernui:ModernUI-Markflow:3.12.0")
    testImplementation("icyllis.modernui:ModernUI-Markflow:3.12.0")
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

configurations.configureEach {
    resolutionStrategy {
        force("com.ibm.icu:icu4j:76.1")
        force("it.unimi.dsi:fastutil:8.5.15")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named<Test>("test") {
    enabled = false
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

fun registerCopyTask(name: String, extraDestinations: List<String> = emptyList()) {
    tasks.register(name) {
        dependsOn(tasks.named("build"))
        val artifact = layout.buildDirectory.file("libs/rdi-5-ui.jar")
        val baseDestinations = listOf(
            layout.projectDirectory.dir("..\\..\\server\\master\\run\\client-libs"),
            layout.projectDirectory.dir("${System.getProperty("user.home")}\\Documents\\rdi5ship")
        )
        val destinationDirs = baseDestinations + extraDestinations.map { layout.projectDirectory.dir(it) }

        doLast {
            val jarFile = artifact.get().asFile
            if (!jarFile.exists()) {
                throw GradleException("未找到构建产物: $jarFile")
            }
            destinationDirs.forEach { target ->
                val targetDir = target.asFile
                targetDir.mkdirs()
                val destFile = targetDir.resolve(jarFile.name)
                Files.copy(
                    jarFile.toPath(),
                    destFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            }
        }
    }
}
tasks.register<Exec>("makeShipPack") {
    dependsOn(tasks.named("出core-local"))

    val shipDir = layout.projectDirectory.dir("${System.getProperty("user.home")}\\Documents\\rdi5ship")
    val filesNeed = listOf("rdi-5-ui.jar", "双击启动.cmd", "fonts", "jre")
    val zipFile = shipDir.file("rdi5ship.zip")

    workingDir = shipDir.asFile

    doFirst {
        val shipDirFile = shipDir.asFile
        val sevenZip = File("C:\\Program Files\\7-Zip\\7z.exe")
        if (!sevenZip.exists()) throw GradleException("未找到 7-Zip: $sevenZip")
        if (!shipDirFile.exists()) throw GradleException("未找到 ship 目录: $shipDirFile")

        filesNeed.forEach { name ->
            val target = File(shipDirFile, name)
            if (!target.exists()) throw GradleException("缺少文件或目录: $target")
        }

        val zipTarget = zipFile.asFile
        if (zipTarget.exists() && !zipTarget.delete()) {
            throw GradleException("无法删除旧压缩包: $zipTarget")
        }

        commandLine(
            sevenZip.absolutePath,
            "a",
            "-tzip",
            "-mmt",

            zipTarget.absolutePath,
            *filesNeed.toTypedArray()
        )
    }
}
registerCopyTask("出core-local")
registerCopyTask("出core-release", listOf("\\\\rdi5\\rdi55\\ihq\\client-libs"))

application {
    mainClass.set("calebxzhou.rdi.RDIKt")
}
