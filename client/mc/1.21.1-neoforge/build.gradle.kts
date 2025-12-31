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
    maven {
        name = "Kotlin for Forge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
        content {
            includeGroup("thedarkcolour")
        }
    }
    maven {
        name = "Jared's maven"
        url = uri("https://maven.blamejared.com/")
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
    exclusiveContent {
        forRepository {
            maven {
                url = uri("https://cursemaven.com")
            }
        }
        filter {
            includeGroup("curse.maven")
        }
    }
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
            systemProperty("rdi.host.port", "65232")
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

val mcLibs = listOf(
    "io.ktor:ktor-client-okhttp:$ktorVersion",
    "io.ktor:ktor-client-core:$ktorVersion",
    "io.ktor:ktor-client-websockets:$ktorVersion",
    "io.ktor:ktor-client-encoding:$ktorVersion",
    "org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0",
    "io.ktor:ktor-serialization-kotlinx-json:$ktorVersion",
    "calebxzhou.mykotutils:log:0.1"
)

val modrinthMods = listOf(
    "justenoughcharacters:4.5.15",
    "notenoughcrashes:4.4.8+1.21.1-neoforge",
    "imblocker-original:5.0.2",
    "sodium:mc1.21.1-0.6.13-neoforge"
)

val curseMods = emptyList<String>()

val autoEmbedJarJarExcludeCoords = setOf(
    "org.jetbrains.kotlin:kotlin-stdlib",
    "org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm",
    "org.jetbrains.kotlinx:kotlinx-serialization-core-jvm",
    "org.jetbrains.kotlinx:kotlinx-serialization-json-jvm",
    "org.slf4j:slf4j-api",
    "com.google.errorprone:error_prone_annotations",
    "org.ow2.asm:asm",
    "org.ow2.asm:asm-commons",
    "org.ow2.asm:asm-tree",
    "org.ow2.asm:asm-analysis",
    "org.ow2.asm:asm-util"
)

val autoEmbedJarJarTransitivesExtension = extensions.getByType<AutoEmbedJarJarTransitivesExtension>()
autoEmbedJarJarTransitivesExtension.jarJarExcludeCoords.addAll(autoEmbedJarJarExcludeCoords)

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
    testImplementation(kotlin("test"))
    implementation("org.hotswapagent:hotswap-agent-core:2.0.1")
    testImplementation("ch.qos.logback:logback-classic:1.5.21")
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.6")
    mcLibs.forEach { lib ->
        add("libraries", lib)
        autoEmbedJarJarTransitivesExtension.include(lib)
        add("additionalRuntimeClasspath", lib)
    }

    modrinthMods.forEach { dep ->
        implementation("maven.modrinth:$dep")
    }

    curseMods.forEach { dep ->
        implementation("curse.maven:$dep-deobf")
    }
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

fun registerCopyTask(name: String, extraDestination: File? = null) {
    tasks.register(name) {
        dependsOn(tasks.named("build"))
        val artifact = layout.buildDirectory.file("libs/rdi-${'$'}{version}.jar")
        val destinations = mutableListOf(
            layout.projectDirectory.dir("..${'$'}{File.separator}ihq${'$'}{File.separator}run").asFile,
            File(System.getProperty("user.home"), "Documents\\RDI5sea-Ref\\.minecraft\\versions\\RDI5.5\\mods")
        )
        extraDestination?.let { destinations.add(it) }
        doLast {
            val jarFile = artifact.get().asFile
            check(jarFile.exists()) { "未找到构建产物: ${'$'}jarFile" }
            destinations.forEach { targetDir ->
                targetDir.mkdirs()
                val destFile = File(targetDir, jarFile.name)
                Files.copy(jarFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}

registerCopyTask("出core-debug")
registerCopyTask("出core-release", File("\\\\rdi5\\rdi55\\ihq"))