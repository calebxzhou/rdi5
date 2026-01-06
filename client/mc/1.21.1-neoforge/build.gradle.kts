import calebxzhou.gradle.plugins.AutoEmbedJarJarTransitivesExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import org.gradle.language.jvm.tasks.ProcessResources
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

val kotlinVersion = providers.gradleProperty("kotlin_version").get()
val ktorVersion = providers.gradleProperty("ktor_version").get()
val modId = providers.gradleProperty("mod_id").get()
val modGroupId = providers.gradleProperty("mod_group_id").get()
val modVersion = providers.gradleProperty("mod_version").get()
val neoVersion = providers.gradleProperty("neo_version").get()
val parchmentMinecraftVersion = providers.gradleProperty("parchment_minecraft_version").get()
val parchmentMappingsVersion = providers.gradleProperty("parchment_mappings_version").get()
val minecraftVersion = providers.gradleProperty("minecraft_version").get()
val minecraftVersionRange = providers.gradleProperty("minecraft_version_range").get()
val neoVersionRange = providers.gradleProperty("neo_version_range").get()
val loaderVersionRange = providers.gradleProperty("loader_version_range").get()

plugins {
    `java-library`
    `maven-publish`
    idea
    id("net.neoforged.moddev") version "2.0.124"
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("auto-embed-jarjar-transitives") version "0.1.0"
}

tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.BIN
}

version = modVersion
group = modGroupId

repositories {
    mavenLocal()
}

base {
    archivesName.set(modId)
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

neoForge {
    version = neoVersion

    parchment {
        mappingsVersion = parchmentMappingsVersion
        minecraftVersion = parchmentMinecraftVersion
    }

    validateAccessTransformers = true

    runs {
        register("client") {
            client()
            programArgument("--width")
            programArgument("2560")
            programArgument("--height")
            programArgument("1440")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
            systemProperty("mixin.hotSwap", "true")
            systemProperty("rdi.ihq.url", "http://127.0.0.1:65231")
            systemProperty("rdi.game.ip", "127.0.0.1:65230")
            systemProperty("rdi.host.name", "测试测试12123主机")
            systemProperty("rdi.host.port", "25565")
        }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        register(modId) {
            val sourceSets = the<SourceSetContainer>()
            sourceSet(sourceSets.named("main").get())
        }
    }
}

val localRuntimeConfiguration = configurations.maybeCreate("localRuntime")
val librariesConfiguration = configurations.maybeCreate("libraries")

configurations.named("runtimeClasspath") {
    extendsFrom(localRuntimeConfiguration)
}
configurations.named("implementation") {
    extendsFrom(librariesConfiguration)
}

librariesConfiguration.isTransitive = true

allprojects {
    configurations.all {
        resolutionStrategy {
            force("org.slf4j:slf4j-api:2.0.16")
        }
    }
}

val metadataOutput = layout.buildDirectory.dir("generated/sources/modMetadata")

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "neo_version" to neoVersion,
        "neo_version_range" to neoVersionRange,
        "loader_version_range" to loaderVersionRange,
        "mod_id" to modId,
        "mod_name" to providers.gradleProperty("mod_name").get(),
        "mod_license" to providers.gradleProperty("mod_license").get(),
        "mod_version" to modVersion,
        "mod_authors" to providers.gradleProperty("mod_authors").get(),
        "mod_description" to providers.gradleProperty("mod_description").get()
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
    into(metadataOutput)
}

sourceSets.named("main") {
    resources.srcDir("src/generated/resources")
    resources.srcDir(metadataOutput)
}

neoForge.ideSyncTask(generateModMetadata)

dependencies {
}

publishing {
    publications {
        create("mavenJava", MavenPublication::class) {
            from(components["java"])
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.named<Test>("test") {
    isEnabled = false
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
        val artifact = layout.buildDirectory.file("libs/rdi-5-mc-client-1.21.1-neoforge.jar")
        val baseDestinations = listOf(
            layout.projectDirectory.dir("..\\..\\..\\server\\master\\run\\client-libs")
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
registerCopyTask("出core-local")
registerCopyTask("出core-release", listOf("\\\\rdi5\\rdi55\\ihq\\client-libs\\"))