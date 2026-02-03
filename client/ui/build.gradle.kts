import org.gradle.jvm.tasks.Jar
import java.text.SimpleDateFormat
import java.util.*

val ktorVersion = "3.3.3"
val version = "5.10.6"
project.version = version
plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
    id("org.jetbrains.compose") version "1.10.0-rc02"
    `java-library`
    idea
    application
}

tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.BIN
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

base {
    archivesName.set("rdi-5-ui")
}
val hotDevClassName = providers.gradleProperty("className")
    .orElse("calebxzhou.rdi.client.MainKt")

tasks.matching { it.name == "hotRun" }.configureEach {
    val runDir = layout.projectDirectory.dir("run").asFile
    doFirst {
        runDir.mkdirs()
    }
    if (this is JavaExec) {
        workingDir = runDir
        jvmArgs(
            "-Drdi.debug=true",
           // "-Drdi.init.screen=ml",
           // "-Drdi.mockData=true",
            "-Drdi.account=eyJfaWQiOiI2OGIzMTRiYmFkYWY1MmRkYWI5NmI1ZWQiLCJuYW1lIjoiMTIzMTIzIiwicHdkIjoiMTIzQEBAIiwicXEiOiIxMjMxMjMifQ=="
        )
    }
}
tasks.named<Jar>("jar") {
    archiveFileName.set("rdi-5-ui.jar")
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Built-By" to System.getProperty("user.name"),
                "Build-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                    .format(Date()),
                "Created-By" to "Gradle ${gradle.gradleVersion}"
            )
        )
    }
//archiveClassifier.set("plain")
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
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.androidx.navigation:navigation-compose:2.9.1")
    // Source: https://mvnrepository.com/artifact/org.jetbrains.compose.material3/material3-desktop
    implementation("org.jetbrains.compose.material3:material3-desktop:1.10.0-alpha05")
    val lwjglVersion = "3.3.3"
    val components = listOf("", "glfw", "opengl")
    components.forEach { component ->
        val suffix = if (component.isNotEmpty()) "-$component" else ""
        implementation("org.lwjgl:lwjgl${suffix}:$lwjglVersion")
        implementation("org.lwjgl:lwjgl${suffix}:$lwjglVersion:natives-windows")
    }

    testImplementation(kotlin("test"))
    implementation("io.netty:netty-all:4.2.7.Final")
    implementation(project(":common"))

    runtimeOnly("org.hotswapagent:hotswap-agent-core:2.0.1")
    implementation("com.github.oshi:oshi-core:6.9.1")
    implementation("com.electronwill.night-config:toml:3.8.3")
    implementation("ch.qos.logback:logback-classic:1.5.21")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.6")
    implementation("net.peanuuutz.tomlkt:tomlkt:0.5.0")

    implementation("calebxzhou.mykotutils:std:0.1")
    implementation("calebxzhou.mykotutils:log:0.1")
    implementation("calebxzhou.mykotutils:ktor:0.1")
    implementation("calebxzhou.mykotutils:hwspec:0.1")
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
    enabled = true
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
fun registerCopyTask(name: String, extraDestinations: List<String> = emptyList()) {
    tasks.register(name) {
        notCompatibleWithConfigurationCache("uses project file operations at execution time")
        dependsOn(tasks.named("installDist"))
        val baseDestinations = listOf(
            layout.projectDirectory.dir("..\\..\\server\\master\\run\\client-libs\\lib"),
            layout.projectDirectory.dir("${System.getProperty("user.home")}\\Documents\\rdi5ship\\lib")
        )
        val destinationDirs = baseDestinations + extraDestinations.map { layout.projectDirectory.dir(it) }

        doLast {
            val srcDir = layout.buildDirectory.dir("install/ui/lib").get().asFile
            if (!srcDir.exists()) {
                throw GradleException("未找到目录: $srcDir")
            }
            val srcFileNames = srcDir.listFiles()?.map { it.name }?.toSet() ?: emptySet()
            destinationDirs.forEach { target ->
                val targetDir = target.asFile
                targetDir.mkdirs()
                copy {
                    from(srcDir)
                    into(targetDir)
                }
                // Delete unused libraries in target
                targetDir.listFiles()?.forEach { file ->
                    if (file.name !in srcFileNames) {
                        val deleted = file.deleteRecursively()
                        if (deleted) {
                            println("Deleted unused library: ${file.name}")
                        } else {
                            println("Failed to delete: ${file.name}")
                        }
                    }
                }
            }
        }
    }
}
registerCopyTask("出core2-local")
registerCopyTask("出core2-release", listOf("\\\\rdi\\rdi55\\ihq\\client-libs\\lib"))
tasks.register<Exec>("makeShipPack") {
   // dependsOn(tasks.named("出core2-local"))

    val shipDir = layout.projectDirectory.dir("${System.getProperty("user.home")}\\Documents\\rdi5ship")
    val filesNeed = listOf("lib", "双击启动.cmd", "fonts", "jre","jre8")
    val zipFile = shipDir.file("rdi5ship-${version}.zip")

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


application {
    mainClass.set("calebxzhou.rdi.client.MainKt")
}
